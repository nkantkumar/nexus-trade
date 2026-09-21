package com.trading.settlement.reconciliation;

import com.trading.settlement.dvp.SettlementInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Custodian & Depository Reconciliation Engine.
 * Matches internal settlement instructions against external CSD feeds (DTCC, Euroclear)
 * and detects cash amount breaks, quantity breaks, and status mismatches.
 */
@Service
public class CustodianReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(CustodianReconciliationService.class);

    public ReconciliationReport runReconciliation(
            List<SettlementInstruction> internalInstructions,
            List<CustodianRecord> custodianRecords
    ) {
        String reportId = "RECON-" + UUID.randomUUID().toString().substring(0, 8);
        List<ReconciliationReport.DiscrepancyBreak> breaks = new ArrayList<>();

        Map<String, SettlementInstruction> internalMap = new HashMap<>();
        for (SettlementInstruction instr : internalInstructions) {
            internalMap.put(instr.getInstructionId(), instr);
        }

        Map<String, CustodianRecord> custodianMap = new HashMap<>();
        for (CustodianRecord record : custodianRecords) {
            custodianMap.put(record.internalInstructionId(), record);
        }

        int matchedCount = 0;

        // Check internal instructions against custodian records
        for (SettlementInstruction internal : internalInstructions) {
            CustodianRecord external = custodianMap.get(internal.getInstructionId());
            if (external == null) {
                breaks.add(new ReconciliationReport.DiscrepancyBreak(
                        internal.getInstructionId(),
                        "NONE",
                        ReconciliationReport.BreakType.MISSING_IN_CUSTODIAN_FEED,
                        "Instruction " + internal.getInstructionId() + " present internally but not found in custodian feed."
                ));
                continue;
            }

            boolean hasBreak = false;

            if (internal.getQuantity() != external.quantity()) {
                breaks.add(new ReconciliationReport.DiscrepancyBreak(
                        internal.getInstructionId(),
                        external.custodianReferenceId(),
                        ReconciliationReport.BreakType.QUANTITY_MISMATCH,
                        "Quantity mismatch: Internal=" + internal.getQuantity() + " vs Custodian=" + external.quantity()
                ));
                hasBreak = true;
            }

            if (Math.abs(internal.getCashAmount() - external.cashAmount()) > 0.01) {
                breaks.add(new ReconciliationReport.DiscrepancyBreak(
                        internal.getInstructionId(),
                        external.custodianReferenceId(),
                        ReconciliationReport.BreakType.CASH_AMOUNT_MISMATCH,
                        "Cash amount mismatch: Internal=" + internal.getCashAmount() + " vs Custodian=" + external.cashAmount()
                ));
                hasBreak = true;
            }

            if (!internal.getStatus().name().equalsIgnoreCase(external.custodianStatus())) {
                breaks.add(new ReconciliationReport.DiscrepancyBreak(
                        internal.getInstructionId(),
                        external.custodianReferenceId(),
                        ReconciliationReport.BreakType.STATUS_MISMATCH,
                        "Status mismatch: Internal=" + internal.getStatus().name() + " vs Custodian=" + external.custodianStatus()
                ));
                hasBreak = true;
            }

            if (!hasBreak) {
                matchedCount++;
            }
        }

        // Check for custodian records missing in internal ledger
        for (CustodianRecord external : custodianRecords) {
            if (!internalMap.containsKey(external.internalInstructionId())) {
                breaks.add(new ReconciliationReport.DiscrepancyBreak(
                        "NONE",
                        external.custodianReferenceId(),
                        ReconciliationReport.BreakType.MISSING_IN_INTERNAL_LEDGER,
                        "Custodian record " + external.custodianReferenceId() + " for instruction " + external.internalInstructionId() + " not found in internal ledger."
                ));
            }
        }

        log.info("Reconciliation Report {}: Total Internal={}, Total Custodian={}, Matched={}, Breaks={}",
                reportId, internalInstructions.size(), custodianRecords.size(), matchedCount, breaks.size());

        return new ReconciliationReport(reportId, Instant.now(), internalInstructions.size(), custodianRecords.size(), matchedCount, breaks);
    }
}
