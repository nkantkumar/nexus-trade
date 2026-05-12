package com.trading.risk;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fat Finger Validator - Now with MarketDataProvider for market prices
 */
public class FatFingerValidator implements RiskValidator {
    private final MarketDataProvider marketDataProvider;
    private final Map<String, OrderStatistics> symbolStats = new ConcurrentHashMap<>();
    private final double maxPriceDeviationPercent = 0.05;  // 5% from market
    private final long maxQuantityDeviation = 10000;       // Max quantity deviation

    // Constructor injection
    public FatFingerValidator(MarketDataProvider marketDataProvider) {
        this.marketDataProvider = marketDataProvider;
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // Check 1: Order value limit
        double orderValue = order.getQuantity() * order.getPrice();
        if (orderValue > account.getMaxOrderValue()) {
            return RiskCheckResult.fail(RiskCheckType.FAT_FINGER,
                    String.format("Order value %.2f exceeds limit %.2f",
                            orderValue, account.getMaxOrderValue()));
        }

        // Check 2: Order quantity limit
        if (order.getQuantity() > account.getMaxOrderQuantity()) {
            return RiskCheckResult.fail(RiskCheckType.FAT_FINGER,
                    String.format("Order quantity %d exceeds limit %d",
                            order.getQuantity(), account.getMaxOrderQuantity()));
        }

        // Check 3: Price deviation from market - Now using injected marketDataProvider
        double marketPrice = marketDataProvider.getCurrentPrice(order.getSymbol());
        if (marketPrice > 0) {
            double deviation = Math.abs(order.getPrice() - marketPrice) / marketPrice;
            if (deviation > maxPriceDeviationPercent) {
                return RiskCheckResult.fail(RiskCheckType.FAT_FINGER,
                        String.format("Price deviation %.2f%% exceeds limit %.2f%%. Order=%.2f, Market=%.2f",
                                deviation * 100, maxPriceDeviationPercent * 100,
                                order.getPrice(), marketPrice));
            }
        }

        // Check 4: Unusual order size compared to historical
        OrderStatistics stats = symbolStats.computeIfAbsent(order.getSymbol(),
                k -> new OrderStatistics());

        double zScore = stats.calculateZScore(order.getQuantity());
        if (Math.abs(zScore) > 5.0) {  // 5 standard deviations
            return RiskCheckResult.fail(RiskCheckType.FAT_FINGER,
                    String.format("Unusual order size (%.2f sigma). Quantity=%d, Avg=%.2f, StdDev=%.2f",
                            zScore, order.getQuantity(), stats.getMean(), stats.getStdDev()));
        }

        // Update statistics asynchronously
        stats.update(order.getQuantity());

        return RiskCheckResult.pass(RiskCheckType.FAT_FINGER);
    }

    @Override
    public int getPriority() { return 5; }

    // Inner class for statistics
    private static class OrderStatistics {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong sum = new AtomicLong(0);
        private final AtomicLong sumSquares = new AtomicLong(0);

        void update(long quantity) {
            count.incrementAndGet();
            sum.addAndGet(quantity);
            sumSquares.addAndGet(quantity * quantity);
        }

        double getMean() {
            long c = count.get();
            return c > 0 ? (double) sum.get() / c : 0;
        }

        double getStdDev() {
            long c = count.get();
            if (c < 2) return 0;
            double mean = getMean();
            double variance = (sumSquares.get() / (double) c) - (mean * mean);
            return Math.sqrt(variance);
        }

        double calculateZScore(long value) {
            double mean = getMean();
            double stdDev = getStdDev();
            if (stdDev == 0) return 0;
            return (value - mean) / stdDev;
        }
    }
}
