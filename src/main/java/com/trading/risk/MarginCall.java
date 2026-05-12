package com.trading.risk;


import java.util.UUID;

public class MarginCall {
    private final String accountId;
    private final double shortfallAmount;
    private final long timestamp;
    private MarginCallStatus status;
    private String response;
    private long responseTime;

    public MarginCall(String accountId, double shortfallAmount, long timestamp) {
        this.accountId = accountId;
        this.shortfallAmount = shortfallAmount;
        this.timestamp = timestamp;
        this.status = MarginCallStatus.ISSUED;
    }

    public String getAccountId() { return accountId; }
    public double getShortfallAmount() { return shortfallAmount; }
    public long getTimestamp() { return timestamp; }
    public MarginCallStatus getStatus() { return status; }
    public String getResponse() { return response; }

    public void setStatus(MarginCallStatus status) { this.status = status; }
    public void setResponse(String response) {
        this.response = response;
        this.responseTime = System.currentTimeMillis();
    }

    public boolean isResolved() {
        return status == MarginCallStatus.RESOLVED || status == MarginCallStatus.LIQUIDATED;
    }
}

/**
 * Margin call status
 */
public enum MarginCallStatus {
    ISSUED,
    ACKNOWLEDGED,
    RESOLVED,
    LIQUIDATED,
    ESCALATED
}

/**
 * Risk breach record
 */
public class RiskBreach {
    private final String breachId;
    private final String accountId;
    private final String symbol;
    private final RiskCheckType checkType;
    private final String message;
    private final long timestamp;
    private RiskBreachSeverity severity;
    private boolean acknowledged;
    private String resolution;

    public RiskBreach(String accountId, String symbol, RiskCheckType checkType,
                      String message, long timestamp) {
        this.breachId = UUID.randomUUID().toString();
        this.accountId = accountId;
        this.symbol = symbol;
        this.checkType = checkType;
        this.message = message;
        this.timestamp = timestamp;
        this.severity = determineSeverity(checkType);
        this.acknowledged = false;
    }

    private RiskBreachSeverity determineSeverity(RiskCheckType checkType) {
        switch (checkType) {
            case MARGIN:
            case CREDIT:
                return RiskBreachSeverity.CRITICAL;
            case POSITION_LIMIT:
            case EXPOSURE_LIMIT:
                return RiskBreachSeverity.HIGH;
            case FAT_FINGER:
                return RiskBreachSeverity.MEDIUM;
            default:
                return RiskBreachSeverity.LOW;
        }
    }

    // Getters
    public String getBreachId() { return breachId; }
    public String getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public RiskCheckType getCheckType() { return checkType; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public RiskBreachSeverity getSeverity() { return severity; }
    public boolean isAcknowledged() { return acknowledged; }
    public String getResolution() { return resolution; }

    public void acknowledge() { this.acknowledged = true; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}

