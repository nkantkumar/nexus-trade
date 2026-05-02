package com.trading.strategy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import com.trading.infra.chronicle.ChronicleAppenders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StrategyEngineService {
    private static final String STRATEGY_CHRONICLE_PATH = "build/chronicle/strategy";

    private final MeanReversionStrategy strategy;
    private final KafkaTemplate<String, AlgoSignal> kafkaTemplate;
    private final Counter signalCounter;

    public StrategyEngineService(KafkaTemplate<String, AlgoSignal> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.strategy = new MeanReversionStrategy();
        this.signalCounter = meterRegistry.counter("algo.signal.count");
    }

    public void onMarketData(MarketDataEvent event) {
        strategy.onMarketData(event);
        AlgoSignal signal = strategy.currentSignal();
        if (signal != null) {
            kafkaTemplate.send("algo-signals", signal.symbol(), signal);
            ChronicleAppenders.forPath(STRATEGY_CHRONICLE_PATH).writeText(signal.toString());
            signalCounter.increment();
        }
    }

    public void onExecution(ExecutionEvent event) {
        strategy.onExecution(event);
    }
}

abstract class Strategy {
    abstract void onMarketData(MarketDataEvent marketDataEvent);
    abstract void onExecution(ExecutionEvent executionEvent);
    void start() {}
    void stop() {}
    void pause() {}
}

class MeanReversionStrategy extends Strategy {
    private final Deque<Double> prices = new ArrayDeque<>();
    private AlgoSignal lastSignal;

    @Override
    void onMarketData(MarketDataEvent event) {
        prices.addLast(event.price());
        if (prices.size() > 20) {
            prices.removeFirst();
        }
        if (prices.size() < 20) {
            return;
        }
        double mean = prices.stream().mapToDouble(Double::doubleValue).average().orElse(event.price());
        double std = Math.sqrt(prices.stream().mapToDouble(p -> Math.pow(p - mean, 2)).average().orElse(0.0));
        double upper = mean + 2 * std;
        double lower = mean - 2 * std;
        if (event.price() > upper) {
            lastSignal = new AlgoSignal(UUID.randomUUID().toString(), event.symbol(), "SELL", kellySize(0.55, 1.2), Instant.now());
        } else if (event.price() < lower) {
            lastSignal = new AlgoSignal(UUID.randomUUID().toString(), event.symbol(), "BUY", kellySize(0.55, 1.2), Instant.now());
        } else {
            lastSignal = null;
        }
    }

    @Override
    void onExecution(ExecutionEvent executionEvent) {}

    AlgoSignal currentSignal() {
        return lastSignal;
    }

    private long kellySize(double winRate, double payoff) {
        double kelly = winRate - ((1 - winRate) / payoff);
        return Math.max(1, (long) (kelly * 1_000));
    }
}

record MarketDataEvent(String symbol, double price, long size) {}
record ExecutionEvent(String orderId, long filledQty, double avgPrice) {}
record AlgoSignal(String id, String symbol, String side, long qty, Instant ts) {}
