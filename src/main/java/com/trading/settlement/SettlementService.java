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
import com.trading.settlement.equities.CorporateAction;
import com.trading.settlement.equities.CorporateActionAdjuster;
import com.trading.settlement.equities.EquityFailManagementEngine;
import com.trading.settlement.reconciliation.CustodianRecord;
import com.trading.settlement.reconciliation.CustodianReconciliationService;
import com.trading.settlement.reconciliation.ReconciliationReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Enterprise Securities Post-Trade, Clearing & Settlement Orchestrator.
 * Connects Aeron Low-Latency Trade Streams, Block Trade Allocations, CCP Multilateral Netting,
 * DvP Settlement State Machine, Corporate Action Adjustments, and Custodian Reconciliations.
 */
@Service
public class SettlementService {
    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

    private final EquityTradeCaptureService tradeCaptureService;
    private final BlockTradeAllocationEngine allocationEngine;
    private final CcpClearingEngine clearingEngine;
    private final DvpSettlementEngine dvpEngine;
    private final CorporateActionAdjuster corporateActionAdjuster;
    private final EquityFailManagementEngine failManagementEngine;
    private final CustodianReconciliationService reconciliationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public SettlementService(
            EquityTradeCaptureService tradeCaptureService,
            BlockTradeAllocationEngine allocationEngine,
            CcpClearingEngine clearingEngine,
            DvpSettlementEngine dvpEngine,
            CorporateActionAdjuster corporateActionAdjuster,
            EquityFailManagementEngine failManagementEngine,
            CustodianReconciliationService reconciliationService,
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.tradeCaptureService = tradeCaptureService;
        this.allocationEngine = allocationEngine;
        this.clearingEngine = clearingEngine;
        this.dvpEngine = dvpEngine;
        this.corporateActionAdjuster = corporateActionAdjuster;
        this.failManagementEngine = failManagementEngine;
        this.reconciliationService = reconciliationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public EquityTradeCaptureService.CapturedEquityTrade processAeronTradeEvent(AeronTradeCodec.TradeEvent event) {
        EquityTradeCaptureService.CapturedEquityTrade trade = tradeCaptureService.captureAeronTrade(event, 1); // T+1
        clearingEngine.novateTrade(trade);
        if (kafkaTemplate != null) {
            kafkaTemplate.send("trade-cleared-events", trade.tradeId(), trade);
        }
        return trade;
    }

    public AllocationInstruction allocateBlockTrade(
            String blockTradeId, String symbol, String side, long totalQuantity, double price,
            String currency, Map<String, Double> accountSharesPercent, Map<String, String> accountCustodianMap
    ) {
        LocalDate tradeDate = LocalDate.now();
        LocalDate settlementDate = tradeCaptureService.calculateSettlementDate(tradeDate, 1);
        return allocationEngine.allocateBlockTrade(
                blockTradeId, symbol, side, totalQuantity, price, currency, tradeDate, settlementDate, accountSharesPercent, accountCustodianMap
        );
    }

    public List<NettedObligation> executeMultilateralNettingBatch(List<ClearedTrade> trades) {
        return clearingEngine.calculateMultilateralNetting(trades);
    }

    public SettlementInstruction initiateDvPSettlement(
            String obligationOrTradeId, String delivererAccountId, String receiverAccountId,
            String symbol, long quantity, double cashAmount, String currency, DvPModel model
    ) {
        return dvpEngine.createInstruction(obligationOrTradeId, delivererAccountId, receiverAccountId, symbol, quantity, cashAmount, currency, model);
    }

    public boolean processDvPSettlement(String instructionId) {
        return dvpEngine.processDvPSettlement(instructionId);
    }

    public List<CorporateActionAdjuster.AdjustedInstruction> processCorporateAction(CorporateAction action, List<SettlementInstruction> pending) {
        return corporateActionAdjuster.applyCorporateAction(action, pending);
    }

    public EquityFailManagementEngine.BuyInNotice triggerBuyInForFailedSettlement(SettlementInstruction instruction, double currentPrice, double penaltyPercent) {
        return failManagementEngine.triggerBuyIn(instruction, currentPrice, penaltyPercent);
    }

    public ReconciliationReport reconcileCustodianRecords(List<SettlementInstruction> internalInstructions, List<CustodianRecord> custodianRecords) {
        return reconciliationService.runReconciliation(internalInstructions, custodianRecords);
    }

    public DvpSettlementEngine getDvpEngine() {
        return dvpEngine;
    }

    public CcpClearingEngine getClearingEngine() {
        return clearingEngine;
    }

    public EquityTradeCaptureService getTradeCaptureService() {
        return tradeCaptureService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void scheduledCustodianReconciliationJob() {
        log.info("Running scheduled custodian reconciliation job...");
    }
}
