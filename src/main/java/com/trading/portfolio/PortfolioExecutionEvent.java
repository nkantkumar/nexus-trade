package com.trading.portfolio;

public record PortfolioExecutionEvent(String accountId, String symbol, long qty, double price) {}
