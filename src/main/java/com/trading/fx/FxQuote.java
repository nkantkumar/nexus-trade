package com.trading.fx;

import java.time.Instant;

public record FxQuote(
        String symbol,
        double bid,
        double ask,
        double mid,
        double spreadPips,
        Instant timestamp,
        String liquidityProvider
) {
    public static FxQuote create(String symbol, double bid, double ask, String liquidityProvider, double pipSize) {
        double mid = (bid + ask) / 2.0;
        double spreadPips = (ask - bid) / pipSize;
        return new FxQuote(symbol, bid, ask, mid, spreadPips, Instant.now(), liquidityProvider);
    }
}
