package com.trading.matching;

public interface OrderStrategy {
    ExecutionConstraints getConstraints(MarketState state);
}
