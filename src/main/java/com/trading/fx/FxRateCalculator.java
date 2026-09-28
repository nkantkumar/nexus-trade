package com.trading.fx;

import java.util.Map;

public class FxRateCalculator {

    /**
     * Covered Interest Rate Parity FX Forward Rate calculation:
     * F = S * (1 + r_quote * days / 360) / (1 + r_base * days / 360)
     */
    public double calculateForwardRate(double spotRate, double baseInterestRate, double quoteInterestRate, int daysToValueDate) {
        double yearFraction = daysToValueDate / 360.0;
        double numerator = 1.0 + (quoteInterestRate * yearFraction);
        double denominator = 1.0 + (baseInterestRate * yearFraction);
        return spotRate * (numerator / denominator);
    }

    /**
     * Forward points in pips = (Forward Rate - Spot Rate) / Pip Size
     */
    public double calculateForwardPointsPips(double spotRate, double forwardRate, double pipSize) {
        return (forwardRate - spotRate) / pipSize;
    }

    /**
     * Cross rate calculation between pair 1 and pair 2.
     * E.g. EUR/USD and USD/JPY -> EUR/JPY = EUR/USD * USD/JPY
     */
    public double calculateCrossRate(double eurusd, double usdjpy) {
        return eurusd * usdjpy;
    }

    /**
     * Calculate pip difference between two rates.
     */
    public double calculatePipDifference(double rate1, double rate2, double pipSize) {
        return Math.abs(rate1 - rate2) / pipSize;
    }

    /**
     * Convert cash amount in source currency into target currency using active market spot rates.
     */
    public double convertAmount(double amount, String sourceCurrency, String targetCurrency, Map<String, Double> rates) {
        if (sourceCurrency.equalsIgnoreCase(targetCurrency)) {
            return amount;
        }

        String directPair = sourceCurrency.toUpperCase() + targetCurrency.toUpperCase();
        if (rates.containsKey(directPair)) {
            return amount * rates.get(directPair);
        }

        String inversePair = targetCurrency.toUpperCase() + sourceCurrency.toUpperCase();
        if (rates.containsKey(inversePair)) {
            return amount / rates.get(inversePair);
        }

        // Try via USD intermediary
        String sourceUsd = sourceCurrency.toUpperCase() + "USD";
        String usdSource = "USD" + sourceCurrency.toUpperCase();
        String targetUsd = targetCurrency.toUpperCase() + "USD";
        String usdTarget = "USD" + targetCurrency.toUpperCase();

        double usdAmount = amount;
        if (rates.containsKey(sourceUsd)) {
            usdAmount = amount * rates.get(sourceUsd);
        } else if (rates.containsKey(usdSource)) {
            usdAmount = amount / rates.get(usdSource);
        }

        if (rates.containsKey(targetUsd)) {
            return usdAmount / rates.get(targetUsd);
        } else if (rates.containsKey(usdTarget)) {
            return usdAmount * rates.get(usdTarget);
        }

        return amount; // fallback if direct or cross rate unavailable
    }
}
