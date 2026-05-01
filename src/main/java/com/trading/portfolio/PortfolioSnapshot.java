package com.trading.portfolio;

public record PortfolioSnapshot(
        String symbol,
        long quantity,
        double realizedPnl,
        double unrealizedPnl,
        double sharpe,
        double maxDrawdown,
        double var95) {}
