package com.trading.fx;

import java.time.LocalDate;
import java.time.Instant;

public record FxTrade(
        String tradeId,
        String accountId,
        String symbol,
        FxOrderType fxOrderType,
        String side, // "BUY" or "SELL"
        double lots,
        long units,
        double spotRate,
        double forwardPoints,
        double allInRate,
        LocalDate valueDate,
        String baseCurrency,
        String quoteCurrency,
        String counterpartyId,
        Instant tradeTime
) {}
