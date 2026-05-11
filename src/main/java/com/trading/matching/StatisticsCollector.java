package com.trading.matching;

import java.util.concurrent.atomic.AtomicLong;

/** Lightweight counters for matching latency and fills. */
public final class StatisticsCollector {

    private final AtomicLong totalMatchingNanos = new AtomicLong();
    private final AtomicLong tradeCount = new AtomicLong();
    private final AtomicLong sampleCount = new AtomicLong();

    public void recordMatchingTime(long nanos) {
        totalMatchingNanos.addAndGet(nanos);
        sampleCount.incrementAndGet();
    }

    public void recordTrades(int count) {
        tradeCount.addAndGet(count);
    }

    public long getTradeCount() {
        return tradeCount.get();
    }

    public double averageMatchingNanos() {
        long n = sampleCount.get();
        return n == 0 ? 0.0 : (double) totalMatchingNanos.get() / n;
    }
}
