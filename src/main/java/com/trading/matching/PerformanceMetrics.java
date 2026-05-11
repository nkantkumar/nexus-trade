package com.trading.matching;

import java.util.concurrent.atomic.LongAdder;

public class PerformanceMetrics {
    private final LongAdder totalOrders = new LongAdder();
    private final LongAdder totalTrades = new LongAdder();
    private final LongAdder totalLatencyNs = new LongAdder();
    private final LongAdder peakLatencyNs = new LongAdder();

    public void recordOrder(long latencyNs) {
        totalOrders.increment();
        totalLatencyNs.add(latencyNs);

        // Update peak
        long currentPeak = peakLatencyNs.longValue();
        while (latencyNs > currentPeak) {
            if (peakLatencyNs.compareAndSet(currentPeak, latencyNs)) {
                break;
            }
            currentPeak = peakLatencyNs.longValue();
        }
    }

    public double getAverageLatencyMicros() {
        return (totalLatencyNs.doubleValue() / totalOrders.doubleValue()) / 1000.0;
    }

    // Throughput in orders per second
    public double getThroughput() {
        return totalOrders.doubleValue() * 1_000_000_000.0 / totalLatencyNs.doubleValue();
    }
}
