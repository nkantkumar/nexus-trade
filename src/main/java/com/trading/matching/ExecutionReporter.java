package com.trading.matching;

/** Hook for downstream trade publication (Chronicle, Kafka, FIX). Default no-op. */
public final class ExecutionReporter {

    public void reportTrade(Trade trade) {
        // integrate telemetry / messaging here
    }
}
