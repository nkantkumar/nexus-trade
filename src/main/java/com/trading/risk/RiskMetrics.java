package com.trading.risk;

import java.util.Map; /**
 * Risk metrics snapshot
 */
public class RiskMetrics {
    private final long totalChecks;
    private final long passedChecks;
    private final long failedChecks;
    private final double passRate;
    private final double averageLatencyMicros;
    private final double peakLatencyMicros;
    private final Map<RiskCheckType, Long> failuresByType;
    private final int activeAlertCount;
    private final long uptimeSeconds;

    public RiskMetrics(long totalChecks, long passedChecks, long failedChecks,
                       double passRate, double averageLatencyMicros, double peakLatencyMicros,
                       Map<RiskCheckType, Long> failuresByType, int activeAlertCount, long uptimeSeconds) {
        this.totalChecks = totalChecks;
        this.passedChecks = passedChecks;
        this.failedChecks = failedChecks;
        this.passRate = passRate;
        this.averageLatencyMicros = averageLatencyMicros;
        this.peakLatencyMicros = peakLatencyMicros;
        this.failuresByType = failuresByType;
        this.activeAlertCount = activeAlertCount;
        this.uptimeSeconds = uptimeSeconds;
    }

    // Getters
    public long getTotalChecks() { return totalChecks; }
    public long getPassedChecks() { return passedChecks; }
    public long getFailedChecks() { return failedChecks; }
    public double getPassRate() { return passRate; }
    public double getAverageLatencyMicros() { return averageLatencyMicros; }
    public double getPeakLatencyMicros() { return peakLatencyMicros; }
    public Map<RiskCheckType, Long> getFailuresByType() { return failuresByType; }
    public int getActiveAlertCount() { return activeAlertCount; }
    public long getUptimeSeconds() { return uptimeSeconds; }
}
