package com.trading.marketdata;

public enum VenueSide {
    BUY,
    SELL;

    public static VenueSide fromChar(char c) {
        return c == 'B' || c == '1' ? BUY : SELL;
    }
}
