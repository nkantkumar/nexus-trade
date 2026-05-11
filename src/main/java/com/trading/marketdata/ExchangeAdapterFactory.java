package com.trading.marketdata;

import quickfix.ConfigError;

public final class ExchangeAdapterFactory {

    private ExchangeAdapterFactory() {}

    public static ExchangeAdapter createAdapter(ExchangeConfig config, VenueEventListener listener)
            throws ConfigError {
        return switch (config.protocol()) {
            case FIX -> new FIXAdapter(config.fixConfigPath(), listener);
            case OUCH -> new OUCHAdapter(config.host(), config.port(), config.username(), config.password(), listener);
        };
    }

    public static ExchangeAdapter createAdapter(ExchangeConfig config) throws ConfigError {
        return createAdapter(config, new NoOpVenueEventListener());
    }
}
