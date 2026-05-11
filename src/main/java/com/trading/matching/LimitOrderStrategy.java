package com.trading.matching;

public final class LimitOrderStrategy implements OrderStrategy {

    private final double limitPrice;

    public LimitOrderStrategy(double limitPrice) {
        this.limitPrice = limitPrice;
    }

    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        return new ExecutionConstraints(limitPrice, true);
    }
}
