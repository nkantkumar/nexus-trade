package com.trading.risk;

/**
 * Order Type
 */
public enum OrderType {
    MARKET("MKT"),
    LIMIT("LMT"),
    STOP("STP"),
    STOP_LIMIT("STP_LMT"),
    MARKET_IF_TOUCHED("MIT"),
    LIMIT_IF_TOUCHED("LIT"),
    PEGGED("PEG"),
    TRAILING_STOP("TRAIL");

    private final String code;

    OrderType(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public boolean isMarketOrder() {
        return this == MARKET || this == MARKET_IF_TOUCHED;
    }

    public boolean isLimitOrder() {
        return this == LIMIT || this == STOP_LIMIT || this == LIMIT_IF_TOUCHED;
    }

    public boolean isStopOrder() {
        return this == STOP || this == STOP_LIMIT || this == TRAILING_STOP;
    }
}
