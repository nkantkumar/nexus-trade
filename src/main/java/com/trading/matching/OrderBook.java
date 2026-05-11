package com.trading.matching;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OrderBook {
    private final String symbol;
    private final NavigableMap<Double, List<Order>> bids;  // Sorted descending
    private final NavigableMap<Double, List<Order>> asks;  // Sorted ascending
    private final Map<String, Order> orderCache;
    private final ReentrantReadWriteLock lock;

    public OrderBook(String symbol) {
        this.symbol = symbol;
        this.bids = new TreeMap<>(Collections.reverseOrder());
        this.asks = new TreeMap<>();
        this.orderCache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock(true); // Fair lock
    }

    public void addOrder(Order order) {
        lock.writeLock().lock();
        try {
            orderCache.put(order.getOrderId(), order);
            NavigableMap<Double, List<Order>> book =
                    order.getSide() == Order.Side.BUY ? bids : asks;

            book.computeIfAbsent(order.getPrice(), k -> new LinkedList<>())
                    .add(order);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeOrder(String orderId) {
        lock.writeLock().lock();
        try {
            Order order = orderCache.remove(orderId);
            if (order != null) {
                NavigableMap<Double, List<Order>> book =
                        order.getSide() == Order.Side.BUY ? bids : asks;
                List<Order> ordersAtPrice = book.get(order.getPrice());
                if (ordersAtPrice != null) {
                    ordersAtPrice.remove(order);
                    if (ordersAtPrice.isEmpty()) {
                        book.remove(order.getPrice());
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getBestBid() {
        lock.readLock().lock();
        try {
            return bids.isEmpty() ? 0 : bids.firstKey();
        } finally {
            lock.readLock().unlock();
        }
    }

    public double getBestAsk() {
        lock.readLock().lock();
        try {
            return asks.isEmpty() ? 0 : asks.firstKey();
        } finally {
            lock.readLock().unlock();
        }
    }
}