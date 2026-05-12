package com.trading.risk;

package com.trading.risk.application;

import com.trading.risk.core.*;
import com.trading.risk.model.*;

/**
 * Complete application setup with all dependencies properly wired
 */
public class TradingRiskApplication {

    public static void main(String[] args) {
        // 1. Create base services
        MarketDataProvider marketDataProvider = new MarketDataProvider();
        CreditManager creditManager = new CreditManager();
        MarginManager marginManager = new MarginManager(marketDataProvider);

        // 2. Create risk engine with all dependencies injected
        RiskEngine riskEngine = new RiskEngine(creditManager, marginManager, marketDataProvider);

        // 3. Create and register accounts
        createAndRegisterAccounts(riskEngine, creditManager);

        // 4. Initialize market data
        initializeMarketData(marketDataProvider);

        // 5. Start monitoring
        RiskMonitor monitor = new RiskMonitor(riskEngine);

        // 6. Process some orders
        processTestOrders(riskEngine);

        // Keep running
        try {
            Thread.sleep(60000); // Run for 1 minute
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            monitor.shutdown();
            riskEngine.shutdown();
            creditManager.shutdown();
        }
    }

    private static void createAndRegisterAccounts(RiskEngine riskEngine, CreditManager creditManager) {
        // Create proprietary trading account
        RiskAccount propAccount = new RiskAccount("PROP001", "Proprietary Trading", AccountType.PROPRIETARY);
        propAccount.setCreditLimit(10_000_000.00);
        propAccount.setPositionLimit(1_000_000);
        propAccount.setGrossExposureLimit(50_000_000);
        propAccount.setMaxOrderValue(1_000_000);
        propAccount.setMaxOrderQuantity(100_000);
        propAccount.setMaintenanceMargin(0.05);

        creditManager.createCreditAccount("PROP001", 10_000_000.00, 0.30);
        creditManager.setCreditRating("PROP001", CreditRating.EXCELLENT);
        riskEngine.registerAccount(propAccount);

        // Create client account
        RiskAccount clientAccount = new RiskAccount("CLT001", "Client Trading", AccountType.INDIVIDUAL);
        clientAccount.setCreditLimit(100_000.00);
        clientAccount.setPositionLimit(10_000);
        clientAccount.setGrossExposureLimit(200_000);
        clientAccount.setMaxOrderValue(10_000);
        clientAccount.setMaxOrderQuantity(1_000);
        clientAccount.setMaintenanceMargin(0.10);

        creditManager.createCreditAccount("CLT001", 100_000.00, 0.50);
        creditManager.setCreditRating("CLT001", CreditRating.GOOD);
        riskEngine.registerAccount(clientAccount);

        // Create market maker account
        RiskAccount mmAccount = new RiskAccount("MM001", "Market Maker", AccountType.MARKET_MAKER);
        mmAccount.setCreditLimit(5_000_000.00);
        mmAccount.setPositionLimit(500_000);
        mmAccount.setGrossExposureLimit(25_000_000);
        mmAccount.setMaxOrderValue(500_000);
        mmAccount.setMaxOrderQuantity(50_000);
        mmAccount.setMaintenanceMargin(0.03);

        creditManager.createCreditAccount("MM001", 5_000_000.00, 0.20);
        creditManager.setCreditRating("MM001", CreditRating.EXCELLENT);
        creditManager.registerMarketMaker("MM001", 1_000_000.00);
        riskEngine.registerAccount(mmAccount);

        System.out.println("Registered accounts: PROP001, CLT001, MM001");
    }

    private static void initializeMarketData(MarketDataProvider marketDataProvider) {
        // Simulate market data updates
        marketDataProvider.updateMarketData("AAPL", 175.50, 50000000);
        marketDataProvider.updateMarketData("GOOGL", 140.25, 30000000);
        marketDataProvider.updateMarketData("MSFT", 330.75, 40000000);
        marketDataProvider.updateMarketData("AMZN", 145.80, 35000000);
        marketDataProvider.updateMarketData("TSLA", 240.30, 80000000);

        System.out.println("Market data initialized");
    }

    private static void processTestOrders(RiskEngine riskEngine) {
        // Test order 1: Should pass
        OrderRequest order1 = new OrderRequest(
                "ORD001", "AAPL", OrderSide.BUY, 1000, 175.50, OrderType.LIMIT,
                TimeInForce.DAY, "PROP001", "CLIENT1"
        );

        RiskCheckResult result1 = riskEngine.checkPreTrade(order1, "PROP001");
        System.out.println("Order 1 result: " + (result1.isPassed() ? "PASSED" : "FAILED - " + result1.getMessage()));

        // Test order 2: Should fail - excessive quantity
        OrderRequest order2 = new OrderRequest(
                "ORD002", "AAPL", OrderSide.BUY, 200000, 175.50, OrderType.LIMIT,
                TimeInForce.DAY, "CLT001", "CLIENT2"
        );

        RiskCheckResult result2 = riskEngine.checkPreTrade(order2, "CLT001");
        System.out.println("Order 2 result: " + (result2.isPassed() ? "PASSED" : "FAILED - " + result2.getMessage()));

        // Test order 3: Should fail - excessive price deviation
        OrderRequest order3 = new OrderRequest(
                "ORD003", "AAPL", OrderSide.BUY, 1000, 200.00, OrderType.LIMIT,
                TimeInForce.DAY, "PROP001", "CLIENT3"
        );

        RiskCheckResult result3 = riskEngine.checkPreTrade(order3, "PROP001");
        System.out.println("Order 3 result: " + (result3.isPassed() ? "PASSED" : "FAILED - " + result3.getMessage()));
    }
}
