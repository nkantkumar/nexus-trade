package com.trading.referencedata;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final Map<String, CurrencyMapping> currencyMappings = new ConcurrentHashMap<>();
    private final Set<LocalDate> marketHolidays = ConcurrentHashMap.newKeySet();

    public ReferenceDataService() {
        symbols.put("AAPL", new SymbolMetadata("AAPL", "NASDAQ", "US", "TECH"));
        symbols.put("EURUSD", new SymbolMetadata("EURUSD", "FX", "EU", "FOREX"));
        currencyMappings.put("NASDAQ", new CurrencyMapping("NASDAQ", "USD"));
    }

    public SymbolMetadata symbol(String symbol) {
        return symbols.get(symbol);
    }

    public CurrencyMapping currency(String venue) {
        return currencyMappings.get(venue);
    }

    public TradingSession tradingSession(String venue) {
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

    @GetMapping("/venue/{venue}/currency")
    CurrencyMapping currency(@PathVariable String venue) {
        return service.currency(venue);
    }
}

record SymbolMetadata(String symbol, String exchange, String country, String assetClass) {}
record TradingSession(String venue, LocalTime open, LocalTime close, String timezone) {}
record CurrencyMapping(String venue, String settlementCurrency) {}
