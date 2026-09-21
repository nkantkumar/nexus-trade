package com.trading.settlement.dvp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delivery-versus-Payment (DvP) Settlement Engine.
 * Manages atomic cash reservation, securities locking, state machine transitions,
 * and ISO 20022 message dispatches.
 */
@Service
public class DvpSettlementEngine {
    private static final Logger log = LoggerFactory.getLogger(DvpSettlementEngine.class);

    private final Map<String, Double> cashLedger = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> securitiesLedger = new ConcurrentHashMap<>(); // Account -> Symbol -> Qty
    private final Map<String, SettlementInstruction> instructions = new ConcurrentHashMap<>();

    public void initializeAccountCash(String accountId, double cash) {
        cashLedger.put(accountId, cash);
    }

    public void initializeAccountSecurities(String accountId, String symbol, long quantity) {
        securitiesLedger.computeIfAbsent(accountId, k -> new ConcurrentHashMap<>()).put(symbol, quantity);
    }

    public double getCashBalance(String accountId) {
        return cashLedger.getOrDefault(accountId, 0.0);
    }

    public long getSecuritiesBalance(String accountId, String symbol) {
        return securitiesLedger.getOrDefault(accountId, Map.of()).getOrDefault(symbol, 0L);
    }

    public SettlementInstruction createInstruction(
            String obligationOrTradeId,
            String delivererAccountId,
            String receiverAccountId,
            String symbol,
            long quantity,
            double cashAmount,
            String currency,
            DvPModel model
    ) {
        String instrId = "SETTLE-" + obligationOrTradeId;
        SettlementInstruction instruction = new SettlementInstruction(
                instrId, obligationOrTradeId, delivererAccountId, receiverAccountId,
                symbol, quantity, cashAmount, currency, java.time.LocalDate.now(), model
        );
        instructions.put(instrId, instruction);
        log.info("Created DvP Settlement Instruction {}: {} -> {} ({} shares of {}, {} {})",
                instrId, delivererAccountId, receiverAccountId, quantity, symbol, cashAmount, currency);
        return instruction;
    }

    public boolean processDvPSettlement(String instructionId) {
        SettlementInstruction instr = instructions.get(instructionId);
        if (instr == null) {
            throw new IllegalArgumentException("Settlement instruction not found: " + instructionId);
        }

        // 1. Match instruction
        instr.setStatus(SettlementInstruction.Status.MATCHED);

        // 2. Reserve cash from receiver account
        double receiverCash = getCashBalance(instr.getReceiverAccountId());
        if (receiverCash < instr.getCashAmount()) {
            log.warn("DvP Settlement Failed: Account {} cash shortage (has {}, needs {})",
                    instr.getReceiverAccountId(), receiverCash, instr.getCashAmount());
            instr.setStatus(SettlementInstruction.Status.FAILED_CASH_SHORTAGE);
            return false;
        }
        cashLedger.put(instr.getReceiverAccountId(), receiverCash - instr.getCashAmount());
        instr.setStatus(SettlementInstruction.Status.CASH_RESERVED);

        // Generate pacs.008 cash message
        Iso20022MessageFactory.IsoMessage pacs008 = Iso20022MessageFactory.createPacs008CashTransfer(
                instructionId, instr.getReceiverAccountId(), instr.getDelivererAccountId(), instr.getCashAmount(), instr.getCurrency()
        );
        instr.setPacs008MessageId(pacs008.messageId());

        // 3. Lock securities from deliverer account
        long delivererSecurities = getSecuritiesBalance(instr.getDelivererAccountId(), instr.getSymbol());
        if (delivererSecurities < instr.getQuantity()) {
            log.warn("DvP Settlement Failed: Account {} securities shortage (has {} {}, needs {})",
                    instr.getDelivererAccountId(), delivererSecurities, instr.getSymbol(), instr.getQuantity());

            // Rollback cash
            cashLedger.put(instr.getReceiverAccountId(), getCashBalance(instr.getReceiverAccountId()) + instr.getCashAmount());
            instr.setStatus(SettlementInstruction.Status.FAILED_SECURITIES_SHORTAGE);
            return false;
        }

        Map<String, Long> delivererPortfolio = securitiesLedger.get(instr.getDelivererAccountId());
        delivererPortfolio.put(instr.getSymbol(), delivererSecurities - instr.getQuantity());
        instr.setStatus(SettlementInstruction.Status.SECURITIES_RESERVED);

        // Generate sese.023 securities message
        Iso20022MessageFactory.IsoMessage sese023 = Iso20022MessageFactory.createSese023SecuritiesSettlement(
                instructionId, instr.getDelivererAccountId(), instr.getReceiverAccountId(), instr.getSymbol(), instr.getQuantity()
        );
        instr.setSese023MessageId(sese023.messageId());

        // 4. Final atomic transfer
        double delivererCash = getCashBalance(instr.getDelivererAccountId());
        cashLedger.put(instr.getDelivererAccountId(), delivererCash + instr.getCashAmount());

        long receiverSecurities = getSecuritiesBalance(instr.getReceiverAccountId(), instr.getSymbol());
        securitiesLedger.computeIfAbsent(instr.getReceiverAccountId(), k -> new ConcurrentHashMap<>())
                .put(instr.getSymbol(), receiverSecurities + instr.getQuantity());

        instr.setStatus(SettlementInstruction.Status.SETTLED);
        instr.setSettledAt(Instant.now());

        log.info("DvP Settlement SUCCESSFUL for instruction {}. Cash & Securities transferred atomically.", instructionId);
        return true;
    }

    public SettlementInstruction getInstruction(String instructionId) {
        return instructions.get(instructionId);
    }
}
