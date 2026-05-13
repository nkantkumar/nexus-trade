package com.trading.risk;

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
