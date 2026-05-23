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
    private final String symbol;
    private final boolean dayTrade;

    public Trade(String buyOrderId, String sellOrderId, double price, long quantity) {
        this(UUID.randomUUID().toString(), buyOrderId, sellOrderId, price, quantity, System.nanoTime(), "Unknown", false);
    }

    public Trade(String tradeId, String buyOrderId, String sellOrderId,
                 double price, long quantity, long timestamp) {
        this(tradeId, buyOrderId, sellOrderId, price, quantity, timestamp, "Unknown", false);
    }

    public Trade(String tradeId, String buyOrderId, String sellOrderId,
                 double price, long quantity, long timestamp, String symbol, boolean dayTrade) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.symbol = symbol;
        this.dayTrade = dayTrade;
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public double getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }
    public String getSymbol() { return symbol != null ? symbol : "Unknown"; }
    public boolean isDayTrade() { return dayTrade; }

    public double getValue() {
        return price * quantity;
    }
}
