package com.trading.matching;

public class PriorityOrderBook {
    private final NavigableMap<Double, PriorityQueue<TimePriorityOrder>> bids;
    private final NavigableMap<Double, PriorityQueue<TimePriorityOrder>> asks;

    // Order with timestamp priority
    private static class TimePriorityOrder extends Order {
        private final long sequenceId;

        public TimePriorityOrder(Order order, long sequenceId) {
            super(order.getOrderId(), order.getSide(),
                    order.getPrice(), order.getQuantity());
            this.sequenceId = sequenceId;
        }

        public int compareTo(TimePriorityOrder other) {
            return Long.compare(this.sequenceId, other.sequenceId);
        }
    }

    public void addOrder(Order order, long sequenceId) {
        TimePriorityOrder tpOrder = new TimePriorityOrder(order, sequenceId);
        NavigableMap<Double, PriorityQueue<TimePriorityOrder>> book =
                order.getSide() == Order.Side.BUY ? bids : asks;

        book.computeIfAbsent(order.getPrice(), k ->
                new PriorityQueue<>()).add(tpOrder);
    }
}