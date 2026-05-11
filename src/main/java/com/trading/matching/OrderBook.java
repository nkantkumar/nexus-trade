package com.trading.matching;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Price-sorted book with FIFO queues per price level. All mutating paths used by the matching engine run under
 * {@code synchronized (book)} from {@link MatchingEngine}, consistent with {@code synchronized} methods here.
 */
public class OrderBook {

    private final String symbol;
    private final NavigableMap<Double, List<Order>> bids;
    private final NavigableMap<Double, List<Order>> asks;
    private final Map<String, Order> orderCache;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new TreeMap<>(java.util.Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.orderCache = new ConcurrentHashMap<>();
    }

    public synchronized void addOrder(Order order) {
        orderCache.put(order.getOrderId(), order);
        NavigableMap<Double, List<Order>> sideBook = order.getSide() == Order.Side.BUY ? bids : asks;
        sideBook.computeIfAbsent(order.getPrice(), k -> new LinkedList<>()).add(order);
    }

    public synchronized void removeOrder(String orderId) {
        Order order = orderCache.remove(orderId);
        if (order != null) {
            NavigableMap<Double, List<Order>> sideBook = order.getSide() == Order.Side.BUY ? bids : asks;
            List<Order> atPrice = sideBook.get(order.getPrice());
            if (atPrice != null) {
                atPrice.remove(order);
                if (atPrice.isEmpty()) {
                    sideBook.remove(order.getPrice());
                }
            }
        }
    }

    public synchronized double getBestBid() {
        return bids.isEmpty() ? 0 : bids.firstKey();
    }

    public synchronized double getBestAsk() {
        return asks.isEmpty() ? 0 : asks.firstKey();
    }

    /** Must only be called while holding {@code synchronized (this)} (see {@link MatchingEngine}). */
    public synchronized List<Order> getOrdersAtPrice(Order.Side side, double price) {
        NavigableMap<Double, List<Order>> sideBook = side == Order.Side.BUY ? bids : asks;
        List<Order> at = sideBook.get(price);
        return at == null ? List.of() : at;
    }

    public synchronized void removePriceLevel(Order.Side side, double price) {
        NavigableMap<Double, List<Order>> sideBook = side == Order.Side.BUY ? bids : asks;
        List<Order> removed = sideBook.remove(price);
        if (removed != null) {
            for (Order o : removed) {
                orderCache.remove(o.getOrderId());
            }
        }
    }

    public String getSymbol() {
        return symbol;
    }
}
