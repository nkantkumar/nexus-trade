package com.trading.risk;


import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Main Risk Engine - coordinates all risk checks
 */
public class RiskEngine {
    private final List<RiskValidator> preTradeValidators;
    private final List<RiskValidator> postTradeValidators;
    private final Map<String, RiskAccount> accounts;
    private final RiskEventPublisher eventPublisher;
    private final ScheduledExecutorService housekeepingExecutor;
    private final RiskMetricsCollector metricsCollector;

    // Dependencies
    private final CreditManager creditManager;
    private final MarginManager marginManager;
    private final MarketDataProvider marketDataProvider;

    // Real-time risk monitoring
    private final Map<String, RiskAlert> activeAlerts;
    private volatile RiskLevel globalRiskLevel = RiskLevel.NORMAL;

    // Constructor with dependency injection
    public RiskEngine(CreditManager creditManager,
                      MarginManager marginManager,
                      MarketDataProvider marketDataProvider) {
        this.preTradeValidators = new ArrayList<>();
        this.postTradeValidators = new ArrayList<>();
        this.accounts = new ConcurrentHashMap<>();
        this.eventPublisher = new RiskEventPublisher();
        this.activeAlerts = new ConcurrentHashMap<>();
        this.metricsCollector = new RiskMetricsCollector();

        // Injected dependencies
        this.creditManager = creditManager;
        this.marginManager = marginManager;
        this.marketDataProvider = marketDataProvider;

        // Setup housekeeping thread
        this.housekeepingExecutor = Executors.newSingleThreadScheduledExecutor();
        housekeepingExecutor.scheduleAtFixedRate(this::housekeeping, 60, 60, TimeUnit.SECONDS);

        // Initialize validators with dependencies
        initializeValidators();
    }

    private void housekeeping() {
        long now = System.currentTimeMillis();
        activeAlerts.values().removeIf(alert -> now - alert.getTimestamp() > 300000); // 5 min expiry
        metricsCollector.setActiveAlertCount(activeAlerts.size());
    }

    // Convenience constructor with defaults
    public RiskEngine() {
        this(new CreditManager(), new MarginManager(), new MarketDataProvider());
    }

    private void initializeValidators() {
        preTradeValidators.add(new TradingHoursValidator());
        preTradeValidators.add(new SymbolRestrictionValidator());
        preTradeValidators.add(new RateLimitValidator());
        preTradeValidators.add(new DuplicateOrderValidator());
        preTradeValidators.add(new FatFingerValidator(marketDataProvider));
        preTradeValidators.add(new PositionLimitValidator());
        preTradeValidators.add(new ExposureValidator());
        preTradeValidators.add(new MarginValidator(marginManager, marketDataProvider));
        preTradeValidators.add(new CreditValidator(creditManager));

        // Sort by priority (lower priority = runs first)
        preTradeValidators.sort(Comparator.comparingInt(RiskValidator::getPriority));

        // Post-trade validators
        postTradeValidators.add(new PostTradeRiskValidator());
    }

    public void registerAccount(RiskAccount account) {
        accounts.put(account.getAccountId(), account);
        eventPublisher.publishRiskEvent(new RiskEvent(RiskEventType.ACCOUNT_REGISTERED, account.getAccountId(), null, null));
    }

    public RiskCheckResult checkPreTrade(OrderRequest order, String accountId) {
        RiskAccount account = accounts.get(accountId);
        if (account == null) {
            return RiskCheckResult.fail(RiskCheckType.CREDIT, "Account not found: " + accountId);
        }

        double price = marketDataProvider.getCurrentPrice(order.getSymbol());
        double vol = marketDataProvider.getVolatility(order.getSymbol());
        RiskContext context = new RiskContext(price, vol, System.currentTimeMillis());

        long start = System.nanoTime();
        for (RiskValidator validator : preTradeValidators) {
            RiskCheckResult result = validator.validate(order, account, context);
            if (!result.isPassed()) {
                long latency = System.nanoTime() - start;
                metricsCollector.recordCheck(latency, false);
                metricsCollector.recordFailure(result.getCheckType());
                
                // Publish risk event
                RiskEvent event = new RiskEvent(RiskEventType.PRE_TRADE_REJECTED, accountId, order, result);
                eventPublisher.publishRiskEvent(event);
                return result;
            }
        }

        long latency = System.nanoTime() - start;
        metricsCollector.recordCheck(latency, true);
        return RiskCheckResult.pass(RiskCheckType.POSITION_LIMIT); // General pass outcome
    }

    public void updatePostTrade(Execution execution, String accountId) {
        RiskAccount account = accounts.get(accountId);
        if (account == null) return;

        // Update positions/metrics
        Position position = account.getPosition(execution.getSymbol());
        long executionQty = execution.getQuantity();
        if (execution.getSide() == OrderSide.SELL) {
            executionQty = -executionQty;
        }


        account.addPosition(position);

        // Run post-trade checks
        double price = marketDataProvider.getCurrentPrice(execution.getSymbol());
        double vol = marketDataProvider.getVolatility(execution.getSymbol());
        RiskContext context = new RiskContext(price, vol, System.currentTimeMillis(), true);

        for (RiskValidator validator : postTradeValidators) {
            RiskCheckResult result = validator.validate(null, account, context);
            if (!result.isPassed()) {
                eventPublisher.publishRiskEvent(new RiskEvent(RiskEventType.POST_TRADE_BREACH, accountId, null, result));
                
                RiskAlert alert = new RiskAlert(accountId, result.getCheckType(), RiskAlertSeverity.HIGH,
                        result.getMessage(), null);
                activeAlerts.put(alert.getAlertId(), alert);
                metricsCollector.setActiveAlertCount(activeAlerts.size());
            }
        }
    }

    public void disableTrading(String accountId) {
        System.out.println("Trading disabled for account: " + accountId);
    }

    public void setTradingRestricted(String accountId, boolean restricted) {
        System.out.println("Trading restricted state for " + accountId + " set to " + restricted);
    }

    public void forceLiquidation(String accountId) {
        System.out.println("Force liquidation triggered for account: " + accountId);
        eventPublisher.publishRiskEvent(new RiskEvent(RiskEventType.FORCE_LIQUIDATION, accountId, null, null));
    }

    public void subscribeToEvents(RiskEventListener listener) {
        eventPublisher.subscribe(listener);
    }

    public void increaseMonitoringFrequency(String accountId) {
        System.out.println("Increased monitoring frequency for account: " + accountId);
    }

    public void shutdown() {
        housekeepingExecutor.shutdown();
    }

    public RiskMetricsCollector getMetricsCollector() {
        return metricsCollector;
    }

    public RiskLevel getGlobalRiskLevel() {
        return globalRiskLevel;
    }

    public void setGlobalRiskLevel(RiskLevel level) {
        this.globalRiskLevel = level;
    }
}

// Supporting classes

class RiskContext {
    private final double marketPrice;
    private final double volatility;
    private final long timestamp;
    private final boolean postTrade;

    public RiskContext(double marketPrice, double volatility, long timestamp) {
        this(marketPrice, volatility, timestamp, false);
    }

    public RiskContext(double marketPrice, double volatility, long timestamp, boolean postTrade) {
        this.marketPrice = marketPrice;
        this.volatility = volatility;
        this.timestamp = timestamp;
        this.postTrade = postTrade;
    }

    public double getMarketPrice() { return marketPrice; }
    public double getVolatility() { return volatility; }
    public long getTimestamp() { return timestamp; }
    public boolean isPostTrade() { return postTrade; }
}

enum RiskLevel { NORMAL, ELEVATED, HIGH, CRITICAL }
enum RiskEventType {
    PRE_TRADE_REJECTED, POST_TRADE_BREACH, MARGIN_CALL,
    FORCE_LIQUIDATION, ACCOUNT_REGISTERED, RISK_BREACH
}

class RiskEvent {
    private final RiskEventType type;
    private final String accountId;
    private final OrderRequest order;
    private final RiskCheckResult result;
    private final long timestamp;

    public RiskEvent(RiskEventType type, String accountId, OrderRequest order, RiskCheckResult result) {
        this.type = type;
        this.accountId = accountId;
        this.order = order;
        this.result = result;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters...
}

class RiskEventPublisher {
    private final List<RiskEventListener> listeners = new CopyOnWriteArrayList<>();

    public void publishRiskEvent(RiskEvent event) {
        for (RiskEventListener listener : listeners) {
            try {
                listener.onRiskEvent(event);
            } catch (Exception e) {
                System.err.println("Risk event listener error: " + e.getMessage());
            }
        }
    }

    public void subscribe(RiskEventListener listener) {
        listeners.add(listener);
    }
}

interface RiskEventListener {
    void onRiskEvent(RiskEvent event);
}