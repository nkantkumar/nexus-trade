package com.trading.fx;

public enum FxPair {
    EURUSD("EUR", "USD", 0.0001, 4, 100_000, true),
    USDJPY("USD", "JPY", 0.01, 2, 100_000, true),
    GBPUSD("GBP", "USD", 0.0001, 4, 100_000, true),
    AUDUSD("AUD", "USD", 0.0001, 4, 100_000, true),
    USDCHF("USD", "CHF", 0.0001, 4, 100_000, true),
    USDCAD("USD", "CAD", 0.0001, 4, 100_000, true),
    EURGBP("EUR", "GBP", 0.0001, 4, 100_000, false),
    EURJPY("EUR", "JPY", 0.01, 2, 100_000, false),
    GBPJPY("GBP", "JPY", 0.01, 2, 100_000, false);

    private final String baseCurrency;
    private final String quoteCurrency;
    private final double pipSize;
    private final int pipDecimalPlaces;
    private final long standardLotSize;
    private final boolean isMajor;

    FxPair(String baseCurrency, String quoteCurrency, double pipSize, int pipDecimalPlaces, long standardLotSize, boolean isMajor) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.pipSize = pipSize;
        this.pipDecimalPlaces = pipDecimalPlaces;
        this.standardLotSize = standardLotSize;
        this.isMajor = isMajor;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public double getPipSize() {
        return pipSize;
    }

    public int getPipDecimalPlaces() {
        return pipDecimalPlaces;
    }

    public long getStandardLotSize() {
        return standardLotSize;
    }

    public boolean isMajor() {
        return isMajor;
    }

    public double calculatePipValue(double rate, double lots) {
        double units = lots * standardLotSize;
        if ("USD".equals(quoteCurrency)) {
            return units * pipSize;
        } else {
            return (units * pipSize) / rate;
        }
    }

    public double calculatePnL(double entryRate, double exitRate, double lots, boolean isBuy) {
        double units = lots * standardLotSize;
        double rateDiff = isBuy ? (exitRate - entryRate) : (entryRate - exitRate);
        return rateDiff * units;
    }
}
