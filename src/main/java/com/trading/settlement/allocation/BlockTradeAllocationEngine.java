package com.trading.settlement.allocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Institutional Equities Block Trade Allocation Engine.
 * Splits high-volume block executions across target client/fund accounts based on configured percentages
 * while guaranteeing non-fractional share totals and balanced cash amounts.
 */
@Service
public class BlockTradeAllocationEngine {
    private static final Logger log = LoggerFactory.getLogger(BlockTradeAllocationEngine.class);

    public AllocationInstruction allocateBlockTrade(
            String blockTradeId,
            String symbol,
            String side,
            long totalQuantity,
            double price,
            String currency,
            LocalDate tradeDate,
            LocalDate settlementDate,
            Map<String, Double> accountSharesPercent,
            Map<String, String> accountCustodianMap
    ) {
        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("Block trade quantity must be positive.");
        }

        double totalPct = accountSharesPercent.values().stream().mapToDouble(Double::doubleValue).sum();
        if (Math.abs(totalPct - 100.0) > 0.001) {
            throw new IllegalArgumentException("Allocation percentages must sum to 100%. Total was: " + totalPct);
        }

        List<AllocationInstruction.AccountAllocation> allocations = new ArrayList<>();
        long allocatedSharesSum = 0;

        List<Map.Entry<String, Double>> entries = new ArrayList<>(accountSharesPercent.entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Double> entry = entries.get(i);
            String accountId = entry.getKey();
            double pct = entry.getValue();
            String custodian = accountCustodianMap.getOrDefault(accountId, "DEFAULT_CSD");

            long allocatedQuantity;
            if (i == entries.size() - 1) {
                // Assign remaining shares to last account to prevent rounding drift
                allocatedQuantity = totalQuantity - allocatedSharesSum;
            } else {
                allocatedQuantity = Math.round(totalQuantity * (pct / 100.0));
                allocatedSharesSum += allocatedQuantity;
            }

            double allocatedAmount = Math.round(allocatedQuantity * price * 100.0) / 100.0;

            allocations.add(new AllocationInstruction.AccountAllocation(
                    accountId, custodian, pct, allocatedQuantity, allocatedAmount
            ));
        }

        log.info("Successfully allocated block trade {} ({} shares of {}) across {} accounts.",
                blockTradeId, totalQuantity, symbol, allocations.size());

        return new AllocationInstruction(
                blockTradeId, symbol, side, totalQuantity, price, currency, tradeDate, settlementDate, allocations
        );
    }
}
