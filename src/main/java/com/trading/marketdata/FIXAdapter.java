package com.trading.marketdata;

import quickfix.ConfigError;
import quickfix.SessionSettings;

/**
 * FIX 4.4 gateway: wires {@link FIXApplication} with optional {@link VenueEventListener} callbacks.
 */
public final class FIXAdapter implements ExchangeAdapter {

    private final FIXApplication fixApp;
    private volatile boolean started;

    public FIXAdapter(String configPath, VenueEventListener listener) throws ConfigError {
        SessionSettings settings = new SessionSettings(configPath);
        this.fixApp = new FIXApplication(listener, settings);
    }

    public FIXAdapter(String configPath) throws ConfigError {
        this(configPath, new NoOpVenueEventListener());
    }

    @Override
    public void connect() {
        try {
            if (!started) {
                fixApp.start();
                started = true;
            }
        } catch (ConfigError e) {
            throw new IllegalStateException("FIX start failed", e);
        }
    }

    @Override
    public void disconnect() {
        fixApp.stop();
        started = false;
    }

    @Override
    public String sendOrder(InternalOrder order) {
        fixApp.sendNewOrder(order);
        return order.getOrderId();
    }

    @Override
    public void cancelOrder(String orderId) {
        fixApp.sendCancelRequest(orderId, 0);
    }

    @Override
    public void replaceOrder(String orderId, long newQuantity, double newPrice) {
        fixApp.sendReplaceRequest(orderId, newQuantity, newPrice);
    }

    @Override
    public void subscribeMarketData(String symbol) {
        fixApp.sendMarketDataRequest(symbol);
    }

    @Override
    public boolean isConnected() {
        return started;
    }
}
