package com.trading.risk;

/**
 * Order submission result
 */
public class OrderSubmissionResult {
    private final boolean accepted;
    private final String orderId;
    private final String message;
    private final long timestamp;

    private OrderSubmissionResult(boolean accepted, String orderId, String message) {
        this.accepted = accepted;
        this.orderId = orderId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static OrderSubmissionResult accepted(String orderId) {
        return new OrderSubmissionResult(true, orderId, "Order accepted");
    }

    public static OrderSubmissionResult rejected(String reason) {
        return new OrderSubmissionResult(false, null, reason);
    }

    public boolean isAccepted() { return accepted; }
    public String getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
