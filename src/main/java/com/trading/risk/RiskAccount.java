package com.trading.risk;



import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Account positions and risk limits
 */
public class RiskAccount {
    private final String accountId;
    private final String accountName;
    private final AccountType accountType;
    private final Map<String, Position> positions;  // Symbol -> Position
    private final Map<String, ProductLimit> productLimits;

    // Risk limits
    private volatile long positionLimit;      // Max position per symbol
    private volatile long grossExposureLimit; // Total gross exposure
    private volatile long netExposureLimit;   // Total net exposure
    private volatile double maxOrderValue;    // Max single order value
    private volatile int maxOrderQuantity;    // Max single order quantity

    // Credit and margin
    private volatile double creditLimit;      // Total credit line
    private volatile double usedCredit;       // Currently used credit
    private volatile double maintenanceMargin; // Required maintenance margin
    private volatile double initialMargin;     // Required initial margin

    // Real-time metrics
    private final AtomicLong grossExposure = new AtomicLong(0);
    private final AtomicLong netExposure = new AtomicLong(0);
    private final AtomicLong totalOpenPnl = new AtomicLong(0);

    public RiskAccount(String accountId, String accountName, AccountType type) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountType = type;
        this.positions = new ConcurrentHashMap<>();
        this.productLimits = new ConcurrentHashMap<>();
    }

    // Getters and setters
    public String getAccountId() { return accountId; }
    public long getPositionLimit() { return positionLimit; }
    public void setPositionLimit(long limit) { this.positionLimit = limit; }

    public double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(double limit) { this.creditLimit = limit; }

    public long getGrossExposure() { return grossExposure.get(); }
    public long getNetExposure() { return netExposure.get(); }

    public void addPosition(Position position) {
        positions.put(position.getSymbol(), position);
        recalculateExposure();
    }

    public Position getPosition(String symbol) {
        return positions.getOrDefault(symbol, new Position(symbol, accountId));
    }

    private void recalculateExposure() {
        long gross = 0;
        long net = 0;

        for (Position pos : positions.values()) {
            long absValue = Math.abs(pos.getQuantity()) * (long)(pos.getCurrentPrice() * 10000);
            gross += absValue;
            net += pos.getQuantity() * (long)(pos.getCurrentPrice() * 10000);
        }

        grossExposure.set(gross);
        netExposure.set(net);
    }

    public void setGrossExposureLimit(int grossExposureLimit) {
        this.grossExposureLimit = grossExposureLimit;
    }

    public int getGrossExposureLimit() {
        return Math.toIntExact(grossExposureLimit);
    }

    public double getUsedCredit() {
        return usedCredit;
    }

    public void setUsedCredit(double usedCredit) {
        this.usedCredit = usedCredit;
    }

    public double getMaintenanceMargin() {
        return maintenanceMargin;
    }

    public void setMaintenanceMargin(double maintenanceMargin) {
        this.maintenanceMargin = maintenanceMargin;
    }

    public boolean getAccountType() {
        return accountType;
    }

    public void setAccountType(boolean accountType) {
        this.accountType = accountType;
    }

    public double getMaxOrderValue() {
        return maxOrderValue;
    }

    public void setMaxOrderValue(double maxOrderValue) {
        this.maxOrderValue = maxOrderValue;
    }

    public long getMaxOrderQuantity() {
        return maxOrderQuantity;
    }

    public void setMaxOrderQuantity(long maxOrderQuantity) {
        this.maxOrderQuantity = maxOrderQuantity;
    }
}

/**
 * Position for a specific symbol
 */
public class Position {
    private final String symbol;
    private final String accountId;
    private volatile long quantity;          // Positive = long, Negative = short
    private volatile double averagePrice;
    private volatile double currentPrice;
    private volatile double realizedPnl;
    private final List<PositionLeg> legs;     // For spread positions

    public Position(String symbol, String accountId) {
        this.symbol = symbol;
        this.accountId = accountId;
        this.legs = new ArrayList<>();
    }

    public void update(long deltaQuantity, double price) {
        if (quantity == 0) {
            quantity = deltaQuantity;
            averagePrice = price;
        } else {
            // Update average price based on new trade
            double newTotalValue = (quantity * averagePrice) + (deltaQuantity * price);
            quantity += deltaQuantity;
            averagePrice = quantity != 0 ? newTotalValue / quantity : 0;
        }
    }

    public void updatePrice(double newPrice) {
        this.currentPrice = newPrice;
    }

    public double getUnrealizedPnl() {
        if (quantity == 0) return 0;
        return quantity > 0 ? quantity * (currentPrice - averagePrice)
                : -quantity * (currentPrice - averagePrice);
    }

    // Getters
    public String getSymbol() { return symbol; }
    public long getQuantity() { return quantity; }
    public double getAveragePrice() { return averagePrice; }
    public double getCurrentPrice() { return currentPrice; }
    public double getRealizedPnl() { return realizedPnl; }
}

