package com.trading.marketdata;

public record ExchangeConfig(
        Protocol protocol,
        String fixConfigPath,
        String host,
        int port,
        String username,
        String password) {

    public enum Protocol {
        FIX,
        OUCH
    }

    public static ExchangeConfig fix(String fixConfigPath) {
        return new ExchangeConfig(Protocol.FIX, fixConfigPath, "", 0, "", "");
    }

    public static ExchangeConfig ouch(String host, int port, String username, String password) {
        return new ExchangeConfig(Protocol.OUCH, "", host, port, username, password);
    }
}
