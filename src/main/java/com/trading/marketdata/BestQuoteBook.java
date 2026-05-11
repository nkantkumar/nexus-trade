package com.trading.marketdata;

/**
 * Aggregates top-of-book quotes from market data feeds (e.g. FIX snapshot handler).
 */
public interface BestQuoteBook {

    void updateBestQuote(String symbol, double bestBid, long bidSize, double bestAsk, long askSize);
}
