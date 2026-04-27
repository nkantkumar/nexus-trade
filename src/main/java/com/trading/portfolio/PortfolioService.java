package com.trading.portfolio;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioService {
    private final Map<String, Position> positions = new HashMap<>();
    private final StringRedisTemplate redisTemplate;

    public PortfolioService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "execution-events", groupId = "portfolio")
    public void onExecution(PortfolioExecutionEvent event) {
        Position p = positions.computeIfAbsent(event.symbol(), ignored -> new Position());
        p.apply(event);
        redisTemplate.opsForValue().set("portfolio:" + event.accountId() + ":" + event.symbol(), String.valueOf(p.netQty));
    }

    @GetMapping("/portfolio/{symbol}")
    public PortfolioSnapshot snapshot(@PathVariable String symbol) {
        Position p = positions.getOrDefault(symbol, new Position());
        return new PortfolioSnapshot(symbol, p.netQty, p.realizedPnl(), p.unrealizedPnl(), p.sharpeRatio(), p.maxDrawdown(), p.var95());
    }
}

record PortfolioExecutionEvent(String accountId, String symbol, long qty, double price) {}
record PortfolioSnapshot(String symbol, long quantity, double realizedPnl, double unrealizedPnl, double sharpe, double maxDrawdown, double var95) {}

final class Position {
    long netQty;
    double markPrice;
    final Deque<Double> fifo = new ArrayDeque<>();
    final List<Double> returns = new java.util.ArrayList<>();
    double peakValue;
    double maxDrawdown;

    void apply(PortfolioExecutionEvent event) {
        this.netQty += event.qty();
        this.markPrice = event.price();
        this.fifo.add(event.price());
        double portfolioValue = netQty * markPrice;
        peakValue = Math.max(peakValue, portfolioValue);
        if (peakValue > 0) {
            maxDrawdown = Math.max(maxDrawdown, (peakValue - portfolioValue) / peakValue);
        }
        returns.add(event.price() / (fifo.peekFirst() == null ? event.price() : fifo.peekFirst()) - 1.0);
    }

    double realizedPnl() { return 0.0; }
    double unrealizedPnl() { return netQty * (markPrice - (fifo.peekFirst() == null ? markPrice : fifo.peekFirst())); }
    double sharpeRatio() {
        if (returns.isEmpty()) { return 0.0; }
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double std = Math.sqrt(returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(1e-9));
        return std == 0.0 ? 0.0 : mean / std;
    }
    double maxDrawdown() { return maxDrawdown; }
    double var95() {
        if (returns.size() < 10) { return 0.0; }
        List<Double> sorted = returns.stream().sorted().toList();
        int idx = (int) Math.floor(sorted.size() * 0.05);
        return Math.abs(sorted.get(idx));
    }
}
