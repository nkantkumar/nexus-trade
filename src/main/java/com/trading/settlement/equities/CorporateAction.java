package com.trading.settlement.equities;

import java.time.LocalDate;

/**
 * Equities Corporate Action event definition (Stock Split, Cash Dividend).
 */
public record CorporateAction(
        String actionId,
        String symbol,
        Type type,
        LocalDate exDate,
        LocalDate recordDate,
        LocalDate paymentDate,
        double splitRatioNumerator,   // e.g. 2 for 2-for-1 split
        double splitRatioDenominator, // e.g. 1 for 2-for-1 split
        double dividendPerShare,
        String currency
) {
    public enum Type {
        STOCK_SPLIT,
        CASH_DIVIDEND,
        RIGHTS_ISSUE
    }
}
