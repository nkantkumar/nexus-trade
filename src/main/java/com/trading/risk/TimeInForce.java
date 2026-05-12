package com.trading.risk;

/**
 * Time In Force
 */
public enum TimeInForce {
    DAY("0"),      // Good for trading day
    GTC("1"),      // Good till cancelled
    IOC("3"),      // Immediate or cancel
    FOK("4"),      // Fill or kill
    GTD("6"),      // Good till date
    AT_THE_OPEN("7"),
    AT_THE_CLOSE("8");

    private final String code;

    TimeInForce(String code) {
        this.code = code;
    }

    public String getCode() { return code; }

    public boolean isImmediateExecution() {
        return this == IOC || this == FOK;
    }
}
