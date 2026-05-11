package com.trading.marketdata;

public interface ExchangeAdapter {
    void connect();
    void disconnect();
    String sendOrder(InternalOrder order);
    void cancelOrder(String orderId);
    void replaceOrder(String orderId, long newQuantity, double newPrice);
    void subscribeMarketData(String symbol);
    boolean isConnected();
}
