package com.trading.risk;



import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Account positions and risk limits
 */
@Getter
@Setter
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

}


