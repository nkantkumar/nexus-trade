package com.trading.matching;

public record MarketState(double lastPrice, double bestBid, double bestAsk) {

    public double getLastPrice() {
        return lastPrice;
    }

    public double getBestAvailablePrice() {
        return bestAsk > 0 ? bestAsk : lastPrice;
    }
}
