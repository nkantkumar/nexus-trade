package com.trading.fx;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

@Service
@RestController
@RequestMapping("/fx")
public class FxTradingService {
    private static final Logger log = LoggerFactory.getLogger(FxTradingService.class);

    private final Map<String, FxQuote> marketQuotes = new ConcurrentHashMap<>();
    private final Map<String, List<FxTrade>> accountTrades = new ConcurrentHashMap<>();
    private final Map<String, Double> accountBalances = new ConcurrentHashMap<>();
    private final FxRateCalculator rateCalculator = new FxRateCalculator();
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    public FxTradingService(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        initializeDefaultQuotes();
    }

    private void initializeDefaultQuotes() {
        marketQuotes.put("EURUSD", FxQuote.create("EURUSD", 1.0850, 1.0852, "BARCLAYS_LP", 0.0001));
        marketQuotes.put("USDJPY", FxQuote.create("USDJPY", 148.20, 148.23, "CITI_LP", 0.01));
        marketQuotes.put("GBPUSD", FxQuote.create("GBPUSD", 1.2950, 1.2953, "HSBC_LP", 0.0001));
        marketQuotes.put("EURJPY", FxQuote.create("EURJPY", 160.80, 160.85, "UBS_LP", 0.01));
    }

    @PostMapping("/quotes/update")
    public FxQuote updateQuote(@RequestBody FxQuote quote) {
        marketQuotes.put(quote.symbol(), quote);
        return quote;
    }

    @GetMapping("/quotes/{symbol}")
    public FxQuote getQuote(@PathVariable String symbol) {
        FxQuote quote = marketQuotes.get(symbol.toUpperCase());
        if (quote == null) {
            throw new IllegalArgumentException("Unsupported or unknown FX symbol: " + symbol);
        }
        return quote;
    }

    @GetMapping("/quotes")
    public Collection<FxQuote> getAllQuotes() {
        return marketQuotes.values();
    }

    @GetMapping("/forward-quote")
    public Map<String, Object> getForwardQuote(
            @RequestParam String symbol,
            @RequestParam double baseRate,
            @RequestParam double quoteRate,
            @RequestParam int days
    ) {
        FxQuote spotQuote = getQuote(symbol);
        FxPair pair = FxPair.valueOf(symbol.toUpperCase());
        double forwardRate = rateCalculator.calculateForwardRate(spotQuote.mid(), baseRate, quoteRate, days);
        double forwardPointsPips = rateCalculator.calculateForwardPointsPips(spotQuote.mid(), forwardRate, pair.getPipSize());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("symbol", symbol);
        res.put("spotMid", spotQuote.mid());
        res.put("forwardRate", forwardRate);
        res.put("forwardPointsPips", forwardPointsPips);
        res.put("daysToValueDate", days);
        res.put("valueDate", LocalDate.now().plusDays(days));
        return res;
    }

    @PostMapping("/execute")
    public FxTrade executeTrade(@RequestBody FxTradeRequest request) {
        FxPair pair = FxPair.valueOf(request.symbol().toUpperCase());
        FxQuote quote = getQuote(request.symbol());

        double execSpot = "BUY".equalsIgnoreCase(request.side()) ? quote.ask() : quote.bid();
        double forwardPoints = 0.0;
        double allInRate = execSpot;

        if (request.fxOrderType() == FxOrderType.FORWARD && request.daysToValueDate() > 0) {
            forwardPoints = request.baseRate() != 0 || request.quoteRate() != 0 ?
                    rateCalculator.calculateForwardPointsPips(execSpot, rateCalculator.calculateForwardRate(execSpot, request.baseRate(), request.quoteRate(), request.daysToValueDate()), pair.getPipSize()) : 5.0;
            allInRate = execSpot + (forwardPoints * pair.getPipSize());
        }

        long units = (long) (request.lots() * pair.getStandardLotSize());
        LocalDate valDate = request.valueDate() != null ? request.valueDate() :
                LocalDate.now().plusDays(request.daysToValueDate() > 0 ? request.daysToValueDate() : 2);

        FxTrade trade = new FxTrade(
                "FXTRD-" + UUID.randomUUID().toString().substring(0, 8),
                request.accountId(),
                pair.name(),
                request.fxOrderType(),
                request.side().toUpperCase(),
                request.lots(),
                units,
                execSpot,
                forwardPoints,
                allInRate,
                valDate,
                pair.getBaseCurrency(),
                pair.getQuoteCurrency(),
                request.counterpartyId() != null ? request.counterpartyId() : "CLS_BANK_LP",
                Instant.now()
        );

        accountTrades.computeIfAbsent(request.accountId(), k -> new ArrayList<>()).add(trade);
        log.info("FX Trade Executed: {} {} {} lots @ {} (Value Date: {})", trade.side(), trade.symbol(), trade.lots(), trade.allInRate(), trade.valueDate());

        if (kafkaTemplate != null) {
            kafkaTemplate.send("fx-trade-events", trade.tradeId(), trade);
        }

        return trade;
    }

    @GetMapping("/arbitrage/triangular")
    public Map<String, Object> checkTriangularArbitrage(
            @RequestParam(defaultValue = "EURUSD") String pairA,
            @RequestParam(defaultValue = "USDJPY") String pairB,
            @RequestParam(defaultValue = "EURJPY") String pairC
    ) {
        FxQuote qA = getQuote(pairA);
        FxQuote qB = getQuote(pairB);
        FxQuote qC = getQuote(pairC);

        double syntheticCross = qA.mid() * qB.mid();
        double directCross = qC.mid();
        double discrepancyPips = Math.abs(syntheticCross - directCross) / 0.01;
        boolean profitable = discrepancyPips > 2.0;

        Map<String, Object> arb = new LinkedHashMap<>();
        arb.put("pairA", pairA + " (" + qA.mid() + ")");
        arb.put("pairB", pairB + " (" + qB.mid() + ")");
        arb.put("pairC", pairC + " (" + qC.mid() + ")");
        arb.put("syntheticCross", syntheticCross);
        arb.put("directCross", directCross);
        arb.put("discrepancyPips", discrepancyPips);
        arb.put("arbitrageOpportunityDetected", profitable);
        return arb;
    }

    @GetMapping("/trades/{accountId}")
    public List<FxTrade> getAccountTrades(@PathVariable String accountId) {
        return accountTrades.getOrDefault(accountId, List.of());
    }
}

record FxTradeRequest(
        String accountId,
        String symbol,
        FxOrderType fxOrderType,
        String side,
        double lots,
        int daysToValueDate,
        LocalDate valueDate,
        double baseRate,
        double quoteRate,
        String counterpartyId
) {}
