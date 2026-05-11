package com.trading.matching;

import lombok.Getter;

@Getter
public class Order {

    private final String orderId;
    private final String symbol;
    private final long timestamp;
    private final Side side;
    private final double price;
    private long quantity;
    private OrderStatus status;

    public enum Side {
        BUY,
        SELL
    }

    public enum OrderStatus {
        ACTIVE,
        FILLED,
        PARTIALLY_FILLED,
        CANCELLED
    }

    public Order(String orderId, Side side, double price, long quantity) {
        this(orderId, "", side, price, quantity);
    }

    public Order(String orderId, String symbol, Side side, double price, long quantity) {
        this.orderId = orderId;
        this.symbol = symbol == null ? "" : symbol;
        this.timestamp = System.nanoTime();
        this.side = side;
        this.price = price;
        this.quantity = quantity;
        this.status = OrderStatus.ACTIVE;
    }

    public void fill(long fillQuantity) {
        if (fillQuantity <= 0) {
            return;
        }
        if (fillQuantity > this.quantity) {
            throw new IllegalArgumentException("fill exceeds remaining quantity");
        }
        this.quantity -= fillQuantity;
        if (this.quantity == 0) {
            this.status = OrderStatus.FILLED;
        } else {
            this.status = OrderStatus.PARTIALLY_FILLED;
        }
    }
}
