package com.trading.pricing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PricingService {
    public OptionGreeks blackScholes(OptionRequest req) {
        double s = req.spot();
        double k = req.strike();
        double t = req.daysToExpiry() / 365.0;
        double r = req.rate();
        double v = req.vol();
        double d1 = (Math.log(s / k) + (r + 0.5 * v * v) * t) / (v * Math.sqrt(t));
        double d2 = d1 - v * Math.sqrt(t);
        double price = s * normCdf(d1) - k * Math.exp(-r * t) * normCdf(d2);
        return new OptionGreeks(price, normCdf(d1), 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Garman-Kohlhagen Pricing Model for Foreign Exchange (FX) Currency Options.
     * Uses domestic interest rate (rd) and foreign interest rate (rf).
     */
    public OptionGreeks garmanKohlhagen(FxOptionRequest req) {
        double s = req.spotRate();
        double k = req.strikeRate();
        double t = req.daysToExpiry() / 365.0;
        double rd = req.domesticRate();
        double rf = req.foreignRate();
        double v = req.volatility();

        double d1 = (Math.log(s / k) + (rd - rf + 0.5 * v * v) * t) / (v * Math.sqrt(t));
        double d2 = d1 - v * Math.sqrt(t);

        double callPrice = s * Math.exp(-rf * t) * normCdf(d1) - k * Math.exp(-rd * t) * normCdf(d2);
        double putPrice = k * Math.exp(-rd * t) * normCdf(-d2) - s * Math.exp(-rf * t) * normCdf(-d1);
        double price = "CALL".equalsIgnoreCase(req.optionType()) ? callPrice : putPrice;

        double delta = "CALL".equalsIgnoreCase(req.optionType()) ? Math.exp(-rf * t) * normCdf(d1) : -Math.exp(-rf * t) * normCdf(-d1);
        return new OptionGreeks(price, delta, 0.0, 0.0, 0.0, 0.0);
    }

    /**
     * Calculates FX Forward Rate based on Covered Interest Rate Parity:
     * F = S * (1 + rd * t) / (1 + rf * t)
     */
    public double calculateFxForwardRate(double spotRate, double domesticRate, double foreignRate, int daysToExpiry) {
        double t = daysToExpiry / 360.0;
        return spotRate * ((1.0 + domesticRate * t) / (1.0 + foreignRate * t));
    }

    /**
     * Calculates FX Mark-To-Market valuation for multi-currency positions converted to base currency.
     */
    public double markToMarketFx(Map<String, Double> currencyPositions, Map<String, Double> fxSpotRates, String baseCurrency) {
        double totalBaseValue = 0.0;
        for (Map.Entry<String, Double> entry : currencyPositions.entrySet()) {
            String currency = entry.getKey();
            double balance = entry.getValue();
            if (currency.equalsIgnoreCase(baseCurrency)) {
                totalBaseValue += balance;
            } else {
                String pair = currency.toUpperCase() + baseCurrency.toUpperCase();
                double rate = fxSpotRates.getOrDefault(pair, 1.0);
                totalBaseValue += balance * rate;
            }
        }
        return totalBaseValue;
    }

    public double monteCarlo(OptionRequest req, int paths) {
        int workers = Math.max(1, Runtime.getRuntime().availableProcessors());
        int perWorker = Math.max(1, paths / workers);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Double>> futures = new ArrayList<>();
            for (int w = 0; w < workers; w++) {
                futures.add(executor.submit(() -> {
                    double sum = 0;
                    for (int i = 0; i < perWorker; i++) {
                        sum += Math.max(0, req.spot() - req.strike());
                    }
                    return sum;
                }));
            }
            double total = 0;
            for (Future<Double> future : futures) {
                total += future.get();
            }
            return total / (workers * perWorker);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        } catch (ExecutionException ex) {
            throw new IllegalStateException(ex.getCause());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void markToMarket() {
        // mark to market extension point
    }

    private double normCdf(double x) {
        return 0.5 * (1.0 + Math.tanh(Math.sqrt(2.0 / Math.PI) * (x + 0.044715 * Math.pow(x, 3))));
    }
}

record OptionRequest(double spot, double strike, double vol, double rate, int daysToExpiry, LocalDate asOfDate) {}
record FxOptionRequest(double spotRate, double strikeRate, double volatility, double domesticRate, double foreignRate, int daysToExpiry, String optionType) {}
record OptionGreeks(double price, double delta, double gamma, double theta, double vega, double rho) {}

