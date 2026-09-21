package com.trading.settlement;

import com.trading.settlement.aeron.AeronTradeCodec;
import com.trading.settlement.allocation.AllocationInstruction;
import com.trading.settlement.allocation.BlockTradeAllocationEngine;
import com.trading.settlement.allocation.EquityTradeCaptureService;
import com.trading.settlement.clearing.CcpClearingEngine;
import com.trading.settlement.clearing.ClearedTrade;
import com.trading.settlement.clearing.NettedObligation;
import com.trading.settlement.dvp.DvpSettlementEngine;
import com.trading.settlement.dvp.DvPModel;
import com.trading.settlement.dvp.SettlementInstruction;
import com.trading.settlement.equities.CorporateActionAdjuster;
import com.trading.settlement.equities.EquityFailManagementEngine;
import com.trading.settlement.reconciliation.CustodianReconciliationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SettlementServiceTest {

    private SettlementService service;

    @BeforeEach
    public void setUp() {
        EquityTradeCaptureService captureService = new EquityTradeCaptureService();
        BlockTradeAllocationEngine allocationEngine = new BlockTradeAllocationEngine();
        CcpClearingEngine clearingEngine = new CcpClearingEngine();
        DvpSettlementEngine dvpEngine = new DvpSettlementEngine();
        CorporateActionAdjuster corporateActionAdjuster = new CorporateActionAdjuster();
        EquityFailManagementEngine failManagementEngine = new EquityFailManagementEngine();
        CustodianReconciliationService reconciliationService = new CustodianReconciliationService();

        service = new SettlementService(
                captureService, allocationEngine, clearingEngine, dvpEngine,
                corporateActionAdjuster, failManagementEngine, reconciliationService, null
        );
    }

    @Test
    public void testFullEndToEndEquityTradeLifecycle() {
        // 1. Process Aeron Trade Event
        AeronTradeCodec.TradeEvent event = new AeronTradeCodec.TradeEvent(
                "TRD-E2E-01", "B-100", "S-100", "ACC-BUYER", "ACC-SELLER", "META", 100, 500.0, System.currentTimeMillis(), "USD"
        );

        EquityTradeCaptureService.CapturedEquityTrade captured = service.processAeronTradeEvent(event);
        assertNotNull(captured);
        assertEquals("TRD-E2E-01", captured.tradeId());

        // Verify novation at CCP
        assertEquals(1, service.getClearingEngine().getClearedTrades().size());
        ClearedTrade cleared = service.getClearingEngine().getClearedTrades().values().iterator().next();
        assertEquals("CLR-TRD-E2E-01", cleared.clearedTradeId());

        // 2. Multilateral Netting
        List<NettedObligation> netted = service.executeMultilateralNettingBatch(List.of(cleared));
        assertEquals(2, netted.size()); // ACC-BUYER and ACC-SELLER net obligations

        // 3. Setup DvP Balances & Initiate Settlement
        service.getDvpEngine().initializeAccountCash("ACC-BUYER", 100000.0);
        service.getDvpEngine().initializeAccountCash("ACC-SELLER", 0.0);
        service.getDvpEngine().initializeAccountSecurities("ACC-SELLER", "META", 200);

        SettlementInstruction instr = service.initiateDvPSettlement(
                cleared.clearedTradeId(), "ACC-SELLER", "ACC-BUYER", "META", 100, 50000.0, "USD", DvPModel.MODEL_1
        );

        boolean success = service.processDvPSettlement(instr.getInstructionId());
        assertTrue(success);
        assertEquals(SettlementInstruction.Status.SETTLED, instr.getStatus());

        // Check final balances
        assertEquals(50000.0, service.getDvpEngine().getCashBalance("ACC-BUYER"), 0.01);
        assertEquals(50000.0, service.getDvpEngine().getCashBalance("ACC-SELLER"), 0.01);
        assertEquals(100, service.getDvpEngine().getSecuritiesBalance("ACC-BUYER", "META"));
        assertEquals(100, service.getDvpEngine().getSecuritiesBalance("ACC-SELLER", "META"));
    }
}
