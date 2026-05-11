package com.trading.matching;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Order {
    private final String orderId;
    private final long timestamp;
    private final Side side;
    private final double price;
    private long quantity;
    private OrderStatus status;

    public enum Side { BUY, SELL }
    public enum OrderStatus { ACTIVE, FILLED, PARTIALLY_FILLED, CANCELLED }

    public Order(String orderId, Side side, double price, long quantity) {
        this.orderId = orderId;
        this.timestamp = System.nanoTime();
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.status = OrderStatus.ACTIVE;
    }

    // Getters and setters
    public void fill(long fillQuantity) {
        this.quantity -= fillQuantity;
        if (this.quantity == 0) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }

    // equals and hashCode for orderId
}
