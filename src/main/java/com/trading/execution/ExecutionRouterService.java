package com.trading.execution;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionNotFound;

@Service
public class ExecutionRouterService {
    private final KafkaTemplate<String, ExecutionReportEvent> kafkaTemplate;
    private final VenueRegistry venueRegistry = new VenueRegistry();

    public ExecutionRouterService(KafkaTemplate<String, ExecutionReportEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public List<ChildOrder> slice(OrderIntent intent) {
        long sliceQty = Math.max(1, intent.quantity() / 4);
        return List.of(
                new ChildOrder(UUID.randomUUID().toString(), intent.symbol(), sliceQty, intent.limitPrice(), "TWAP"),
                new ChildOrder(UUID.randomUUID().toString(), intent.symbol(), intent.quantity() - sliceQty, intent.limitPrice(), "VWAP"));
    }

    /**
     * Smart Order Routing (SOR) for FX trades selecting optimal bank Liquidity Provider (LP).
     */
    public Venue selectBestFxVenue(String currencyPair, BigDecimal desiredRate, String side) {
        List<Venue> fxVenues = venueRegistry.fxVenues();
        return fxVenues.stream()
                .min(Comparator.comparing((Venue v) -> v.price().subtract(desiredRate).abs())
                        .thenComparing(v -> BigDecimal.valueOf(v.latencyScore() + (1.0 - v.fillRateScore()))))
                .orElse(fxVenues.get(0));
    }

    public Venue selectBestVenue(List<Venue> venues, BigDecimal bestPrice) {
        return venues.stream()
                .min(Comparator.comparing((Venue v) -> v.price().subtract(bestPrice).abs())
                        .thenComparing(v -> BigDecimal.valueOf(v.latencyScore() + (1.0 - v.fillRateScore()))))
                .orElseThrow();
    }

    public void routeFIX(ChildOrder childOrder) {
        Venue venue = childOrder.symbol().length() == 6 ?
                selectBestFxVenue(childOrder.symbol(), childOrder.limitPrice(), "BUY") :
                selectBestVenue(venueRegistry.all(), childOrder.limitPrice());
        Message message = new Message();
        try {
            Session.sendToTarget(message, venue.sessionId());
        } catch (SessionNotFound ex) {
            throw new IllegalStateException("Unable to route FIX order to " + venue.venueId(), ex);
        }
    }

    public void aggregate(ExecutionReportEvent report) {
        kafkaTemplate.send("execution-events", report.orderId(), report);
    }
}

record OrderIntent(String symbol, long quantity, BigDecimal limitPrice) {}
record ChildOrder(String childOrderId, String symbol, long quantity, BigDecimal limitPrice, String algo) {}
record ExecutionReportEvent(String orderId, long filledQty, BigDecimal avgPrice, String status, Instant ts) {}
record Venue(String venueId, String sessionId, BigDecimal price, double latencyScore, double fillRateScore) {}

class VenueRegistry {
    List<Venue> all() {
        return List.of(
                new Venue("VENUE_A", "FIX-A", BigDecimal.valueOf(101.10), 0.9, 0.94),
                new Venue("VENUE_B", "FIX-B", BigDecimal.valueOf(101.05), 0.8, 0.90));
    }

    List<Venue> fxVenues() {
        return List.of(
                new Venue("EBS_LP", "FIX-EBS", BigDecimal.valueOf(1.0851), 0.95, 0.98),
                new Venue("CURRENEX_LP", "FIX-CURRENEX", BigDecimal.valueOf(1.0850), 0.92, 0.96),
                new Venue("HOTSPOT_FX", "FIX-HOTSPOT", BigDecimal.valueOf(1.0852), 0.90, 0.95));
    }
}

