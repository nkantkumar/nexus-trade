package com.trading.risk;

package com.trading.risk.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order request from client/trading system
 */
public class OrderRequest {
    private final String orderId;
    private final String symbol;
    private final OrderSide side;
    private final long quantity;
    private final double price;
    private final OrderType orderType;
    private final TimeInForce timeInForce;
    private final long timestamp;
    private final String accountId;
    private final String clientId;
    private final Map<String, Object> attributes;

    // Optional fields for advanced order types
    private Double stopPrice;
    private Double triggerPrice;
    private Long expireTime;
    private String icebergDisplayQuantity;

    // Constructor for basic orders
    public OrderRequest(String symbol, OrderSide side, long quantity, double price) {
        this(UUID.randomUUID().toString(), symbol, side, quantity, price,
                OrderType.LIMIT, TimeInForce.DAY, null, null);
    }

    // Full constructor
    public OrderRequest(String orderId, String symbol, OrderSide side,
                        long quantity, double price, OrderType orderType) {
        this(orderId, symbol, side, quantity, price, orderType,
                TimeInForce.DAY, null, null);
    }

    public OrderRequest(String orderId, String symbol, OrderSide side,
                        long quantity, double price, OrderType orderType,
                        TimeInForce timeInForce, String accountId, String clientId) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.orderType = orderType;
        this.timeInForce = timeInForce;
        this.accountId = accountId;
        this.clientId = clientId;
        this.timestamp = System.currentTimeMillis();
        this.attributes = new HashMap<>();
    }

    // Getters
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public long getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public OrderType getOrderType() { return orderType; }
    public TimeInForce getTimeInForce() { return timeInForce; }
    public long getTimestamp() { return timestamp; }
    public String getAccountId() { return accountId; }
    public String getClientId() { return clientId; }
    public Double getStopPrice() { return stopPrice; }
    public Double getTriggerPrice() { return triggerPrice; }

    // Setters for optional fields
    public void setStopPrice(Double stopPrice) { this.stopPrice = stopPrice; }
    public void setTriggerPrice(Double triggerPrice) { this.triggerPrice = triggerPrice; }
    public void setExpireTime(Long expireTime) { this.expireTime = expireTime; }
    public void setIcebergDisplayQuantity(String icebergDisplayQuantity) {
        this.icebergDisplayQuantity = icebergDisplayQuantity;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    // Calculate order value
    public double getOrderValue() {
        return quantity * price;
    }

    public long getOrderValueInCents() {
        return (long)(quantity * price * 100);
    }

    @Override
    public String toString() {
        return String.format("OrderRequest{id=%s, sym=%s, side=%s, qty=%d, price=%.2f, type=%s}",
                orderId, symbol, side, quantity, price, orderType);
    }
}

