package com.trading.settlement.reconciliation;

import com.trading.settlement.dvp.DvPModel;
import com.trading.settlement.dvp.SettlementInstruction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustodianReconciliationTest {

    @Test
    public void testReconciliationCleanMatch() {
        CustodianReconciliationService service = new CustodianReconciliationService();

        SettlementInstruction internal = new SettlementInstruction(
                "INST-REC-1", "TRD-1", "DEL-1", "RCV-1", "AAPL", 100, 15000.0, "USD", LocalDate.now(), DvPModel.MODEL_1
        );
        internal.setStatus(SettlementInstruction.Status.SETTLED);

        CustodianRecord external = new CustodianRecord(
                "DTCC-REF-100", "INST-REC-1", "RCV-1", "AAPL", 100, 15000.0, "USD", LocalDate.now(), "SETTLED"
        );

        ReconciliationReport report = service.runReconciliation(List.of(internal), List.of(external));

        assertEquals(1, report.matchedCount());
        assertEquals(0, report.breaks().size());
    }

    @Test
    public void testReconciliationDetectsQuantityAndCashBreaks() {
        CustodianReconciliationService service = new CustodianReconciliationService();

        SettlementInstruction internal = new SettlementInstruction(
                "INST-REC-BREAK", "TRD-2", "DEL-1", "RCV-1", "MSFT", 500, 200000.0, "USD", LocalDate.now(), DvPModel.MODEL_1
        );
        internal.setStatus(SettlementInstruction.Status.SETTLED);

        // Custodian reports 400 shares and $180,000 cash (breaks!)
        CustodianRecord external = new CustodianRecord(
                "DTCC-REF-200", "INST-REC-BREAK", "RCV-1", "MSFT", 400, 180000.0, "USD", LocalDate.now(), "SETTLED"
        );

        ReconciliationReport report = service.runReconciliation(List.of(internal), List.of(external));

        assertEquals(0, report.matchedCount());
        assertEquals(2, report.breaks().size());

        assertTrue(report.breaks().stream().anyMatch(b -> b.breakType() == ReconciliationReport.BreakType.QUANTITY_MISMATCH));
        assertTrue(report.breaks().stream().anyMatch(b -> b.breakType() == ReconciliationReport.BreakType.CASH_AMOUNT_MISMATCH));
    }
}
