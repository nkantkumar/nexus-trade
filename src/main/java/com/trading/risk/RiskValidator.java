package com.trading.risk;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for all risk validators
 */
public interface RiskValidator {
    RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context);
    int getPriority();  // Lower = higher priority
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