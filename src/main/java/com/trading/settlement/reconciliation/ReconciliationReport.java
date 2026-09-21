package com.trading.settlement.reconciliation;

import java.time.Instant;
import java.util.List;

/**
 * Reconciliation audit report comparing internal settlement ledger against CSD feeds.
 */
public record ReconciliationReport(
        String reportId,
        Instant generatedAt,
        int totalInternalInstructions,
        int totalCustodianRecords,
        int matchedCount,
        List<DiscrepancyBreak> breaks
) {
    public record DiscrepancyBreak(
            String internalInstructionId,
            String custodianRefId,
            BreakType breakType,
            String description
    ) {}

    public enum BreakType {
        CASH_AMOUNT_MISMATCH,
        QUANTITY_MISMATCH,
        STATUS_MISMATCH,
        MISSING_IN_INTERNAL_LEDGER,
        MISSING_IN_CUSTODIAN_FEED
    }
}
