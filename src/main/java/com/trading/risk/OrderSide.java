package com.trading.risk;

/**
 * Order Side (Buy/Sell)
 */
public enum OrderSide {
    BUY("B"),
    SELL("S");

    private final String code;

    OrderSide(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public boolean isBuy() { return this == BUY; }
    public boolean isSell() { return this == SELL; }

    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}
