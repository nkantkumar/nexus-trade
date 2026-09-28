package com.trading.referencedata;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Service
public class ReferenceDataService {
    private final Map<String, SymbolMetadata> symbols = new ConcurrentHashMap<>();
    private final Map<String, FxPairMetadata> fxPairs = new ConcurrentHashMap<>();
    private final Map<String, CurrencyMapping> currencyMappings = new ConcurrentHashMap<>();
    private final Set<LocalDate> marketHolidays = ConcurrentHashMap.newKeySet();

    public ReferenceDataService() {
        symbols.put("AAPL", new SymbolMetadata("AAPL", "NASDAQ", "US", "TECH"));
        symbols.put("EURUSD", new SymbolMetadata("EURUSD", "FX", "EU", "FOREX"));
        symbols.put("USDJPY", new SymbolMetadata("USDJPY", "FX", "GLOBAL", "FOREX"));
        symbols.put("GBPUSD", new SymbolMetadata("GBPUSD", "FX", "GLOBAL", "FOREX"));
        symbols.put("AUDUSD", new SymbolMetadata("AUDUSD", "FX", "GLOBAL", "FOREX"));
        symbols.put("USDCHF", new SymbolMetadata("USDCHF", "FX", "GLOBAL", "FOREX"));
        symbols.put("USDCAD", new SymbolMetadata("USDCAD", "FX", "GLOBAL", "FOREX"));
        symbols.put("EURGBP", new SymbolMetadata("EURGBP", "FX", "GLOBAL", "FOREX"));
        symbols.put("EURJPY", new SymbolMetadata("EURJPY", "FX", "GLOBAL", "FOREX"));

        currencyMappings.put("NASDAQ", new CurrencyMapping("NASDAQ", "USD"));
        currencyMappings.put("FX_EBS", new CurrencyMapping("FX_EBS", "USD"));
        currencyMappings.put("FX_CURRENEX", new CurrencyMapping("FX_CURRENEX", "USD"));

        initializeFxPairs();
    }

    private void initializeFxPairs() {
        fxPairs.put("EURUSD", new FxPairMetadata("EURUSD", "EUR", "USD", 0.0001, 4, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("USDJPY", new FxPairMetadata("USDJPY", "USD", "JPY", 0.01, 2, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("GBPUSD", new FxPairMetadata("GBPUSD", "GBP", "USD", 0.0001, 4, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("AUDUSD", new FxPairMetadata("AUDUSD", "AUD", "USD", 0.0001, 4, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("USDCHF", new FxPairMetadata("USDCHF", "USD", "CHF", 0.0001, 4, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("USDCAD", new FxPairMetadata("USDCAD", "USD", "CAD", 0.0001, 4, 100_000, 1, "T+1 SPOT"));
        fxPairs.put("EURGBP", new FxPairMetadata("EURGBP", "EUR", "GBP", 0.0001, 4, 100_000, 2, "T+2 SPOT"));
        fxPairs.put("EURJPY", new FxPairMetadata("EURJPY", "EUR", "JPY", 0.01, 2, 100_000, 2, "T+2 SPOT"));
    }

    public SymbolMetadata symbol(String symbol) {
        return symbols.get(symbol);
    }

    public FxPairMetadata fxPair(String pair) {
        return fxPairs.get(pair.toUpperCase());
    }

    public Collection<FxPairMetadata> allFxPairs() {
        return fxPairs.values();
    }

    public CurrencyMapping currency(String venue) {
        return currencyMappings.get(venue);
    }

    public TradingSession tradingSession(String venue) {
        if (venue != null && venue.toUpperCase().startsWith("FX")) {
            return new TradingSession(venue, LocalTime.of(0, 0), LocalTime.of(23, 59), "UTC"); // 24/5 FX market
        }
        return new TradingSession(venue, LocalTime.of(9, 30), LocalTime.of(16, 0), "America/New_York");
    }

    public boolean holiday(LocalDate date) {
        return marketHolidays.contains(date);
    }
}

@RestController
@RequestMapping("/reference-data")
class ReferenceDataController {
    private final ReferenceDataService service;

    ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/symbols/{symbol}")
    SymbolMetadata symbol(@PathVariable String symbol) {
        return service.symbol(symbol);
    }

    @GetMapping("/fx/pairs")
    Collection<FxPairMetadata> fxPairs() {
        return service.allFxPairs();
    }

    @GetMapping("/fx/pairs/{pair}")
    FxPairMetadata fxPair(@PathVariable String pair) {
        return service.fxPair(pair);
    }

    @GetMapping("/venue/{venue}/currency")
    CurrencyMapping currency(@PathVariable String venue) {
        return service.currency(venue);
    }
}

record SymbolMetadata(String symbol, String exchange, String country, String assetClass) {}
record FxPairMetadata(String symbol, String baseCurrency, String quoteCurrency, double pipSize, int decimalPlaces, long standardLotSize, int spotSettlementDays, String convention) {}
record TradingSession(String venue, LocalTime open, LocalTime close, String timezone) {}
record CurrencyMapping(String venue, String settlementCurrency) {}
