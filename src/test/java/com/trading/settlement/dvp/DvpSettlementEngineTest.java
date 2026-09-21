package com.trading.settlement.dvp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DvpSettlementEngineTest {

    @Test
    public void testDvPSettlementSuccessWithIsoMessages() {
        DvpSettlementEngine engine = new DvpSettlementEngine();

        // Setup accounts
        engine.initializeAccountCash("SELLER-ACC", 500.0);
        engine.initializeAccountCash("BUYER-ACC", 50000.0);
        engine.initializeAccountSecurities("SELLER-ACC", "NVDA", 200);
        engine.initializeAccountSecurities("BUYER-ACC", "NVDA", 0);

        SettlementInstruction instruction = engine.createInstruction(
                "OBLIG-100", "SELLER-ACC", "BUYER-ACC", "NVDA", 100, 12000.0, "USD", DvPModel.MODEL_1
        );

        boolean settled = engine.processDvPSettlement(instruction.getInstructionId());

        assertTrue(settled, "DvP Settlement should succeed when both cash and securities are available.");

        SettlementInstruction updated = engine.getInstruction(instruction.getInstructionId());
        assertEquals(SettlementInstruction.Status.SETTLED, updated.getStatus());
        assertNotNull(updated.getPacs008MessageId());
        assertNotNull(updated.getSese023MessageId());
        assertTrue(updated.getPacs008MessageId().startsWith("PACS008-"));
        assertTrue(updated.getSese023MessageId().startsWith("SESE023-"));

        // Check cash transfer: BUYER-ACC spent $12,000 (has $38,000), SELLER-ACC received $12,000 (has $12,500)
        assertEquals(38000.0, engine.getCashBalance("BUYER-ACC"), 0.01);
        assertEquals(12500.0, engine.getCashBalance("SELLER-ACC"), 0.01);

        // Check securities transfer: SELLER-ACC now has 100 shares, BUYER-ACC has 100 shares
        assertEquals(100, engine.getSecuritiesBalance("SELLER-ACC", "NVDA"));
        assertEquals(100, engine.getSecuritiesBalance("BUYER-ACC", "NVDA"));
    }

    @Test
    public void testDvPFailureCashShortage() {
        DvpSettlementEngine engine = new DvpSettlementEngine();
        engine.initializeAccountCash("BUYER-ACC", 100.0); // Insufficient cash ($100 vs $5000 required)
        engine.initializeAccountSecurities("SELLER-ACC", "MSFT", 100);

        SettlementInstruction instruction = engine.createInstruction(
                "OBLIG-SHORT-CASH", "SELLER-ACC", "BUYER-ACC", "MSFT", 50, 5000.0, "USD", DvPModel.MODEL_1
        );

        boolean result = engine.processDvPSettlement(instruction.getInstructionId());
        assertFalse(result);
        assertEquals(SettlementInstruction.Status.FAILED_CASH_SHORTAGE, instruction.getStatus());
    }

    @Test
    public void testIso20022MessageGeneration() {
        Iso20022MessageFactory.IsoMessage pacs = Iso20022MessageFactory.createPacs008CashTransfer(
                "INSTR-01", "BUYER-1", "SELLER-1", 1500.0, "USD"
        );
        assertTrue(pacs.xmlPayload().contains("<FIToFICstmrCdtTrf>"));
        assertTrue(pacs.xmlPayload().contains("INSTR-01"));
        assertTrue(pacs.xmlPayload().contains("1500.00"));

        Iso20022MessageFactory.IsoMessage sese = Iso20022MessageFactory.createSese023SecuritiesSettlement(
                "INSTR-01", "SELLER-1", "BUYER-1", "US0378331005", 50
        );
        assertTrue(sese.xmlPayload().contains("<SctiesSttlmTxInstr>"));
        assertTrue(sese.xmlPayload().contains("<Unit>50</Unit>"));
    }
}
