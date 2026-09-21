package com.trading.settlement.equities;

import com.trading.settlement.dvp.SettlementInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Adjusts unsettled pending equity transactions for Corporate Actions
 * occurring between Trade Date (T) and Settlement Date (T+1/T+2).
 */
@Service
public class CorporateActionAdjuster {
    private static final Logger log = LoggerFactory.getLogger(CorporateActionAdjuster.class);

    public record AdjustedInstruction(
            String instructionId,
            String symbol,
            long originalQuantity,
            long adjustedQuantity,
            double originalCashAmount,
            double adjustedCashAmount,
            String adjustmentReason
    ) {}

    public List<AdjustedInstruction> applyCorporateAction(CorporateAction action, List<SettlementInstruction> pendingInstructions) {
        List<AdjustedInstruction> adjustedList = new ArrayList<>();

        for (SettlementInstruction instr : pendingInstructions) {
            if (!instr.getSymbol().equalsIgnoreCase(action.symbol())) continue;
            if (instr.getStatus() == SettlementInstruction.Status.SETTLED) continue;

            if (action.type() == CorporateAction.Type.STOCK_SPLIT) {
                // Stock split adjustments: e.g. 2-for-1 -> quantity * 2, total cash unchanged
                double factor = action.splitRatioNumerator() / action.splitRatioDenominator();
                long newQty = Math.round(instr.getQuantity() * factor);

                AdjustedInstruction adj = new AdjustedInstruction(
                        instr.getInstructionId(),
                        instr.getSymbol(),
                        instr.getQuantity(),
                        newQty,
                        instr.getCashAmount(),
                        instr.getCashAmount(),
                        "Stock Split " + action.splitRatioNumerator() + ":" + action.splitRatioDenominator()
                );
                adjustedList.add(adj);
                log.info("Applied Stock Split adjustment to instruction {}: Shares {} -> {}",
                        instr.getInstructionId(), instr.getQuantity(), newQty);
            } else if (action.type() == CorporateAction.Type.CASH_DIVIDEND) {
                // Cash dividend claim adjustment if settlement occurs after ex-date but trade was cum-dividend
                double dividendClaim = instr.getQuantity() * action.dividendPerShare();
                AdjustedInstruction adj = new AdjustedInstruction(
                        instr.getInstructionId(),
                        instr.getSymbol(),
                        instr.getQuantity(),
                        instr.getQuantity(),
                        instr.getCashAmount(),
                        instr.getCashAmount() + dividendClaim,
                        "Cash Dividend Claim of " + dividendClaim + " " + action.currency()
                );
                adjustedList.add(adj);
                log.info("Applied Cash Dividend adjustment to instruction {}: Cash Claim added {}",
                        instr.getInstructionId(), dividendClaim);
            }
        }

        return adjustedList;
    }
}
