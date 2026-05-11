package com.trading.matching;

public interface OrderStrategy {
    ExecutionConstraints getConstraints(MarketState state);
}

public class LimitOrder implements OrderStrategy {
    private final double limitPrice;

    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        return new ExecutionConstraints(limitPrice, true);
    }
}

public class MarketOrder implements OrderStrategy {
    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        double bestPrice = state.getBestAvailablePrice();
        return new ExecutionConstraints(bestPrice, true); // Execute at market
    }
}

public class StopOrder implements OrderStrategy {
    private final double stopPrice;
    private boolean triggered = false;

    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        if (!triggered && state.getLastPrice() >= stopPrice) {
            triggered = true;
        }
        return triggered ? new ExecutionConstraints(stopPrice, true)
                : new ExecutionConstraints(0, false);
    }
}