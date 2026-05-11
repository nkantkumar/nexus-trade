package com.trading.matching;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

public class LockFreeOrderBook {
    private final AtomicReference<OrderBookSnapshot> snapshotRef;
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    private static class OrderBookSnapshot {
        final long version;
        final Map<Double, OrderNode> bids;
        final Map<Double, OrderNode> asks;

        OrderBookSnapshot(long version, Map<Double, OrderNode> bids,
                          Map<Double, OrderNode> asks) {
            this.version = version;
            this.bids = bids;
            this.asks = asks;
        }
    }

    public boolean addOrder(Order order) {
        while (true) {
            OrderBookSnapshot currentSnapshot = snapshotRef.get();
            OrderBookSnapshot newSnapshot = createNewSnapshot(currentSnapshot, order);

            if (snapshotRef.compareAndSet(currentSnapshot, newSnapshot)) {
                return true;
            }
            // Retry on conflict
        }
    }
}
