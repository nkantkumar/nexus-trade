package com.trading.matching;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class PerformanceMetrics {

    private final LongAdder totalOrders = new LongAdder();
    private final LongAdder totalTrades = new LongAdder();
    private final LongAdder totalLatencyNs = new LongAdder();
    private final AtomicLong peakLatencyNs = new AtomicLong();

    public void recordOrder(long latencyNs) {
        totalOrders.increment();
        totalLatencyNs.add(latencyNs);

        long currentPeak = peakLatencyNs.get();
        while (latencyNs > currentPeak && !peakLatencyNs.compareAndSet(currentPeak, latencyNs)) {
            currentPeak = peakLatencyNs.get();
        }
    }

    public void recordTrade() {
        totalTrades.increment();
    }

    public double getAverageLatencyMicros() {
        long n = totalOrders.sum();
        return n == 0 ? 0.0 : (totalLatencyNs.doubleValue() / n) / 1000.0;
    }

    public double getThroughputOrdersPerSecond() {
        long sumNs = totalLatencyNs.sum();
        return sumNs == 0 ? 0.0 : totalOrders.doubleValue() * 1_000_000_000.0 / sumNs;
    }

    public long getPeakLatencyNanos() {
        return peakLatencyNs.get();
    }
}
