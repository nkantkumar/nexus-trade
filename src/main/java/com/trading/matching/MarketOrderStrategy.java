package com.trading.matching;

public final class MarketOrderStrategy implements OrderStrategy {

    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        double px = state.getBestAvailablePrice();
        return new ExecutionConstraints(px, px > 0);
    }
}
