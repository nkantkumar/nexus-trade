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

