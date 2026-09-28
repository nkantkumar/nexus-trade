package com.trading.risk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FxPositionLimitValidator implements RiskValidator {
    private final Map<String, Double> maxCurrencyLimits = new ConcurrentHashMap<>();

    public FxPositionLimitValidator() {
        maxCurrencyLimits.put("USD", 10_000_000.0);
        maxCurrencyLimits.put("EUR", 10_000_000.0);
        maxCurrencyLimits.put("JPY", 1_000_000_000.0);
        maxCurrencyLimits.put("GBP", 10_000_000.0);
    }

    public void setCurrencyLimit(String currency, double maxExposure) {
        maxCurrencyLimits.put(currency.toUpperCase(), maxExposure);
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        String symbol = order.getSymbol().replace("/", "").toUpperCase();

        // Check if symbol is an FX currency pair
        if (symbol.length() == 6 && (symbol.startsWith("EUR") || symbol.startsWith("USD") || symbol.startsWith("GBP") || symbol.startsWith("JPY") || symbol.startsWith("AUD") || symbol.startsWith("CHF") || symbol.startsWith("CAD"))) {
            String baseCurrency = symbol.substring(0, 3);
            double orderNotional = order.getQuantity() * order.getPrice();

            double currentExposure = account.getPositions().values().stream()
                    .filter(p -> p.getSymbol().toUpperCase().contains(baseCurrency))
                    .mapToDouble(p -> p.getQuantity() * p.getCurrentPrice())
                    .sum();

            double newExposure = Math.abs(currentExposure + orderNotional);
            double limit = maxCurrencyLimits.getOrDefault(baseCurrency, 50_000_000.0);

            if (newExposure > limit) {
                return RiskCheckResult.fail(RiskCheckType.POSITION_LIMIT,
                        String.format("FX Position Exposure for %s (%.2f) exceeds currency limit (%.2f)",
                                baseCurrency, newExposure, limit));
            }
        }

        return RiskCheckResult.pass(RiskCheckType.POSITION_LIMIT);
    }

    @Override
    public int getPriority() {
        return 4;
    }
}
