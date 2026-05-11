package com.trading.matching;

import java.util.concurrent.ArrayBlockingQueue;

/** Tiny pooled mutable entry for hot-path prototypes — avoids Netty-internal pooling APIs. */
public final class OrderFlyweight {

    private final ArrayBlockingQueue<OrderEntry> pool;

    public OrderFlyweight(int poolSize) {
        this.pool = new ArrayBlockingQueue<>(Math.max(16, poolSize));
        for (int i = 0; i < Math.min(poolSize, 1024); i++) {
            pool.offer(new OrderEntry());
        }
    }

    public static final class OrderEntry {
        long orderId;
        long timestamp;
        byte side;
        int priceTicks;
        int quantity;
        int filledQuantity;
        byte status;

        void reset() {
            orderId = 0;
            timestamp = 0;
            side = 0;
            priceTicks = 0;
            quantity = 0;
            filledQuantity = 0;
            status = 0;
        }
    }

    public OrderEntry acquire() {
        OrderEntry e = pool.poll();
        return e != null ? e : new OrderEntry();
    }

    public void release(OrderEntry entry) {
        entry.reset();
        pool.offer(entry);
    }
}
