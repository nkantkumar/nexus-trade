package com.trading.risk;


import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;
import java.util.*;

/**
 * Collects and reports risk engine metrics
 */
public class RiskMetricsCollector {
    private final LongAdder totalChecks = new LongAdder();
    private final LongAdder passedChecks = new LongAdder();
    private final LongAdder failedChecks = new LongAdder();
    private final LongAdder totalLatencyNs = new LongAdder();
    private final AtomicLong peakLatencyNs = new AtomicLong(0);
    private final Map<RiskCheckType, LongAdder> failuresByType = new ConcurrentHashMap<>();

    private final AtomicInteger activeAlertCount = new AtomicInteger(0);
    private final List<RiskMetrics> historicalMetrics = new ArrayList<>();
    private long lastResetTime = System.currentTimeMillis();

    public RiskMetricsCollector() {
        // Initialize failure counters
        for (RiskCheckType type : RiskCheckType.values()) {
            failuresByType.put(type, new LongAdder());
        }
    }

    public void recordCheck(long latencyNs, boolean passed) {
        totalChecks.increment();
        totalLatencyNs.add(latencyNs);

        if (passed) {
            passedChecks.increment();
        } else {
            failedChecks.increment();
        }

        // Update peak latency
        long currentPeak = peakLatencyNs.get();
        while (latencyNs > currentPeak) {
            if (peakLatencyNs.compareAndSet(currentPeak, latencyNs)) {
                break;
            }
            currentPeak = peakLatencyNs.get();
        }
    }

    public void recordFailure(RiskCheckType type) {
        failuresByType.get(type).increment();
    }

    public void setActiveAlertCount(int count) {
        activeAlertCount.set(count);
    }

    public RiskMetrics getLatestMetrics() {
        long total = totalChecks.sum();
        long passed = passedChecks.sum();
        long failed = failedChecks.sum();
        long totalLatency = totalLatencyNs.sum();
        long peakLatency = peakLatencyNs.longValue();

        Map<RiskCheckType, Long> failures = new HashMap<>();
        for (Map.Entry<RiskCheckType, LongAdder> entry : failuresByType.entrySet()) {
            failures.put(entry.getKey(), entry.getValue().sum());
        }

        return new RiskMetrics(
                total,
                passed,
                failed,
                total > 0 ? (double) passed / total : 1.0,
                total > 0 ? (totalLatency / total) / 1000.0 : 0, // microseconds
                peakLatency / 1000.0,
                failures,
                activeAlertCount.get(),
                System.currentTimeMillis() - lastResetTime
        );
    }

    public void generateReport() {
        RiskMetrics metrics = getLatestMetrics();
        System.out.println("\n=== RISK ENGINE METRICS REPORT ===");
        System.out.printf("Period: %d seconds%n", metrics.getUptimeSeconds());
        System.out.printf("Total Checks: %d%n", metrics.getTotalChecks());
        System.out.printf("Pass Rate: %.2f%%%n", metrics.getPassRate() * 100);
        System.out.printf("Avg Latency: %.2f μs%n", metrics.getAverageLatencyMicros());
        System.out.printf("Peak Latency: %.2f μs%n", metrics.getPeakLatencyMicros());
        System.out.printf("Active Alerts: %d%n", metrics.getActiveAlertCount());

        System.out.println("\nFailures by Type:");
        for (Map.Entry<RiskCheckType, Long> entry : metrics.getFailuresByType().entrySet()) {
            if (entry.getValue() > 0) {
                System.out.printf("  %s: %d%n", entry.getKey(), entry.getValue());
            }
        }

        // Store historical snapshot
        historicalMetrics.add(metrics);
        if (historicalMetrics.size() > 100) {
            historicalMetrics.remove(0);
        }
    }

    public void reset() {
        totalChecks.reset();
        passedChecks.reset();
        failedChecks.reset();
        totalLatencyNs.reset();
        peakLatencyNs.set(0L);
        for (LongAdder adder : failuresByType.values()) {
            adder.reset();
        }
        activeAlertCount.set(0);
        lastResetTime = System.currentTimeMillis();
    }
}

