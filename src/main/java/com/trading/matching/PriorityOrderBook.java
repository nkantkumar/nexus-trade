package com.trading.matching;

import java.util.Comparator;
import java.util.NavigableMap;
import java.util.PriorityQueue;
import java.util.TreeMap;

/**
 * Price-level buckets with time priority via {@link PriorityQueue}. Optional alternative to {@link OrderBook}.
 */
public class PriorityOrderBook {

    private final NavigableMap<Double, PriorityQueue<TimePriorityOrder>> bids;
    private final NavigableMap<Double, PriorityQueue<TimePriorityOrder>> asks;

    private static final Comparator<TimePriorityOrder> TIME_FIFO =
            Comparator.comparingLong(TimePriorityOrder::sequenceId);

    private static final class TimePriorityOrder extends Order {

        private final long sequenceId;

        TimePriorityOrder(Order order, long sequenceId) {
            super(order.getOrderId(), order.getSymbol(), order.getSide(), order.getPrice(), order.getQuantity());
            this.sequenceId = sequenceId;
        }

        long sequenceId() {
            return sequenceId;
        }
    }

    public PriorityOrderBook() {
        this.bids = new TreeMap<>(Comparator.reverseOrder());
        this.asks = new TreeMap<>();
    }

    public void addOrder(Order order, long sequenceId) {
        TimePriorityOrder tp = new TimePriorityOrder(order, sequenceId);
        NavigableMap<Double, PriorityQueue<TimePriorityOrder>> side =
                order.getSide() == Order.Side.BUY ? bids : asks;
        side.computeIfAbsent(order.getPrice(), p -> new PriorityQueue<>(TIME_FIFO)).add(tp);
    }
}
