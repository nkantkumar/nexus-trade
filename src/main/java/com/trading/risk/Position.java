package com.trading.risk;


import java.util.ArrayList;
import java.util.List;

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