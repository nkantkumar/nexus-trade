package com.trading.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateOrderCommand(
        String symbol,
        long quantity,
        BigDecimal limitPrice,
        String fxOrderType,
        double lots,
        LocalDate valueDate,
        String baseCurrency,
        String quoteCurrency
) {
    public CreateOrderCommand(String symbol, long quantity, BigDecimal limitPrice) {
        this(symbol, quantity, limitPrice, "SPOT", 0.0, LocalDate.now().plusDays(2), null, null);
    }
}
