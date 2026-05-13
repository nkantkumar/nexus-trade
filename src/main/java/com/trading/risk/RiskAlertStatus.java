package com.trading.risk;

/**
 * Risk Alert Status
 */
public enum RiskAlertStatus {
    ACTIVE("Active", "Alert has been triggered and not yet addressed"),
    ACKNOWLEDGED("Acknowledged", "Alert has been seen but not resolved"),
    RESOLVED("Resolved", "Alert has been resolved"),
    ESCALATED("Escalated", "Alert has been escalated to higher authority"),
    IGNORED("Ignored", "Alert has been marked as false positive");

    private final String displayName;
    private final String description;

    RiskAlertStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
