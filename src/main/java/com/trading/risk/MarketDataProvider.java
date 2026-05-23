package com.trading.risk;


import java.util.*;
import java.util.concurrent.*;

/**
 * Provides market data for risk calculations
 */
public class MarketDataProvider {
    private final Map<String, MarketData> marketDataMap;
    private final Map<String, Double> volatilityMap;
    private final Map<String, Double> betaMap;

    public MarketDataProvider() {
        this.marketDataMap = new ConcurrentHashMap<>();
        this.volatilityMap = new ConcurrentHashMap<>();
        this.betaMap = new ConcurrentHashMap<>();

        // Initialize with some default data
        initializeDefaults();
    }

    private void initializeDefaults() {
        // Default volatilities (annualized)
        volatilityMap.put("AAPL", 0.25);
        volatilityMap.put("GOOGL", 0.28);
        volatilityMap.put("MSFT", 0.22);
        volatilityMap.put("AMZN", 0.35);
        volatilityMap.put("TSLA", 0.55);
        volatilityMap.put("SPY", 0.15);

        // Default betas
        betaMap.put("AAPL", 1.2);
        betaMap.put("GOOGL", 1.1);
        betaMap.put("MSFT", 0.95);
        betaMap.put("AMZN", 1.3);
        betaMap.put("TSLA", 1.8);
        betaMap.put("SPY", 1.0);
    }

    /**
     * Get current market price for a symbol
     */
    public double getCurrentPrice(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        if (data != null) {
            return data.getLastPrice();
        }
        return 0; // Not available
    }

    /**
     * Get bid price
     */
    public double getBidPrice(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        return data != null ? data.getBidPrice() : 0;
    }

    /**
     * Get ask price
     */
    public double getAskPrice(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        return data != null ? data.getAskPrice() : 0;
    }

    /**
     * Get volatility for a symbol
     */
    public double getVolatility(String symbol) {
        return volatilityMap.getOrDefault(symbol, 0.30); // Default 30% volatility
    }

    /**
     * Get beta (market correlation) for a symbol
     */
    public double getBeta(String symbol) {
        return betaMap.getOrDefault(symbol, 1.0); // Default beta of 1
    }

    /**
     * Get daily volume
     */
    public long getDailyVolume(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        return data != null ? data.getVolume() : 0;
    }

    /**
     * Update market data (called from market data feed)
     */
    public void updateMarketData(String symbol, double price, double volume) {
        MarketData data = marketDataMap.computeIfAbsent(symbol, k -> new MarketData(symbol));
        data.update(price, (long) volume);

        // Update volatility based on price movement
        updateVolatility(symbol, price);
    }

    private void updateVolatility(String symbol, double newPrice) {
        MarketData data = marketDataMap.get(symbol);
        if (data != null && data.getLastPrice() > 0) {
            double dailyReturn = Math.log(newPrice / data.getLastPrice());
            // Simplified volatility update - would use EWMA in production
            double currentVol = volatilityMap.getOrDefault(symbol, 0.30);
            double newVol = currentVol * 0.94 + Math.abs(dailyReturn) * 0.06;
            volatilityMap.put(symbol, newVol);
        }
    }

    /**
     * Get market depth
     */
    public MarketDepth getMarketDepth(String symbol) {
        MarketData data = marketDataMap.get(symbol);
        return data != null ? data.getMarketDepth() : null;
    }

    // Inner class for market data
    private static class MarketData {
        private final String symbol;
        private double lastPrice;
        private double bidPrice;
        private double askPrice;
        private long volume;
        private long timestamp;
        private MarketDepth marketDepth;

        public MarketData(String symbol) {
            this.symbol = symbol;
            this.marketDepth = new MarketDepth();
        }

        public void update(double price, long volume) {
            this.lastPrice = price;
            this.volume = volume;
            this.timestamp = System.currentTimeMillis();
        }

        public double getLastPrice() { return lastPrice; }
        public double getBidPrice() { return bidPrice; }
        public double getAskPrice() { return askPrice; }
        public long getVolume() { return volume; }
        public MarketDepth getMarketDepth() { return marketDepth; }
    }

    // Market depth for liquidity checks
    public static class MarketDepth {
        private final List<Level> bids = new ArrayList<>();
        private final List<Level> asks = new ArrayList<>();

        public void addBid(double price, long size) {
            bids.add(new Level(price, size));
            bids.sort((a, b) -> Double.compare(b.price, a.price)); // Sort descending
        }

        public void addAsk(double price, long size) {
            asks.add(new Level(price, size));
            asks.sort((a, b) -> Double.compare(a.price, b.price)); // Sort ascending
        }

        public long getLiquidityAtPrice(double price, boolean isBuy) {
            List<Level> levels = isBuy ? asks : bids;
            long total = 0;
            for (Level level : levels) {
                if ((isBuy && level.price <= price) || (!isBuy && level.price >= price)) {
                    total += level.size;
                }
            }
            return total;
        }

        private static class Level {
            final double price;
            final long size;

            Level(double price, long size) {
                this.price = price;
                this.size = size;
            }
        }
    }
}
