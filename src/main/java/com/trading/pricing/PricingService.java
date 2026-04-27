package com.trading.pricing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
record OptionGreeks(double price, double delta, double gamma, double theta, double vega, double rho) {}
