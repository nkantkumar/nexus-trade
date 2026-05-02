package com.trading.marketdata;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import com.trading.infra.chronicle.ChronicleAppenders;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {
    private static final String MARKET_DATA_CHRONICLE_PATH = "build/chronicle/market-data";

    private final KafkaTemplate<String, MarketDataEvent> kafkaTemplate;
    private final SymbolMapper mapper;
    private final TreeMap<BigDecimal, Long> bids = new TreeMap<>();
    private final TreeMap<BigDecimal, Long> asks = new TreeMap<>();

    public MarketDataService(KafkaTemplate<String, MarketDataEvent> kafkaTemplate, SymbolMapper mapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    @KafkaListener(topics = {"raw-equities", "raw-forex", "raw-crypto"}, groupId = "market-data")
    public void ingest(RawTick tick) {
        String canonicalSymbol = mapper.toCanonical(tick.source(), tick.symbol());
        MarketDataEvent event =
                new MarketDataEvent(canonicalSymbol, tick.price(), tick.size(), tick.side(), Instant.now(), tick.source());
        rebuildBook(event);
        ChronicleAppenders.forPath(MARKET_DATA_CHRONICLE_PATH).writeText(event.toString());
        kafkaTemplate.send("market-data-events", canonicalSymbol, event);
    }

    private void rebuildBook(MarketDataEvent event) {
        Map<BigDecimal, Long> side = "BID".equals(event.side()) ? bids : asks;
        side.merge(event.price(), event.size(), Long::sum);
    }
}

record RawTick(String source, String symbol, BigDecimal price, long size, String side) {}
record MarketDataEvent(String symbol, BigDecimal price, long size, String side, Instant ts, String feed) {}

@Service
class SymbolMapper {
    String toCanonical(String source, String symbol) {
        return source.toUpperCase() + ":" + symbol.toUpperCase();
    }
}
