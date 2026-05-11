package com.trading.marketdata;

/**
 * Callbacks from venue connectivity (FIX / OUCH). Wire to risk, portfolio, or matching as needed.
 */
public interface VenueEventListener {

    default void onExecutionFill(String clientOrderId, long cumQty, double avgPx) {}

    default void onExecutionReject(String clientOrderId, String reason) {}

    default void onExecutionCancelled(String clientOrderId) {}

    default void onMarketDataEntry(char mdEntryType, double price, long size) {}

    default void onTradeExecution(String orderId, long executedQuantity, double price, long matchNumber, long timestampNanos) {}
}
