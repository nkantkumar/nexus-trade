package com.trading.risk;

public sealed interface RiskResult permits RiskResult.Approved, RiskResult.Rejected, RiskResult.Breach {
    record Approved(String detail) implements RiskResult {}
    record Rejected(String reason) implements RiskResult {}
    record Breach(String severity, String reason) implements RiskResult {}
}
