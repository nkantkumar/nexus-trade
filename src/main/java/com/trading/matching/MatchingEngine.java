package com.trading.matching;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.matchingExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
        this.stats = new StatisticsCollector();
    }

    public CompletableFuture<ExecutionResult> processOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.nanoTime();

            try {
                // Validate order
                ValidationResult validation = validator.validate(order);
                if (!validation.isValid()) {
                    return new ExecutionResult(order, validation.getErrors());
                }

                // Get or create order book
                OrderBook book = orderBooks.computeIfAbsent(
                        order.getSymbol(),
                        OrderBook::new
                );

                // Execute matching
                List<Trade> trades = matchOrder(book, order);

                // Record statistics
                stats.recordMatchingTime(System.nanoTime() - startTime);
                stats.recordTrades(trades.size());

                return new ExecutionResult(order, trades);

            } catch (Exception e) {
                return new ExecutionResult(order, e.getMessage());
            }
        }, matchingExecutor);
    }

    private List<Trade> matchOrder(OrderBook book, Order order) {
        List<Trade> trades = new ArrayList<>();

        if (order.getSide() == Order.Side.BUY) {
            trades.addAll(matchBuyOrder(book, order));
        } else {
            trades.addAll(matchSellOrder(book, order));
        }

        // If order not fully filled, add to order book
        if (order.getStatus() == Order.OrderStatus.ACTIVE ||
                order.getStatus() == Order.OrderStatus.PARTIALLY_FILLED) {
            book.addOrder(order);
        }

        return trades;
    }

    private List<Trade> matchBuyOrder(OrderBook book, Order buyOrder) {
        List<Trade> trades = new ArrayList<>();
        long remainingQuantity = buyOrder.getQuantity();

        while (remainingQuantity > 0) {
            double bestAsk = book.getBestAsk();
            if (bestAsk == 0 || bestAsk > buyOrder.getPrice()) {
                break; // No more matches
            }

            List<Order> sellOrders = book.getOrdersAtPrice(Order.Side.SELL, bestAsk);
            Iterator<Order> iterator = sellOrders.iterator();

            while (iterator.hasNext() && remainingQuantity > 0) {
                Order sellOrder = iterator.next();
                long matchQuantity = Math.min(remainingQuantity, sellOrder.getQuantity());

                // Execute trade
                Trade trade = executeTrade(buyOrder, sellOrder, matchQuantity);
                trades.add(trade);

                remainingQuantity -= matchQuantity;

                if (sellOrder.getQuantity() == 0) {
                    iterator.remove();
                    book.removeOrder(sellOrder.getOrderId());
                }
            }

            if (sellOrders.isEmpty()) {
                book.removePriceLevel(Order.Side.SELL, bestAsk);
            }
        }

        buyOrder.fill(buyOrder.getQuantity() - remainingQuantity);
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
                System.nanoTime()
        );

        reporter.reportTrade(trade);
        return trade;
    }

    // similar matchSellOrder method
}
