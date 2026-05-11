package com.trading.matching;

public final class StopOrderStrategy implements OrderStrategy {

    private final double stopPrice;
    private boolean triggered;

    public StopOrderStrategy(double stopPrice) {
        this.stopPrice = stopPrice;
    }

    @Override
    public ExecutionConstraints getConstraints(MarketState state) {
        if (!triggered && state.getLastPrice() >= stopPrice) {
            triggered = true;
        }
        return triggered ? new ExecutionConstraints(stopPrice, true) : new ExecutionConstraints(0, false);
    }
}
