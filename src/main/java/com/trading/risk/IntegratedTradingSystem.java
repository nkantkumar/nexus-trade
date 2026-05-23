package com.trading.risk;

import com.trading.matching.MatchingEngine;
import quickfix.fix44.ExecutionReport;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Integrating Risk Engine with Matching Engine
 */
public class IntegratedTradingSystem {
    private final MatchingEngine matchingEngine;
    private final RiskEngine riskEngine;
    private final CreditManager creditManager;


    public IntegratedTradingSystem() {
        this.matchingEngine = new MatchingEngine();
        this.riskEngine = new RiskEngine();
        this.creditManager = new CreditManager();
        // Wire up risk engine with credit manager
        setupRiskEngine();

        // Subscribe to risk events
        riskEngine.subscribeToEvents(new RiskEventHandler());
    }

    private void setupRiskEngine() {
        // Register accounts with risk limits
        RiskAccount propAccount = new RiskAccount("PROP001", "Proprietary Trading", AccountType.PROPRIETARY);
        propAccount.setCreditLimit(10_000_000.00);  // $10M credit line
        propAccount.setPositionLimit(1_000_000);    // 1M shares max per symbol
        propAccount.setGrossExposureLimit(50_000_000); // $50M max gross
        propAccount.setMaxOrderValue(1_000_000);     // $1M max order value
        propAccount.setMaxOrderQuantity(100_000);    // 100k shares max
        riskEngine.registerAccount(propAccount);

        RiskAccount clientAccount = new RiskAccount("CLT001", "Client Trading", AccountType.INDIVIDUAL);
        clientAccount.setCreditLimit(100_000.00);
        clientAccount.setPositionLimit(10_000);
        clientAccount.setGrossExposureLimit(200_000);
        clientAccount.setMaxOrderValue(10_000);
        clientAccount.setMaxOrderQuantity(1_000);
        riskEngine.registerAccount(clientAccount);
    }

    /**
     * Order submission flow with risk checks
     */
    public OrderSubmissionResult submitOrder(OrderRequest order, String accountId) {
        // Step 1: Pre-trade risk checks
        RiskCheckResult riskResult = riskEngine.checkPreTrade(order, accountId);
        if (!riskResult.isPassed()) {
            return OrderSubmissionResult.rejected(riskResult.getMessage());
        }

        // Step 2: Basic validation (price, quantity, symbol)
        com.trading.matching.Order matchingOrder = new com.trading.matching.Order(
            order.getOrderId(),
            order.getSymbol(),
            order.getSide() == OrderSide.BUY ? com.trading.matching.Order.Side.BUY : com.trading.matching.Order.Side.SELL,
            order.getPrice(),
            order.getQuantity()
        );



        // Step 4: Return confirmation
        return null;
    }

    /**
     * Handle execution reports and update risk
     */
    public void onExecutionReport(ExecutionReport report) {
        // Update risk engine post-trade
        try {
            Execution execution = convertToExecution(report);
            String accountId = report.isSetAccount() ? report.getAccount().getValue() : "PROP001";
            riskEngine.updatePostTrade(execution, accountId);
        } catch (Exception e) {
            System.err.println("Error processing execution report: " + e.getMessage());
        }
    }

    private Execution convertToExecution(ExecutionReport report) throws quickfix.FieldNotFound {
        String executionId = report.isSetExecID() ? report.getExecID().getValue() : java.util.UUID.randomUUID().toString();
        String orderId = report.isSetOrderID() ? report.getOrderID().getValue() : "Unknown";
        String symbol = report.isSetSymbol() ? report.getSymbol().getValue() : "Unknown";
        OrderSide side = (report.isSetSide() && report.getSide().getValue() == quickfix.field.Side.BUY) ? OrderSide.BUY : OrderSide.SELL;
        long quantity = report.isSetLastQty() ? (long) report.getLastQty().getValue() : 0L;
        double price = report.isSetLastPx() ? report.getLastPx().getValue() : 0.0;
        return new Execution(executionId, orderId, symbol, side, quantity, price, ExecutionType.TRADE);
    }

    private class RiskEventHandler implements RiskEventListener {
        @Override
        public void onRiskEvent(RiskEvent event) {
            // Need custom handling or simple logging
        }
    }
}

/**
 * Real-time risk monitoring dashboard
 */
class RiskMonitor {
    private final RiskEngine riskEngine;
    private final ScheduledExecutorService monitorExecutor;

    public RiskMonitor(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
        this.monitorExecutor = Executors.newScheduledThreadPool(1);

        // Start real-time monitoring
        monitorExecutor.scheduleAtFixedRate(this::updateDashboard, 0, 1, TimeUnit.SECONDS);
    }

    private void updateDashboard() {
        RiskMetrics metrics = riskEngine.getMetricsCollector().getLatestMetrics();

        // Clear screen and update display
        System.out.print("\033[2J\033[H"); // ANSI clear screen
        System.out.println("=== RISK DASHBOARD ===");
        System.out.printf("Global Risk Level: %s%n", riskEngine.getGlobalRiskLevel());
        System.out.printf("Total Checks: %d | Pass Rate: %.2f%%%n",
                metrics.getTotalChecks(), metrics.getPassRate() * 100);
        System.out.printf("Avg Check Latency: %.2f μs%n", metrics.getAverageLatencyMicros());
        System.out.printf("Active Alerts: %d%n", metrics.getActiveAlertCount());
        System.out.println("\n=== TOP RISK ACCOUNTS ===");
        // Display top risk accounts...
    }

    public void shutdown() {
        monitorExecutor.shutdown();
    }
}
