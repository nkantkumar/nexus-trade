package com.trading.matching;

import java.util.Collections;
import java.util.List;

public final class ExecutionResult {

    private final Order order;
    private final List<Trade> trades;
    private final List<String> errors;

    public static ExecutionResult trades(Order order, List<Trade> trades) {
        return new ExecutionResult(order, trades, List.of());
    }

    public static ExecutionResult errors(Order order, List<String> errors) {
        return new ExecutionResult(order, List.of(), errors);
    }

    public static ExecutionResult error(Order order, String message) {
        return new ExecutionResult(order, List.of(), List.of(message));
    }

    private ExecutionResult(Order order, List<Trade> trades, List<String> errors) {
        this.order = order;
        this.trades = Collections.unmodifiableList(trades);
        this.errors = Collections.unmodifiableList(errors);
    }

    public Order getOrder() {
        return order;
    }

    public List<Trade> getTrades() {
        return trades;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
