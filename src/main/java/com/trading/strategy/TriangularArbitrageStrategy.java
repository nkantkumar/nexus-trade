package com.trading.strategy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FX Triangular Arbitrage Strategy.
 * Monitors cross-rate discrepancies between 3 currency pairs (e.g. EURUSD, USDJPY, EURJPY).
 * Emits signals when synthetic cross rate differs from direct market cross rate beyond pip threshold.
 */
public class TriangularArbitrageStrategy {
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final double pipThreshold = 2.0; // 2 pips threshold

    public void updateMarketPrice(String symbol, double price) {
        lastPrices.put(symbol.toUpperCase(), price);
    }

    public AlgoSignal evaluateArbitrage(String pairA, String pairB, String pairC) {
        Double pA = lastPrices.get(pairA.toUpperCase()); // e.g. EURUSD = 1.0850
        Double pB = lastPrices.get(pairB.toUpperCase()); // e.g. USDJPY = 148.20
        Double pC = lastPrices.get(pairC.toUpperCase()); // e.g. EURJPY = 160.80

        if (pA == null || pB == null || pC == null) {
            return null;
        }

        double syntheticCross = pA * pB; // 1.0850 * 148.20 = 160.797
        double directCross = pC;
        double diffPips = Math.abs(syntheticCross - directCross) / 0.01;

        if (diffPips > pipThreshold) {
            String side = syntheticCross < directCross ? "BUY" : "SELL";
            return new AlgoSignal(
                    "ARB-" + UUID.randomUUID().toString().substring(0, 8),
                    pairC,
                    side,
                    100_000,
                    Instant.now()
            );
        }

        return null;
    }
}
