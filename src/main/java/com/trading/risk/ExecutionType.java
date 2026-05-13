package com.trading.risk;

/**
 * Execution type
 */
public enum ExecutionType {
    NEW,
    PARTIAL_FILL,
    FILL,
    CANCELLED,
    REPLACED,
    REJECTED,
    EXPIRED,
    TRADE
}
