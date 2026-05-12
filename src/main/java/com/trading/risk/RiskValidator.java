package com.trading.risk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;.*;

/**
 * Interface for all risk validators
 */
public interface RiskValidator {
    RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context);
    int getPriority();  // Lower = higher priority
}

/**
 * Position Limit Validator - Checks position limits per symbol
 */
public class PositionLimitValidator implements RiskValidator {
    private static final Logger logger = LoggerFactory.getLogger(PositionLimitValidator.class);

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // Get current position for this symbol
        Position currentPosition = account.getPosition(order.getSymbol());
        long currentQuantity = currentPosition.getQuantity();

        // Calculate new position after order
        long newQuantity = order.getSide() == OrderSide.BUY
                ? currentQuantity + order.getQuantity()
                : currentQuantity - order.getQuantity();

        // Check absolute position limit
        long positionLimit = getPositionLimit(account, order.getSymbol());
        if (Math.abs(newQuantity) > positionLimit) {
            return RiskCheckResult.fail(RiskCheckType.POSITION_LIMIT,
                    String.format("Position limit exceeded. Symbol=%s, Current=%d, New=%d, Limit=%d",
                            order.getSymbol(), currentQuantity, newQuantity, positionLimit));
        }

        // Check net position limits for the account
        long netLimit = account.getNetExposureLimit();
        if (netLimit > 0) {
            long currentNet = account.getNetExposure();
            long orderValue = order.getQuantity() * (long)(order.getPrice() * 10000);
            long newNet = order.getSide() == OrderSide.BUY ? currentNet + orderValue : currentNet - orderValue;

            if (Math.abs(newNet) > netLimit) {
                return RiskCheckResult.fail(RiskCheckType.POSITION_LIMIT,
                        String.format("Net position limit exceeded. Current=%d, New=%d, Limit=%d",
                                currentNet, newNet, netLimit));
            }
        }

        return RiskCheckResult.pass(RiskCheckType.POSITION_LIMIT);
    }

    private long getPositionLimit(RiskAccount account, String symbol) {
        // Check symbol-specific limit first
        ProductLimit productLimit = account.getProductLimits().get(symbol);
        if (productLimit != null && productLimit.getPositionLimit() > 0) {
            return productLimit.getPositionLimit();
        }
        // Fall back to account default
        return account.getPositionLimit();
    }

    @Override
    public int getPriority() { return 10; }
}



// Supporting classes
class MarginRequirement {
    private final double initialMargin;
    private final double maintenanceMargin;

    public MarginRequirement(double initial, double maintenance) {
        this.initialMargin = initial;
        this.maintenanceMargin = maintenance;
    }

    public double getInitialMargin() { return initialMargin; }
    public double getMaintenanceMargin() { return maintenanceMargin; }
}

enum CreditRating { EXCELLENT, GOOD, FAIR, LIMITED, POOR }

class ProductLimit {
    private final String symbol;
    private final long positionLimit;
    private final long notionalLimit;

    public ProductLimit(String symbol, long positionLimit, long notionalLimit) {
        this.symbol = symbol;
        this.positionLimit = positionLimit;
        this.notionalLimit = notionalLimit;
    }

    public long getPositionLimit() { return positionLimit; }
    public long getNotionalLimit() { return notionalLimit; }
}