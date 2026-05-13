package com.trading.risk;

import java.util.UUID; /**
 * Trade execution for internal matching engine
 */
public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final double price;
    private final long quantity;
    private final long timestamp;

    public Trade(String buyOrderId, String sellOrderId, double price, long quantity) {
        this(UUID.randomUUID().toString(), buyOrderId, sellOrderId, price, quantity, System.nanoTime());
    }

    public Trade(String tradeId, String buyOrderId, String sellOrderId,
                 double price, long quantity, long timestamp) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public double getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }

    public double getValue() {
        return price * quantity;
    }
}
