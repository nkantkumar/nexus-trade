package com.trading.risk;


import java.util.HashMap;
import java.util.Map;

/**
 * Complete example showing how to use the risk engine
 */
public class RiskEngineExample {
    public static void main(String[] args) {
        // Initialize components
        RiskEngine riskEngine = new RiskEngine();
        CreditManager creditManager = new CreditManager();
        OrderValidator orderValidator = new OrderValidator(riskEngine);

        // Create and register an account
        RiskAccount account = new RiskAccount("ACC123", "John Doe", AccountType.INDIVIDUAL);
        account.setCreditLimit(100_000.00);
        account.setPositionLimit(10_000);
        account.setGrossExposureLimit(200_000);
        account.setMaxOrderValue(50_000);
        account.setMaxOrderQuantity(5_000);

        creditManager.createCreditAccount("ACC123", 100_000.00, 0.30);
        riskEngine.registerAccount(account);

        // Create an order
        OrderRequest order = new OrderRequest(
                "ORD001",           // orderId
                "AAPL",             // symbol
                OrderSide.BUY,      // side
                1000,               // quantity
                150.25,             // price
                OrderType.LIMIT     // order type
        );

        // Pre-trade risk check
        RiskCheckResult riskResult = riskEngine.checkPreTrade(order, "ACC123");

        if (riskResult.isPassed()) {
            System.out.println("Order passed risk checks: " + order.getOrderId());

            // Simulate execution
            Execution execution = new Execution(
                    "EXEC001",
                    order.getOrderId(),
                    order.getSymbol(),
                    order.getSide(),
                    order.getQuantity(),
                    order.getPrice(),
                    ExecutionType.FILL
            );

            // Update risk post-trade
            riskEngine.updatePostTrade(execution, "ACC123");
            System.out.println("Post-trade risk updated");
        } else {
            System.err.println("Order rejected: " + riskResult.getMessage());
        }

        // Update market prices for mark-to-market
        Map<String, Double> prices = new HashMap<>();
        prices.put("AAPL", 155.50);

        // Print metrics
        RiskMetrics metrics = riskEngine.getMetricsCollector().getLatestMetrics();
        System.out.println("\nRisk Metrics:");
        System.out.println("  Total Checks: " + metrics.getTotalChecks());
        System.out.println("  Pass Rate: " + (metrics.getPassRate() * 100) + "%");
        System.out.println("  Avg Latency: " + metrics.getAverageLatencyMicros() + " μs");

        // Shutdown
        riskEngine.shutdown();
        creditManager.shutdown();
    }
}