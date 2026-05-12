package com.trading.risk;

import java.util.*;


/**
 * Margin Validator - Now properly receives dependencies via constructor
 */
public class MarginValidator implements RiskValidator {
    private final MarginManager marginManager;
    private final MarketDataProvider marketDataProvider;

    // Constructor injection
    public MarginValidator(MarginManager marginManager, MarketDataProvider marketDataProvider) {
        this.marginManager = marginManager;
        this.marketDataProvider = marketDataProvider;
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // 1. Calculate initial margin requirement for this order
        double orderMargin = marginManager.calculateInitialMargin(order, account);

        // 2. Calculate total margin requirement including existing positions
        double totalMargin = marginManager.calculateTotalMargin(account) + orderMargin;

        // 3. Check against available equity
        double availableEquity = account.getCreditLimit() - account.getUsedCredit();
        double requiredMargin = Math.max(totalMargin, account.getMaintenanceMargin());

        if (availableEquity < requiredMargin) {
            return RiskCheckResult.fail(RiskCheckType.MARGIN,
                    String.format("Margin insufficient. Required=%.2f, Available=%.2f, Shortfall=%.2f",
                            requiredMargin, availableEquity, requiredMargin - availableEquity));
        }

        // 4. For existing positions, check maintenance margin
        if (context.isPostTrade()) {
            double maintenanceMargin = marginManager.calculateMaintenanceMargin(account);
            if (availableEquity < maintenanceMargin) {
                return RiskCheckResult.fail(RiskCheckType.MARGIN,
                        String.format("Maintenance margin violation. Required=%.2f, Available=%.2f",
                                maintenanceMargin, availableEquity));
            }
        }

        // 5. Check portfolio concentration
        double concentration = calculateConcentration(account, order);
        if (concentration > 0.40) { // 40% max concentration
            return RiskCheckResult.fail(RiskCheckType.MARGIN,
                    String.format("Portfolio concentration too high: %.2f%%", concentration * 100));
        }

        return RiskCheckResult.pass(RiskCheckType.MARGIN);
    }

    private double calculateConcentration(RiskAccount account, OrderRequest order) {
        double positionValueAfter = Math.abs(
                (account.getPosition(order.getSymbol()).getQuantity() +
                        (order.getSide() == OrderSide.BUY ? order.getQuantity() : -order.getQuantity()))
        ) * order.getPrice();

        double totalPortfolioValue = getTotalPortfolioValue(account);

        if (totalPortfolioValue == 0) return 0;
        return positionValueAfter / totalPortfolioValue;
    }

    private double getTotalPortfolioValue(RiskAccount account) {
        double value = 0;
        for (Position pos : account.getPositions().values()) {
            value += Math.abs(pos.getQuantity()) * pos.getCurrentPrice();
        }
        return value;
    }

    @Override
    public int getPriority() { return 30; }
}
