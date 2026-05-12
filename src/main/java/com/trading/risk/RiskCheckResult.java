package com.trading.risk;

/**
 * Risk check result
 */
public class RiskCheckResult {
    private final boolean passed;
    private final RiskCheckType checkType;
    private final String message;
    private final String details;
    private final long timestamp;

    public RiskCheckResult(boolean passed, RiskCheckType checkType, String message) {
        this(passed, checkType, message, null);
    }

    public RiskCheckResult(boolean passed, RiskCheckType checkType, String message, String details) {
        this.passed = passed;
        this.checkType = checkType;
        this.message = message;
        this.details = details;
        this.timestamp = System.nanoTime();
    }

    public static RiskCheckResult pass(RiskCheckType type) {
        return new RiskCheckResult(true, type, "Check passed");
    }

    public static RiskCheckResult fail(RiskCheckType type, String message) {
        return new RiskCheckResult(false, type, message);
    }

    // Getters
    public boolean isPassed() { return passed; }
    public RiskCheckType getCheckType() { return checkType; }
    public String getMessage() { return message; }
}
