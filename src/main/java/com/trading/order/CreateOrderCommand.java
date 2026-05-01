package com.trading.order;

import java.math.BigDecimal;

public record CreateOrderCommand(String symbol, long quantity, BigDecimal limitPrice) {}
