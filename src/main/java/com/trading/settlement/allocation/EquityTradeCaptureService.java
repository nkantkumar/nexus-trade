package com.trading.settlement.allocation;

import com.trading.settlement.aeron.AeronTradeCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Equities Trade Capture Engine. Ingests raw trades from Aeron IPC streams or FIX engines,
 * enriches them with settlement cycles (T+1 US market standard or T+2 global standard),
 * and prepares them for clearing novation.
 */
@Service
public class EquityTradeCaptureService {
    private static final Logger log = LoggerFactory.getLogger(EquityTradeCaptureService.class);

    public record CapturedEquityTrade(
            String tradeId,
            String buyOrderId,
            String sellOrderId,
            String buyAccountId,
            String sellAccountId,
            String symbol,
            long quantity,
            double price,
            double totalValue,
            String currency,
            LocalDate tradeDate,
            LocalDate settlementDate,
            String status
    ) {}

    private final Map<String, CapturedEquityTrade> capturedTrades = new ConcurrentHashMap<>();

    public CapturedEquityTrade captureAeronTrade(AeronTradeCodec.TradeEvent event, int settlementDays) {
        LocalDate tradeDate = LocalDate.now();
        LocalDate settlementDate = calculateSettlementDate(tradeDate, settlementDays);
        double totalValue = Math.round(event.quantity() * event.price() * 100.0) / 100.0;

        CapturedEquityTrade trade = new CapturedEquityTrade(
                event.tradeId(),
                event.buyOrderId(),
                event.sellOrderId(),
                event.buyAccountId(),
                event.sellAccountId(),
                event.symbol(),
                event.quantity(),
                event.price(),
                totalValue,
                event.currency() != null && !event.currency().isBlank() ? event.currency() : "USD",
                tradeDate,
                settlementDate,
                "CAPTURED"
        );

        capturedTrades.put(trade.tradeId(), trade);
        log.info("Captured Aeron Trade {}: {} shares of {} @ {} (Settlement T+{}: {})",
                trade.tradeId(), trade.quantity(), trade.symbol(), trade.price(), settlementDays, trade.settlementDate());

        return trade;
    }

    public CapturedEquityTrade getTrade(String tradeId) {
        return capturedTrades.get(tradeId);
    }

    public Map<String, CapturedEquityTrade> getAllCapturedTrades() {
        return capturedTrades;
    }

    public LocalDate calculateSettlementDate(LocalDate startDate, int businessDays) {
        LocalDate result = startDate;
        int addedDays = 0;
        while (addedDays < businessDays) {
            result = result.plusDays(1);
            if (!(result.getDayOfWeek() == DayOfWeek.SATURDAY || result.getDayOfWeek() == DayOfWeek.SUNDAY)) {
                addedDays++;
            }
        }
        return result;
    }
}
