package com.trading.risk;


import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MarginManager {
    private final Map<String, MarginRequirements> productMargins;

    private final MarketDataProvider marketDataProvider;

    // Default margin rates
    private static final double DEFAULT_INITIAL_MARGIN = 0.10;  // 10%
    private static final double DEFAULT_MAINTENANCE_MARGIN = 0.05; // 5%
    private static final double PORTFOLIO_MARGIN_FACTOR = 0.15; // 15% for portfolio margin

    public MarginManager() {
        this.productMargins = new ConcurrentHashMap<>();

        this.marketDataProvider = new MarketDataProvider();

        // Initialize default margins for common products
        initializeDefaultMargins();
    }

    public MarginManager(MarketDataProvider marketDataProvider) {
        this.productMargins = new ConcurrentHashMap<>();

        this.marketDataProvider = marketDataProvider;
        initializeDefaultMargins();
    }

    private void initializeDefaultMargins() {
        // Equity margins
        productMargins.put("STOCK", new MarginRequirements(0.10, 0.05, false));
        productMargins.put("PENNY_STOCK", new MarginRequirements(0.50, 0.25, true)); // 50% initial for penny stocks

        // ETF margins
        productMargins.put("ETF", new MarginRequirements(0.10, 0.05, false));
        productMargins.put("LEVERAGED_ETF", new MarginRequirements(0.25, 0.15, true));

        // Options margins
        productMargins.put("EQUITY_OPTION", new MarginRequirements(0.20, 0.10, true));
        productMargins.put("INDEX_OPTION", new MarginRequirements(0.15, 0.08, false));

        // Futures margins (SPAN-based)
        productMargins.put("FUTURE", new MarginRequirements(0.05, 0.04, true));
        productMargins.put("MICRO_FUTURE", new MarginRequirements(0.03, 0.02, false));

        // Forex margins
        productMargins.put("FOREX_MAJOR", new MarginRequirements(0.02, 0.01, false));
        productMargins.put("FOREX_MINOR", new MarginRequirements(0.05, 0.03, false));

        // Crypto margins
        productMargins.put("CRYPTO", new MarginRequirements(0.50, 0.30, true));
        productMargins.put("CRYPTO_MAJOR", new MarginRequirements(0.30, 0.20, true));
    }

    /**
     * Calculate initial margin for an order
     */
    public double calculateInitialMargin(OrderRequest order, RiskAccount account) {
        String productType = getProductType(order.getSymbol());
        MarginRequirements requirements = productMargins.getOrDefault(productType,
                new MarginRequirements(DEFAULT_INITIAL_MARGIN, DEFAULT_MAINTENANCE_MARGIN, false));

        double orderValue = order.getQuantity() * order.getPrice();
        double baseMargin = orderValue * requirements.getInitialMargin();

        // Adjust for order type
        if (order.getOrderType() == OrderType.MARKET) {
            baseMargin *= 1.2; // 20% extra for market orders
        } else if (order.getOrderType() == OrderType.STOP) {
            baseMargin *= 1.1; // 10% extra for stop orders
        }

        // Adjust for account type
        if (account.getAccountType() == AccountType.MARKET_MAKER) {
            baseMargin *= 0.8; // 20% reduction for market makers
        } else if (account.getAccountType() == AccountType.HEDGE_FUND) {
            baseMargin *= 0.9; // 10% reduction for hedge funds
        } else if (account.getAccountType() == AccountType.INDIVIDUAL) {
            baseMargin *= 1.2; // 20% increase for retail
        }

        // Adjust for volatility
        double volatility = marketDataProvider.getVolatility(order.getSymbol());
        if (volatility > 0.5) { // High volatility
            baseMargin *= 1.5;
        } else if (volatility > 0.3) {
            baseMargin *= 1.2;
        }

        return baseMargin;
    }

    /**
     * Calculate total margin requirement for an account
     */
    public double calculateTotalMargin(RiskAccount account) {
        double totalMargin = 0;

        // Use portfolio margin if account qualifies
        if (isEligibleForPortfolioMargin(account)) {
            totalMargin = calculatePortfolioMargin(account);
        } else {
            // Standard Reg-T margin
            for (Position position : account.getPositions().values()) {
                double positionValue = Math.abs(position.getQuantity()) * position.getCurrentPrice();
                String productType = getProductType(position.getSymbol());
                MarginRequirements requirements = productMargins.getOrDefault(productType,
                        new MarginRequirements(DEFAULT_INITIAL_MARGIN, DEFAULT_MAINTENANCE_MARGIN, false));

                totalMargin += positionValue * requirements.getInitialMargin();
            }
        }

        // Add concentrated position surcharge
        double concentration = calculateConcentrationSurcharge(account);
        totalMargin += concentration;

        return totalMargin;
    }

    /**
     * Calculate maintenance margin for an account
     */
    public double calculateMaintenanceMargin(RiskAccount account) {
        double maintenanceMargin = 0;

        for (Position position : account.getPositions().values()) {
            double positionValue = Math.abs(position.getQuantity()) * position.getCurrentPrice();
            String productType = getProductType(position.getSymbol());
            MarginRequirements requirements = productMargins.getOrDefault(productType,
                    new MarginRequirements(DEFAULT_INITIAL_MARGIN, DEFAULT_MAINTENANCE_MARGIN, false));

            maintenanceMargin += positionValue * requirements.getMaintenanceMargin();
        }

        return maintenanceMargin;
    }

    /**
     * Calculate portfolio margin (more sophisticated than Reg-T)
     */
    private double calculatePortfolioMargin(RiskAccount account) {
        // Simulate various market scenarios
        double maxLoss = 0;

        // Scenario 1: Market up 10%
        double lossUp = calculateScenarioLoss(account, +0.10);
        maxLoss = Math.max(maxLoss, lossUp);

        // Scenario 2: Market down 10%
        double lossDown = calculateScenarioLoss(account, -0.10);
        maxLoss = Math.max(maxLoss, lossDown);

        // Scenario 3: Volatility up 20%
        double lossVolUp = calculateVolatilityLoss(account, +0.20);
        maxLoss = Math.max(maxLoss, lossVolUp);

        // Scenario 4: Correlation breakdown
        double lossCorrelation = calculateCorrelationRisk(account);
        maxLoss = Math.max(maxLoss, lossCorrelation);

        // Portfolio margin is 15% above maximum scenario loss
        return maxLoss * 1.15;
    }

    private double calculateScenarioLoss(RiskAccount account, double marketMove) {
        double totalLoss = 0;
        for (Position position : account.getPositions().values()) {
            double beta = marketDataProvider.getBeta(position.getSymbol());
            double positionValue = position.getQuantity() * position.getCurrentPrice();
            double loss = positionValue * marketMove * beta;

            if (position.getQuantity() < 0) { // Short position
                loss = -loss;
            }
            totalLoss += loss;
        }
        return Math.max(0, totalLoss);
    }

    private double calculateVolatilityLoss(RiskAccount account, double volIncrease) {
        // Options positions are sensitive to volatility
        double optionLoss = 0;
        for (Position position : account.getPositions().values()) {
            if (isOption(position.getSymbol())) {
                double vega = getOptionVega(position);
                optionLoss += vega * volIncrease;
            }
        }
        return optionLoss;
    }

    private double calculateCorrelationRisk(RiskAccount account) {
        // Check for correlated positions that could all move together
        // Simplified - would use correlation matrix in production
        return 0;
    }

    private double calculateConcentrationSurcharge(RiskAccount account) {
        double portfolioValue = getPortfolioValue(account);
        double maxPositionValue = 0;

        for (Position position : account.getPositions().values()) {
            double positionValue = Math.abs(position.getQuantity()) * position.getCurrentPrice();
            maxPositionValue = Math.max(maxPositionValue, positionValue);
        }

        double concentration = maxPositionValue / portfolioValue;
        if (concentration > 0.25) { // More than 25% in one position
            return portfolioValue * (concentration - 0.25) * 0.5; // 50% surcharge on excess
        }

        return 0;
    }

    private double getPortfolioValue(RiskAccount account) {
        double value = 0;
        for (Position position : account.getPositions().values()) {
            value += Math.abs(position.getQuantity()) * position.getCurrentPrice();
        }
        return value;
    }

    private boolean isEligibleForPortfolioMargin(RiskAccount account) {
        // Portfolio margin requires minimum equity and sophisticated risk management
        double equity = account.getCreditLimit() - account.getUsedCredit();
        return equity >= 100_000 && // Minimum $100k equity
                account.getAccountType() != AccountType.INDIVIDUAL;
    }

    private String getProductType(String symbol) {
        // Would determine product type from symbol
        if (symbol.startsWith("0")) return "OPTION";
        if (symbol.endsWith("FUT")) return "FUTURE";
        if (symbol.matches(".*[A-Z]{3}.*")) return "STOCK";
        return "STOCK";
    }

    private boolean isOption(String symbol) {
        return symbol.contains("C") || symbol.contains("P");
    }

    private double getOptionVega(Position position) {
        // Would calculate vega based on option Greeks
        return 0;
    }

    public void setMarginRequirement(String productType, double initialMargin, double maintenanceMargin) {
        productMargins.put(productType, new MarginRequirements(initialMargin, maintenanceMargin, false));
    }

    public MarginRequirements getMarginRequirements(String productType) {
        return productMargins.getOrDefault(productType,
                new MarginRequirements(DEFAULT_INITIAL_MARGIN, DEFAULT_MAINTENANCE_MARGIN, false));
    }
}

/**
 * Margin requirements for a product type
 */
class MarginRequirements {
    private final double initialMargin;
    private final double maintenanceMargin;
    private final boolean isHighRisk;

    public MarginRequirements(double initialMargin, double maintenanceMargin, boolean isHighRisk) {
        this.initialMargin = initialMargin;
        this.maintenanceMargin = maintenanceMargin;
        this.isHighRisk = isHighRisk;
    }

    public double getInitialMargin() { return initialMargin; }
    public double getMaintenanceMargin() { return maintenanceMargin; }
    public boolean isHighRisk() { return isHighRisk; }
}
