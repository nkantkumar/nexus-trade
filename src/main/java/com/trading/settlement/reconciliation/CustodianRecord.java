package com.trading.settlement.reconciliation;

import java.time.LocalDate;

/**
 * External feed record received from Custodian / Central Securities Depository (CSD)
 * (e.g., DTCC, Euroclear, Clearstream) for end-of-day reconciliation.
 */
public record CustodianRecord(
        String custodianReferenceId,
        String internalInstructionId,
        String accountId,
        String symbol,
        long quantity,
        double cashAmount,
        String currency,
        LocalDate settlementDate,
        String custodianStatus
) {}
