package com.trading.risk;

/**
 * Trade report
 */
public class TradeReport {
    private final String tradeId;
    private final String orderId;
    private final double price;
    private final long quantity;
    private final long timestamp;

    public TradeReport(String tradeId, String orderId, double price, long quantity) {
        this.tradeId = tradeId;
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getOrderId() { return orderId; }
    public double getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
}
