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
    }

    // Convenience constructor with defaults
    public RiskEngine() {
        this(new CreditManager(), new MarginManager(), new MarketDataProvider());
    }

    private void initializeValidators() {
        // Pre-trade validators (in priority order - lower number = higher priority)
        preTradeValidators.add(new DuplicateOrderValidator());        // Priority 1
        preTradeValidators.add(new RateLimitValidator());              // Priority 2
        preTradeValidators.add(new FatFingerValidator(marketDataProvider)); // Priority 5
        preTradeValidators.add(new SymbolRestrictionValidator());      // Priority 15
        preTradeValidators.add(new TradingHoursValidator());           // Priority 25
        preTradeValidators.add(new PositionLimitValidator());          // Priority 10
        preTradeValidators.add(new ExposureValidator());               // Priority 20

        // Validators that need dependencies - now properly injected
        MarginValidator marginValidator = new MarginValidator(marginManager, marketDataProvider);
        CreditValidator creditValidator = new CreditValidator(creditManager);

        preTradeValidators.add(marginValidator);   // Priority 30
        preTradeValidators.add(creditValidator);   // Priority 40

        // Sort by priority
        preTradeValidators.sort(Comparator.comparingInt(RiskValidator::getPriority));

        // Post-trade validators
        PostTradeRiskValidator postTradeValidator = new PostTradeRiskValidator(marketDataProvider);
        postTradeValidators.add(postTradeValidator);
    }

    public void disableTrading(String accountId) {
    }

    public void setTradingRestricted(String accountId, boolean b) {
    }

    public void forceLiquidation(String accountId) {
    }

    // Rest of RiskEngine methods remain the same...
    // (checkPreTrade, updatePostTrade, registerAccount, etc.)
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