package com.trading.settlement.equities;

import com.trading.settlement.dvp.SettlementInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Equities Fail Management & Buy-In Engine.
 * Monitors failed settlements due to securities shortages, issues buy-in notices,
 * and initiates Securities Borrowing & Lending (SBL) requests.
 */
@Service
public class EquityFailManagementEngine {
    private static final Logger log = LoggerFactory.getLogger(EquityFailManagementEngine.class);

    public record BuyInNotice(
            String buyInId,
            String instructionId,
            String failingAccountId,
            String symbol,
            long shortQuantity,
            double buyInPrice,
            double totalPenaltyCost,
            Instant issuedAt
    ) {}

    public record SecuritiesBorrowRequest(
            String sblRequestId,
            String instructionId,
            String borrowerAccountId,
            String symbol,
            long requiredQuantity,
            double borrowFeeRate,
            String status
    ) {}

    public BuyInNotice triggerBuyIn(SettlementInstruction instruction, double currentMarketPrice, double penaltyPercent) {
        if (instruction.getStatus() != SettlementInstruction.Status.FAILED_SECURITIES_SHORTAGE &&
            instruction.getStatus() != SettlementInstruction.Status.FAILED_CASH_SHORTAGE) {
            throw new IllegalStateException("Instruction is not in FAILED state.");
        }

        String buyInId = "BUYIN-" + UUID.randomUUID().toString().substring(0, 8);
        double penalty = Math.round((currentMarketPrice * instruction.getQuantity() * (penaltyPercent / 100.0)) * 100.0) / 100.0;
        double totalCost = Math.round((currentMarketPrice * instruction.getQuantity() + penalty) * 100.0) / 100.0;

        BuyInNotice notice = new BuyInNotice(
                buyInId,
                instruction.getInstructionId(),
                instruction.getDelivererAccountId(),
                instruction.getSymbol(),
                instruction.getQuantity(),
                currentMarketPrice,
                totalCost,
                Instant.now()
        );

        log.warn("ISSUED BUY-IN NOTICE {}: Failing Account {} must cover {} shares of {} at total cost {} {}",
                buyInId, instruction.getDelivererAccountId(), instruction.getQuantity(), instruction.getSymbol(), totalCost, instruction.getCurrency());

        return notice;
    }

    public SecuritiesBorrowRequest initiateAutoBorrow(SettlementInstruction instruction, double feeRatePct) {
        String sblId = "SBL-" + UUID.randomUUID().toString().substring(0, 8);
        SecuritiesBorrowRequest req = new SecuritiesBorrowRequest(
                sblId,
                instruction.getInstructionId(),
                instruction.getDelivererAccountId(),
                instruction.getSymbol(),
                instruction.getQuantity(),
                feeRatePct,
                "REQUESTED"
        );
        log.info("Initiated SBL Auto-Borrow request {} for account {} ({} shares of {})",
                sblId, instruction.getDelivererAccountId(), instruction.getQuantity(), instruction.getSymbol());
        return req;
    }
}
