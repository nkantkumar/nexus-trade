package com.trading.risk;

import java.math.BigDecimal;

public record TradeIntent(String accountId, String symbol, long quantity, BigDecimal price, BigDecimal vwap, BigDecimal margin) {}
