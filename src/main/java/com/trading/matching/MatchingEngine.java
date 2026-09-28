package com.trading.matching;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-symbol order books with price-time priority at each level (FIFO via {@link OrderBook} lists).
 * Aggressor matching runs synchronously per order book; resting book mutations use per-book synchronization.
 */
public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol.toUpperCase(), OrderBook::new);
    }

    public List<Trade> submitOrder(Order incomingOrder) {
        String symbol = incomingOrder.getSymbol().toUpperCase();
        OrderBook book = getOrderBook(symbol);
        List<Trade> trades = new ArrayList<>();

        synchronized (book) {
            Order.Side side = incomingOrder.getSide();
            if (side == Order.Side.BUY) {
                while (incomingOrder.getQuantity() > 0) {
                    double bestAsk = book.getBestAsk();
                    if (bestAsk == 0 || incomingOrder.getPrice() < bestAsk) {
                        break; // No matching asks
                    }

                    List<Order> restingOrders = book.getOrdersAtPrice(Order.Side.SELL, bestAsk);
                    if (restingOrders.isEmpty()) {
                        break;
                    }

                    Iterator<Order> iter = restingOrders.iterator();
                    while (iter.hasNext() && incomingOrder.getQuantity() > 0) {
                        Order restingAsk = iter.next();
                        long fillQty = Math.min(incomingOrder.getQuantity(), restingAsk.getQuantity());

                        incomingOrder.fill(fillQty);
                        restingAsk.fill(fillQty);

                        trades.add(new Trade(
                                "TRD-" + UUID.randomUUID().toString().substring(0, 8),
                                incomingOrder.getOrderId(),
                                restingAsk.getOrderId(),
                                bestAsk,
                                fillQty,
                                System.nanoTime()
                        ));

                        if (restingAsk.getQuantity() == 0) {
                            iter.remove();
                        }
                    }

                    if (restingOrders.isEmpty()) {
                        book.removePriceLevel(Order.Side.SELL, bestAsk);
                    }
                }
            } else { // SELL
                while (incomingOrder.getQuantity() > 0) {
                    double bestBid = book.getBestBid();
                    if (bestBid == 0 || incomingOrder.getPrice() > bestBid) {
                        break; // No matching bids
                    }

                    List<Order> restingOrders = book.getOrdersAtPrice(Order.Side.BUY, bestBid);
                    if (restingOrders.isEmpty()) {
                        break;
                    }

                    Iterator<Order> iter = restingOrders.iterator();
                    while (iter.hasNext() && incomingOrder.getQuantity() > 0) {
                        Order restingBid = iter.next();
                        long fillQty = Math.min(incomingOrder.getQuantity(), restingBid.getQuantity());

                        incomingOrder.fill(fillQty);
                        restingBid.fill(fillQty);

                        trades.add(new Trade(
                                "TRD-" + UUID.randomUUID().toString().substring(0, 8),
                                restingBid.getOrderId(),
                                incomingOrder.getOrderId(),
                                bestBid,
                                fillQty,
                                System.nanoTime()
                        ));

                        if (restingBid.getQuantity() == 0) {
                            iter.remove();
                        }
                    }

                    if (restingOrders.isEmpty()) {
                        book.removePriceLevel(Order.Side.BUY, bestBid);
                    }
                }
            }

            // If incoming order still has remaining quantity, add to resting book
            if (incomingOrder.getQuantity() > 0) {
                book.addOrder(incomingOrder);
            }
        }

        return trades;
    }
}
