package com.trading.matching;

import io.netty.util.internal.ObjectPool;

// Using flyweight pattern and object pooling
public class OrderFlyweight {
    private static final ObjectPool<OrderEntry> orderPool =
            new ObjectPool<>(1024);

    public static class OrderEntry {
        long orderId;
        long timestamp;
        byte side; // 0 = BUY, 1 = SELL
        int price; // Using integer for fixed-point
        int quantity;
        int filledQuantity;
        byte status;

        public void reset() {
            orderId = 0;
            timestamp = 0;
            side = 0;
            price = 0;
            quantity = 0;
            filledQuantity = 0;
            status = 0;
        }
    }

    public OrderEntry createOrder(/* parameters */) {
        OrderEntry order = orderPool.acquire();
        // Initialize fields
        return order;
    }
}
