package com.trading.settlement.clearing;

import java.time.LocalDate;

/**
 * Multilateral netted settlement obligation per clearing member, symbol, currency, and settlement date.
 * Positive netQuantity = Net Receive Shares / Negative = Net Deliver Shares.
 * Positive netCash = Net Pay Cash / Negative = Net Collect Cash.
 */
public record NettedObligation(
        String obligationId,
        String clearingMemberId,
        String symbol,
        String currency,
        LocalDate settlementDate,
        long grossBuyQuantity,
        long grossSellQuantity,
        long netQuantity,           // netQuantity > 0 means RECEIVE shares, < 0 means DELIVER shares
        double grossBuyAmount,
        double grossSellAmount,
        double netCashAmount,        // netCashAmount > 0 means PAY cash, < 0 means RECEIVE cash
        int tradeCount
) {}
