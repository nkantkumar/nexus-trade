package com.trading.risk;

/**
 * Risk Alert Severity Levels
 */
public enum RiskAlertSeverity {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High"),
    CRITICAL(4, "Critical");

    private final int level;
    private final String displayName;

    RiskAlertSeverity(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }

    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }

    public boolean isGreaterThan(RiskAlertSeverity other) {
        return this.level > other.level;
    }

    public boolean isLessThan(RiskAlertSeverity other) {
        return this.level < other.level;
    }
}
