package com.trading.matching;

/** Single execution against two resting orders at one price. */
public record Trade(
        String tradeId,
        String buyOrderId,
        String sellOrderId,
        double price,
        long quantity,
        long timestampNanos) {}
