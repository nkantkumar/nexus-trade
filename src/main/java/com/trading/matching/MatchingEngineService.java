package com.trading.matching;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import com.trading.infra.chronicle.ChronicleAppenders;
import org.springframework.stereotype.Service;

@Service
public class MatchingEngineService {
    private static final String MATCHING_CHRONICLE_PATH = "build/chronicle/matching";

    private final AtomicReference<OrderBook> bookRef = new AtomicReference<>(new OrderBook());

    public TradeEvent match(InboundOrder incoming) {
        Thread.currentThread().setName("matching-core-cpu0");
        while (true) {
            OrderBook snapshot = bookRef.get();
            MatchResult result = snapshot.match(incoming);
            if (bookRef.compareAndSet(snapshot, result.nextBook())) {
                if (result.trade() != null) {
                    ChronicleAppenders.forPath(MATCHING_CHRONICLE_PATH).writeText(result.trade().toString());
                }
                return result.trade();
            }
        }
    }
}

record InboundOrder(String orderId, String symbol, Side side, long qty, BigDecimal price, boolean market) {}
record TradeEvent(String buyOrderId, String sellOrderId, BigDecimal price, long qty, Instant ts) {}
enum Side { BUY, SELL }

final class OrderBook {
    private final TreeMap<BigDecimal, Deque<InboundOrder>> bids = new TreeMap<>(Map.of());
    private final TreeMap<BigDecimal, Deque<InboundOrder>> asks = new TreeMap<>(Map.of());

    MatchResult match(InboundOrder incoming) {
        // Lock-free design: this object is treated as immutable snapshot and replaced with CAS.
        TreeMap<BigDecimal, Deque<InboundOrder>> nextBids = new TreeMap<>(bids);
        TreeMap<BigDecimal, Deque<InboundOrder>> nextAsks = new TreeMap<>(asks);
        if (incoming.side() == Side.BUY) {
            return tryCross(incoming, nextAsks, nextBids);
        }
        return tryCross(incoming, nextBids, nextAsks);
    }

    private MatchResult tryCross(
            InboundOrder incoming,
            TreeMap<BigDecimal, Deque<InboundOrder>> opposite,
            TreeMap<BigDecimal, Deque<InboundOrder>> same) {
        if (!opposite.isEmpty()) {
            BigDecimal best = opposite.firstKey();
            Deque<InboundOrder> queue = opposite.get(best);
            if (queue != null && !queue.isEmpty()) {
                InboundOrder resting = queue.pollFirst();
                TradeEvent event = incoming.side() == Side.BUY
                        ? new TradeEvent(incoming.orderId(), resting.orderId(), best, Math.min(incoming.qty(), resting.qty()), Instant.now())
                        : new TradeEvent(resting.orderId(), incoming.orderId(), best, Math.min(incoming.qty(), resting.qty()), Instant.now());
                return new MatchResult(new OrderBook(), event);
            }
        }
        same.computeIfAbsent(incoming.price(), k -> new ArrayDeque<>()).addLast(incoming);
        return new MatchResult(new OrderBook(), null);
    }
}

record MatchResult(OrderBook nextBook, TradeEvent trade) {}
