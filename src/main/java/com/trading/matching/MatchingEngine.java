package com.trading.matching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Per-symbol order books with price-time priority at each level (FIFO via {@link OrderBook} lists).
 * Aggressor matching runs asynchronously; resting book mutations use per-book locks inside {@link OrderBook}.
 */
public class MatchingEngine {

    private final Map<String, OrderBook> orderBooks;
    private final OrderValidator validator;
    private final ExecutionReporter reporter;
    private final ExecutorService matchingExecutor;
    private final StatisticsCollector stats;

    public MatchingEngine() {
        this.orderBooks = new ConcurrentHashMap<>();
        this.validator = new OrderValidator();
        this.reporter = new ExecutionReporter();
        this.matchingExecutor =
                Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
        this.stats = new StatisticsCollector();
    }

    public CompletableFuture<ExecutionResult> processOrder(Order order) {
        return CompletableFuture.supplyAsync(
                () -> {
                    long startTime = System.nanoTime();
                    try {
                        ValidationResult validation = validator.validate(order);
                        if (!validation.isValid()) {
                            return ExecutionResult.errors(order, validation.getErrors());
                        }

                        OrderBook book =
                                orderBooks.computeIfAbsent(order.getSymbol(), OrderBook::new);

                        final List<Trade> trades;
                        synchronized (book) {
                            trades =
                                    order.getSide() == Order.Side.BUY
                                            ? matchBuyOrder(book, order)
                                            : matchSellOrder(book, order);

                            if (order.getStatus() == Order.OrderStatus.ACTIVE
                                    || order.getStatus() == Order.OrderStatus.PARTIALLY_FILLED) {
                                book.addOrder(order);
                            }
                        }

                        stats.recordMatchingTime(System.nanoTime() - startTime);
                        stats.recordTrades(trades.size());
                        return ExecutionResult.trades(order, trades);
                    } catch (Exception e) {
                        return ExecutionResult.error(order, e.getMessage());
                    }
                },
                matchingExecutor);
    }

    public void shutdown() {
        matchingExecutor.shutdown();
    }

    private List<Trade> matchBuyOrder(OrderBook book, Order buyOrder) {
        List<Trade> trades = new ArrayList<>();
        while (buyOrder.getQuantity() > 0) {
            double bestAsk = book.getBestAsk();
            if (bestAsk <= 0 || bestAsk > buyOrder.getPrice()) {
                break;
            }
            List<Order> sellOrders = book.getOrdersAtPrice(Order.Side.SELL, bestAsk);
            if (sellOrders == null || sellOrders.isEmpty()) {
                book.removePriceLevel(Order.Side.SELL, bestAsk);
                continue;
            }
            Iterator<Order> iterator = sellOrders.iterator();
            while (iterator.hasNext() && buyOrder.getQuantity() > 0) {
                Order sellOrder = iterator.next();
                long matchQty = Math.min(buyOrder.getQuantity(), sellOrder.getQuantity());
                if (matchQty <= 0) {
                    continue;
                }
                trades.add(executeTrade(buyOrder, sellOrder, matchQty));
                if (sellOrder.getQuantity() == 0) {
                    iterator.remove();
                    book.removeOrder(sellOrder.getOrderId());
                }
            }
            if (sellOrders.isEmpty()) {
                book.removePriceLevel(Order.Side.SELL, bestAsk);
            }
        }
        return trades;
    }

    private List<Trade> matchSellOrder(OrderBook book, Order sellOrder) {
        List<Trade> trades = new ArrayList<>();
        while (sellOrder.getQuantity() > 0) {
            double bestBid = book.getBestBid();
            if (bestBid <= 0 || bestBid < sellOrder.getPrice()) {
                break;
            }
            List<Order> buyOrders = book.getOrdersAtPrice(Order.Side.BUY, bestBid);
            if (buyOrders == null || buyOrders.isEmpty()) {
                book.removePriceLevel(Order.Side.BUY, bestBid);
                continue;
            }
            Iterator<Order> iterator = buyOrders.iterator();
            while (iterator.hasNext() && sellOrder.getQuantity() > 0) {
                Order buyOrder = iterator.next();
                long matchQty = Math.min(sellOrder.getQuantity(), buyOrder.getQuantity());
                if (matchQty <= 0) {
                    continue;
                }
                trades.add(executeTrade(buyOrder, sellOrder, matchQty));
                if (buyOrder.getQuantity() == 0) {
                    iterator.remove();
                    book.removeOrder(buyOrder.getOrderId());
                }
            }
            if (buyOrders.isEmpty()) {
                book.removePriceLevel(Order.Side.BUY, bestBid);
            }
        }
        return trades;
    }

    private Trade executeTrade(Order buyOrder, Order sellOrder, long quantity) {
        double price = sellOrder.getPrice();
        buyOrder.fill(quantity);
        sellOrder.fill(quantity);
        Trade trade = new Trade(
                UUID.randomUUID().toString(),
                buyOrder.getOrderId(),
                sellOrder.getOrderId(),
                price,
                quantity,
                System.nanoTime());
        reporter.reportTrade(trade);
        return trade;
    }
}
