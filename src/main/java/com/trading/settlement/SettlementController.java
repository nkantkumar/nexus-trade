package com.trading.settlement;

import com.trading.settlement.aeron.AeronTradeCodec;
import com.trading.settlement.allocation.AllocationInstruction;
import com.trading.settlement.allocation.EquityTradeCaptureService.CapturedEquityTrade;
import com.trading.settlement.clearing.ClearedTrade;
import com.trading.settlement.clearing.NettedObligation;
import com.trading.settlement.dvp.DvPModel;
import com.trading.settlement.dvp.SettlementInstruction;
import com.trading.settlement.equities.CorporateAction;
import com.trading.settlement.equities.CorporateActionAdjuster;
import com.trading.settlement.equities.EquityFailManagementEngine;
import com.trading.settlement.reconciliation.CustodianRecord;
import com.trading.settlement.reconciliation.ReconciliationReport;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Securities Post-Trade, Clearing & Settlement, and Equities Operations.
 */
@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    public record IngestTradeRequest(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            String buyAccountId,
            String sellAccountId,
            String symbol,
            long quantity,
            double price,
            String currency
    ) {}

    @PostMapping("/trades/ingest")
    public ResponseEntity<CapturedEquityTrade> ingestTrade(@RequestBody IngestTradeRequest req) {
        AeronTradeCodec.TradeEvent event = new AeronTradeCodec.TradeEvent(
                req.tradeId(), req.buyOrderId(), req.sellOrderId(), req.buyAccountId(), req.sellAccountId(),
                req.symbol(), req.quantity(), req.price(), System.currentTimeMillis(), req.currency()
        );
        CapturedEquityTrade captured = settlementService.processAeronTradeEvent(event);
        return ResponseEntity.ok(captured);
    }

    public record BlockAllocationRequest(
            String blockTradeId,
            String symbol,
            String side,
            long totalQuantity,
            double price,
            String currency,
            Map<String, Double> accountSharesPercent,
            Map<String, String> accountCustodianMap
    ) {}

    @PostMapping("/allocations/block")
    public ResponseEntity<AllocationInstruction> allocateBlock(@RequestBody BlockAllocationRequest req) {
        AllocationInstruction instruction = settlementService.allocateBlockTrade(
                req.blockTradeId(), req.symbol(), req.side(), req.totalQuantity(), req.price(),
                req.currency(), req.accountSharesPercent(), req.accountCustodianMap()
        );
        return ResponseEntity.ok(instruction);
    }

    @PostMapping("/clearing/netting")
    public ResponseEntity<List<NettedObligation>> runNetting() {
        List<ClearedTrade> clearedTrades = new ArrayList<>(settlementService.getClearingEngine().getClearedTrades().values());
        List<NettedObligation> obligations = settlementService.executeMultilateralNettingBatch(clearedTrades);
        return ResponseEntity.ok(obligations);
    }

    public record CreateDvpRequest(
            String obligationOrTradeId,
            String delivererAccountId,
            String receiverAccountId,
            String symbol,
            long quantity,
            double cashAmount,
            String currency,
            DvPModel model
    ) {}

    @PostMapping("/dvp/create")
    public ResponseEntity<SettlementInstruction> createDvP(@RequestBody CreateDvpRequest req) {
        SettlementInstruction instr = settlementService.initiateDvPSettlement(
                req.obligationOrTradeId(), req.delivererAccountId(), req.receiverAccountId(),
                req.symbol(), req.quantity(), req.cashAmount(), req.currency(), req.model()
        );
        return ResponseEntity.ok(instr);
    }

    @PostMapping("/dvp/process/{instructionId}")
    public ResponseEntity<Map<String, Object>> processDvP(@PathVariable String instructionId) {
        boolean success = settlementService.processDvPSettlement(instructionId);
        SettlementInstruction instr = settlementService.getDvpEngine().getInstruction(instructionId);
        return ResponseEntity.ok(Map.of(
                "success", success,
                "status", instr != null ? instr.getStatus() : "UNKNOWN",
                "pacs008MessageId", instr != null ? (instr.getPacs008MessageId() != null ? instr.getPacs008MessageId() : "") : "",
                "sese023MessageId", instr != null ? (instr.getSese023MessageId() != null ? instr.getSese023MessageId() : "") : ""
        ));
    }

    @PostMapping("/reconciliation/run")
    public ResponseEntity<ReconciliationReport> runReconciliation(@RequestBody List<CustodianRecord> custodianRecords) {
        List<SettlementInstruction> instructions = new ArrayList<>(); // from dvp engine
        ReconciliationReport report = settlementService.reconcileCustodianRecords(instructions, custodianRecords);
        return ResponseEntity.ok(report);
    }
}
