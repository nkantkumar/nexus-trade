package com.trading.marketdata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OUCHAdapter implements ExchangeAdapter {

    private final OUCHClient ouchClient;
    private final Map<String, Long> orderIdToRef = new ConcurrentHashMap<>();
    private volatile boolean connected;

    public OUCHAdapter(String host, int port, String username, String password, VenueEventListener listener) {
        this.ouchClient = new OUCHClient(listener, host, port, username, password);
    }

    public OUCHAdapter(String host, int port, String username, String password) {
        this(host, port, username, password, new NoOpVenueEventListener());
    }

    @Override
    public void connect() {
        try {
            ouchClient.connect();
            connected = true;
        } catch (Exception e) {
            connected = false;
            throw new IllegalStateException("OUCH connect failed", e);
        }
    }

    @Override
    public void disconnect() {
        ouchClient.disconnect();
        connected = false;
    }

    @Override
    public String sendOrder(InternalOrder order) {
        if (!connected) {
            return null;
        }
        char side = order.getSide() == VenueSide.BUY ? 'B' : 'S';
        long ref =
                ouchClient.sendEnterOrder(
                        order.getOrderId(), order.getSymbol(), side, order.getQuantity(), order.getPrice());
        if (ref < 0) {
            return null;
        }
        orderIdToRef.put(order.getOrderId(), ref);
        return order.getOrderId();
    }

    @Override
    public void cancelOrder(String orderId) {
        Long ref = orderIdToRef.get(orderId);
        if (ref != null && connected) {
            ouchClient.sendCancelOrder(ref, 0);
        }
    }

    @Override
    public void replaceOrder(String orderId, long newQuantity, double newPrice) {
        Long ref = orderIdToRef.get(orderId);
        if (ref != null && connected) {
            ouchClient.sendReplaceOrder(ref, newQuantity, newPrice);
        }
    }

    @Override
    public void subscribeMarketData(String symbol) {
        if (connected) {
            ouchClient.sendMarketDataSubscription(symbol);
        }
    }

    @Override
    public boolean isConnected() {
        return connected && ouchClient.isConnected();
    }
}
