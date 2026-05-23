package com.trading.risk;


import java.util.*;
import java.util.concurrent.*;

/**
 * Validates risk after trades are executed
 * This is critical for detecting risk limit breaches in real-time
 */
public class PostTradeRiskValidator implements RiskValidator {
    private static final Logger logger = Logger.getLogger(PostTradeRiskValidator.class);

    // Post-trade specific limits
    private final Map<String, PostTradeLimits> accountLimits = new ConcurrentHashMap<>();
    private final Map<String, List<Trade>> recentTrades = new ConcurrentHashMap<>();

    // Configuration
    private final double maxDrawdownPercent = 0.20;      // 20% max drawdown
    private final double maxDailyLossPercent = 0.10;     // 10% daily loss limit
    private final long maxTradeFrequencyMs = 100;        // Min 100ms between trades
    private final int maxTradesPerMinute = 60;           // Max 60 trades per minute

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // This validator is primarily for post-trade checks
        // For pre-trade, we just do basic checks
        if (!context.isPostTrade()) {
            return validatePreTrade(order, account, context);
        }

        // Post-trade validations
        List<String> violations = new ArrayList<>();

        // 1. Check drawdown limits
        if (!checkDrawdownLimit(account, context)) {
            violations.add("Maximum drawdown limit exceeded");
        }

        // 2. Check daily loss limits
        if (!checkDailyLossLimit(account, context)) {
            violations.add("Daily loss limit exceeded");
        }

        // 3. Check trade frequency
        if (!checkTradeFrequency(account, order)) {
            violations.add("Trade frequency limit exceeded");
        }

        // 4. Check concentration limits
        if (!checkConcentrationLimit(account, order, context)) {
            violations.add("Position concentration limit exceeded");
        }

        // 5. Check portfolio risk metrics (VaR, etc.)
        if (!checkPortfolioRisk(account, context)) {
            violations.add("Portfolio risk limit exceeded (VaR)");
        }

        // 6. Check for pattern day trading (for individual accounts)
        if (account.getAccountType() == AccountType.INDIVIDUAL) {
            if (!checkPatternDayTrading(account, context)) {
                violations.add("Pattern day trading limit exceeded");
            }
        }

        // 7. Check wash sale rules (for tax purposes)
        if (!checkWashSaleRules(account, order, context)) {
            violations.add("Wash sale rule violation");
        }

        if (!violations.isEmpty()) {
            return RiskCheckResult.fail(RiskCheckType.MARGIN,
                    "Post-trade violations: " + String.join(", ", violations));
        }

        return RiskCheckResult.pass(RiskCheckType.MARGIN);
    }

    private RiskCheckResult validatePreTrade(OrderRequest order, RiskAccount account, RiskContext context) {
        // Basic pre-trade checks specific to post-trade validator
        // These prevent orders that would immediately trigger post-trade violations

        // Check if account is already in drawdown
        if (isAccountInDrawdown(account)) {
            return RiskCheckResult.fail(RiskCheckType.MARGIN,
                    "Account is currently in drawdown, new orders restricted");
        }

        // Check if daily loss would be exceeded by this order
        double potentialLoss = calculatePotentialLoss(order, account);
        double remainingDailyLoss = getRemainingDailyLossAllowance(account);
        if (potentialLoss > remainingDailyLoss) {
            return RiskCheckResult.fail(RiskCheckType.MARGIN,
                    String.format("Order would exceed daily loss limit. Potential loss=%.2f, Remaining=%.2f",
                            potentialLoss, remainingDailyLoss));
        }

        return RiskCheckResult.pass(RiskCheckType.MARGIN);
    }

    /**
     * Check if account exceeds maximum drawdown limits
     */
    private boolean checkDrawdownLimit(RiskAccount account, RiskContext context) {
        PostTradeLimits limits = getAccountLimits(account.getAccountId());
        double currentDrawdown = calculateDrawdown(account);

        if (currentDrawdown > limits.getMaxDrawdownPercent()) {
            logger.warn("Drawdown limit breached: Account={}, Drawdown={}%, Limit={}%",
                    account.getAccountId(), currentDrawdown * 100, limits.getMaxDrawdownPercent() * 100);
            return false;
        }

        return true;
    }

    /**
     * Check daily loss limits (resets at market close)
     */
    private boolean checkDailyLossLimit(RiskAccount account, RiskContext context) {
        PostTradeLimits limits = getAccountLimits(account.getAccountId());
        double dailyLoss = calculateDailyLoss(account);

        if (dailyLoss > limits.getMaxDailyLossPercent()) {
            logger.warn("Daily loss limit breached: Account={}, Loss={}%, Limit={}%",
                    account.getAccountId(), dailyLoss * 100, limits.getMaxDailyLossPercent() * 100);
            return false;
        }

        return true;
    }

    /**
     * Check trade frequency to prevent excessive trading
     */
    private boolean checkTradeFrequency(RiskAccount account, OrderRequest order) {
        String key = account.getAccountId();
        List<Trade> trades = recentTrades.computeIfAbsent(key, k -> new ArrayList<>());

        long now = System.currentTimeMillis();

        // Clean old trades
        trades.removeIf(t -> now - t.getTimestamp() > 60000); // Keep last minute

        // Check frequency within last second
        long recentTradesMs = trades.stream()
                .filter(t -> now - t.getTimestamp() < maxTradeFrequencyMs)
                .count();

        if (recentTradesMs > 0) {
            logger.warn("Trade frequency too high: Account={}, {} trades in {}ms",
                    account.getAccountId(), recentTradesMs, maxTradeFrequencyMs);
            return false;
        }

        // Check trades per minute
        if (trades.size() >= maxTradesPerMinute) {
            logger.warn("Trade limit per minute exceeded: Account={}, Trades={}, Limit={}",
                    account.getAccountId(), trades.size(), maxTradesPerMinute);
            return false;
        }

        return true;
    }

    /**
     * Check position concentration (single symbol exposure)
     */
    private boolean checkConcentrationLimit(RiskAccount account, OrderRequest order, RiskContext context) {
        PostTradeLimits limits = getAccountLimits(account.getAccountId());

        Position position = account.getPosition(order.getSymbol());
        long newQuantity = order.getSide() == OrderSide.BUY
                ? position.getQuantity() + order.getQuantity()
                : position.getQuantity() - order.getQuantity();

        double positionValue = Math.abs(newQuantity) * context.getMarketPrice();
        double portfolioValue = getPortfolioValue(account);

        if (portfolioValue > 0) {
            double concentration = positionValue / portfolioValue;
            if (concentration > limits.getMaxConcentrationPercent()) {
                logger.warn("Concentration limit breached: Account={}, Symbol={}, Concentration={}%, Limit={}%",
                        account.getAccountId(), order.getSymbol(),
                        concentration * 100, limits.getMaxConcentrationPercent() * 100);
                return false;
            }
        }

        return true;
    }

    /**
     * Check portfolio risk metrics (Value at Risk)
     */
    private boolean checkPortfolioRisk(RiskAccount account, RiskContext context) {
        PostTradeLimits limits = getAccountLimits(account.getAccountId());

        // Calculate VaR using historical simulation or parametric method
        double var95 = calculateValueAtRisk(account, 0.95);
        double portfolioValue = getPortfolioValue(account);

        // VaR should not exceed X% of portfolio
        double varRatio = var95 / portfolioValue;
        if (varRatio > limits.getMaxVaRPercent()) {
            logger.warn("VaR limit breached: Account={}, VaR(95%)={}%, Limit={}%",
                    account.getAccountId(), varRatio * 100, limits.getMaxVaRPercent() * 100);
            return false;
        }

        // Check stress test scenarios
        if (isStressTestLimitBreached(account)) {
            logger.warn("Stress test limit breached: Account={}", account.getAccountId());
            return false;
        }

        return true;
    }

    /**
     * Pattern Day Trading rules for individual accounts (US specific)
     * Pattern day trader: 4+ day trades in 5 rolling business days
     */
    private boolean checkPatternDayTrading(RiskAccount account, RiskContext context) {
        if (account.getAccountType() != AccountType.INDIVIDUAL) {
            return true; // Only applies to individual accounts
        }

        String key = account.getAccountId();
        List<Trade> trades = dayTrades.computeIfAbsent(key, k -> new ArrayList<>());

        long now = System.currentTimeMillis();
        long fiveDaysAgo = now - (5 * 24 * 60 * 60 * 1000);

        // Count day trades (buy and sell same symbol within same day)
        long dayTradeCount = trades.stream()
                .filter(t -> t.getTimestamp() > fiveDaysAgo)
                .filter(t -> t.isDayTrade())
                .count();

        if (dayTradeCount >= 4) {
            logger.warn("Pattern day trader limit breached: Account={}, Day trades={}",
                    account.getAccountId(), dayTradeCount);
            return false;
        }

        return true;
    }

    /**
     * Check wash sale rules (IRS rule - can't claim loss if rebought within 30 days)
     */
    private boolean checkWashSaleRules(RiskAccount account, OrderRequest order, RiskContext context) {
        // Only relevant for tax-paying entities
        if (account.getAccountType() == AccountType.HEDGE_FUND ||
                account.getAccountType() == AccountType.PROPRIETARY) {
            return true; // Tax-exempt entities
        }

        String key = account.getAccountId() + ":" + order.getSymbol();
        List<HistoricalTrade> tradeHistory = washSaleHistory.computeIfAbsent(key, k -> new ArrayList<>());

        long now = System.currentTimeMillis();
        long thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000);

        // Check if we're buying back a stock we sold at a loss in the last 30 days
        if (order.getSide() == OrderSide.BUY) {
            for (HistoricalTrade trade : tradeHistory) {
                if (trade.getTimestamp() > thirtyDaysAgo &&
                        trade.getSide() == OrderSide.SELL &&
                        trade.getPrice() < trade.getPurchasePrice() && // Sold at loss
                        trade.getQuantity() >= Math.abs(order.getQuantity())) {

                    logger.warn("Wash sale detected: Account={}, Symbol={}, Loss sale on {}",
                            account.getAccountId(), order.getSymbol(),
                            new Date(trade.getTimestamp()));
                    return false;
                }
            }
        }

        // Record this trade for future wash sale detection
        if (order.getSide() == OrderSide.SELL) {
            Position pos = account.getPosition(order.getSymbol());
            HistoricalTrade record = new HistoricalTrade(
                    order.getSide(),
                    order.getQuantity(),
                    order.getPrice(),
                    pos.getAveragePrice(),
                    now
            );
            tradeHistory.add(record);

            // Clean old records
            tradeHistory.removeIf(t -> now - t.getTimestamp() > 30 * 24 * 60 * 60 * 1000L);
        }

        return true;
    }

    /**
     * Record trade for post-trade analysis
     */
    public void recordTrade(Trade trade, RiskAccount account) {
        String key = account.getAccountId();
        List<Trade> trades = recentTrades.computeIfAbsent(key, k -> new ArrayList<>());
        trades.add(trade);

        // Update P&L tracking
        updateDailyPnL(account, trade);

        // Update drawdown tracking
        updateDrawdownTracking(account);
    }

    /**
     * Calculate Value at Risk using parametric method
     */
    private double calculateValueAtRisk(RiskAccount account, double confidenceLevel) {
        double portfolioValue = getPortfolioValue(account);
        double portfolioVolatility = getPortfolioVolatility(account);

        // Z-score for confidence level
        double zScore;
        if (confidenceLevel == 0.95) {
            zScore = 1.645;
        } else if (confidenceLevel == 0.99) {
            zScore = 2.326;
        } else {
            zScore = 1.96; // Default 95%
        }

        // Parametric VaR = Portfolio Value * Volatility * Z-score * sqrt(holding period)
        double holdingPeriodDays = 1.0; // 1-day VaR
        return portfolioValue * portfolioVolatility * zScore * Math.sqrt(holdingPeriodDays);
    }

    /**
     * Calculate drawdown from peak
     */
    private double calculateDrawdown(RiskAccount account) {
        PeakTracker tracker = peakTrackers.get(account.getAccountId());
        if (tracker == null) return 0;

        double currentValue = getPortfolioValue(account);
        double peakValue = tracker.getPeakValue();

        if (peakValue == 0) return 0;
        return (peakValue - currentValue) / peakValue;
    }

    /**
     * Calculate daily loss as percentage of starting equity
     */
    private double calculateDailyLoss(RiskAccount account) {
        DailyPnL pnl = dailyPnL.get(account.getAccountId());
        if (pnl == null) return 0;

        double startingEquity = pnl.getStartingEquity();
        if (startingEquity == 0) return 0;

        double loss = -pnl.getRealizedPnL(); // Negative PnL is loss
        return loss / startingEquity;
    }

    /**
     * Check if stress test scenarios would cause excessive losses
     */
    private boolean isStressTestLimitBreached(RiskAccount account) {
        // Define stress scenarios
        Map<String, Double> stressScenarios = Map.of(
                "Market crash -20%", -0.20,
                "Flash crash -10%", -0.10,
                "Volatility spike +100%", -0.15,
                "Liquidity freeze", -0.25
        );

        double maxLoss = 0;
        for (Map.Entry<String, Double> scenario : stressScenarios.entrySet()) {
            double portfolioLoss = calculateScenarioLoss(account, scenario.getValue());
            maxLoss = Math.max(maxLoss, portfolioLoss);
        }

        double portfolioValue = getPortfolioValue(account);
        double stressLossPercent = maxLoss / portfolioValue;

        return stressLossPercent > 0.30; // Can't lose more than 30% in stress scenario
    }

    private double calculateScenarioLoss(RiskAccount account, double marketMove) {
        double totalLoss = 0;
        for (Position position : account.getPositions().values()) {
            // Beta-adjusted loss
            double beta = getBeta(position.getSymbol());
            double positionLoss = position.getQuantity() * position.getCurrentPrice() *
                    marketMove * beta;
            totalLoss += positionLoss;
        }
        return totalLoss;
    }

    private PostTradeLimits getAccountLimits(String accountId) {
        return accountLimits.computeIfAbsent(accountId, k -> {
            PostTradeLimits limits = new PostTradeLimits();
            limits.setMaxDrawdownPercent(maxDrawdownPercent);
            limits.setMaxDailyLossPercent(maxDailyLossPercent);
            limits.setMaxConcentrationPercent(0.25); // 25% max concentration
            limits.setMaxVaRPercent(0.10); // 10% VaR limit
            return limits;
        });
    }

    // Helper methods and tracking structures

    private final Map<String, PeakTracker> peakTrackers = new ConcurrentHashMap<>();
    private final Map<String, DailyPnL> dailyPnL = new ConcurrentHashMap<>();
    private final Map<String, List<HistoricalTrade>> washSaleHistory = new ConcurrentHashMap<>();
    private final Map<String, List<Trade>> dayTrades = new ConcurrentHashMap<>();

    private double getPortfolioValue(RiskAccount account) {
        double value = 0;
        for (Position position : account.getPositions().values()) {
            value += Math.abs(position.getQuantity()) * position.getCurrentPrice();
        }
        return value;
    }

    private double getPortfolioVolatility(RiskAccount account) {
        // Simplified - would use proper covariance matrix in production
        return 0.25; // 25% annual volatility
    }

    private double getBeta(String symbol) {
        // Simplified - would fetch from market data
        return 1.0;
    }

    private boolean isAccountInDrawdown(RiskAccount account) {
        double drawdown = calculateDrawdown(account);
        return drawdown > maxDrawdownPercent * 0.8; // 80% of limit
    }

    private double calculatePotentialLoss(OrderRequest order, RiskAccount account) {
        // Estimate worst-case loss for this order
        double volatility = getVolatility(order.getSymbol());
        double worstCaseMove = 3 * volatility; // 3 standard deviations
        return order.getQuantity() * order.getPrice() * worstCaseMove;
    }

    private double getVolatility(String symbol) {
        // Would fetch from market data
        return 0.02; // 2% daily volatility
    }

    private double getRemainingDailyLossAllowance(RiskAccount account) {
        double currentLoss = calculateDailyLoss(account);
        PostTradeLimits limits = getAccountLimits(account.getAccountId());
        double maxLoss = limits.getMaxDailyLossPercent();
        return maxLoss - currentLoss;
    }

    private void updateDailyPnL(RiskAccount account, Trade trade) {
        DailyPnL pnl = dailyPnL.computeIfAbsent(account.getAccountId(),
                k -> new DailyPnL(getPortfolioValue(account)));

        double tradePnL = calculateTradePnL(trade, account);
        pnl.addRealizedPnL(tradePnL);
    }

    private void updateDrawdownTracking(RiskAccount account) {
        PeakTracker tracker = peakTrackers.computeIfAbsent(account.getAccountId(),
                k -> new PeakTracker());
        double currentValue = getPortfolioValue(account);
        tracker.update(currentValue);
    }

    private double calculateTradePnL(Trade trade, RiskAccount account) {
        Position position = account.getPosition(trade.getSymbol());
        // Simplified PnL calculation
        return trade.getQuantity() * (trade.getPrice() - position.getAveragePrice());
    }

    private double getVolatility(RiskAccount account) {
        // Simplified - would use historical data
        return 0.25;
    }

    @Override
    public int getPriority() {
        return 100; // Lower priority than pre-trade validators
    }
}

/**
 * Post-trade limits configuration
 */
class PostTradeLimits {
    private double maxDrawdownPercent;
    private double maxDailyLossPercent;
    private double maxConcentrationPercent;
    private double maxVaRPercent;
    private int maxDayTradesPer5Days;

    // Getters and setters
    public double getMaxDrawdownPercent() { return maxDrawdownPercent; }
    public void setMaxDrawdownPercent(double percent) { this.maxDrawdownPercent = percent; }

    public double getMaxDailyLossPercent() { return maxDailyLossPercent; }
    public void setMaxDailyLossPercent(double percent) { this.maxDailyLossPercent = percent; }

    public double getMaxConcentrationPercent() { return maxConcentrationPercent; }
    public void setMaxConcentrationPercent(double percent) { this.maxConcentrationPercent = percent; }

    public double getMaxVaRPercent() { return maxVaRPercent; }
    public void setMaxVaRPercent(double percent) { this.maxVaRPercent = percent; }

    public int getMaxDayTradesPer5Days() { return maxDayTradesPer5Days; }
    public void setMaxDayTradesPer5Days(int count) { this.maxDayTradesPer5Days = count; }
}

/**
 * Tracks peak portfolio value for drawdown calculations
 */
class PeakTracker {
    private double peakValue = 0;
    private long peakTimestamp = 0;

    public synchronized void update(double currentValue) {
        if (currentValue > peakValue) {
            peakValue = currentValue;
            peakTimestamp = System.currentTimeMillis();
        }
    }

    public double getPeakValue() { return peakValue; }
    public long getPeakTimestamp() { return peakTimestamp; }
}

/**
 * Tracks daily P&L
 */
class DailyPnL {
    private final double startingEquity;
    private double realizedPnL = 0;
    private double unrealizedPnL = 0;
    private int day = -1;

    public DailyPnL(double startingEquity) {
        this.startingEquity = startingEquity;
        this.day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }

    public void addRealizedPnL(double pnl) {
        // Reset if new day
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        if (currentDay != day) {
            realizedPnL = 0;
            day = currentDay;
        }
        this.realizedPnL += pnl;
    }

    public double getStartingEquity() { return startingEquity; }
    public double getRealizedPnL() { return realizedPnL; }
}

/**
 * Historical trade for wash sale detection
 */
class HistoricalTrade {
    private final OrderSide side;
    private final long quantity;
    private final double price;
    private final double purchasePrice;
    private final long timestamp;

    public HistoricalTrade(OrderSide side, long quantity, double price,
                           double purchasePrice, long timestamp) {
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.purchasePrice = purchasePrice;
        this.timestamp = timestamp;
    }

    public OrderSide getSide() { return side; }
    public long getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getPurchasePrice() { return purchasePrice; }
    public long getTimestamp() { return timestamp; }
}

/**
 * Logger interface (simplified)
 */
class Logger {
    public static Logger getLogger(Class<?> clazz) {
        return new Logger();
    }
    public void warn(String message, Object... args) {
        try {
            String formatted = message.replace("{}", "%s");
            System.out.printf("[WARN] " + formatted + "%n", args);
        } catch (Exception e) {
            System.out.println("[WARN] " + message);
        }
    }
}
