package com.trading.settlement.clearing;

import java.time.LocalDate;

/**
 * Novated trade record by the Central Counterparty Clearinghouse (CCP).
 * Interposes CCP between buyer and seller.
 */
public record ClearedTrade(
        String clearedTradeId,
        String originalTradeId,
        String clearingMemberBuyer,
        String clearingMemberSeller,
        String ccpId,
        String symbol,
        long quantity,
        double price,
        double totalValue,
        String currency,
        LocalDate settlementDate,
        String status
) {}
