package com.trading.settlement.clearing;

import com.trading.settlement.allocation.EquityTradeCaptureService.CapturedEquityTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central Counterparty (CCP) Clearing Engine.
 * Responsibilities:
 * 1. Trade Novation: Interposes CCP as buyer to seller and seller to buyer.
 * 2. Multilateral Netting: Aggregates multiple gross equity trades into single net securities & cash obligations.
 * 3. Clearing Member Initial & Variation Margin Requirement Calculations.
 */
@Service
public class CcpClearingEngine {
    private static final Logger log = LoggerFactory.getLogger(CcpClearingEngine.class);

    private static final String DEFAULT_CCP_ID = "NEXUS_CCP_01";
    private final Map<String, ClearedTrade> clearedTrades = new ConcurrentHashMap<>();

    public record MarginRequirement(
            String clearingMemberId,
            double initialMarginRequired,
            double variationMarginRequired,
            double totalMarginRequired,
            String currency
    ) {}

    public ClearedTrade novateTrade(CapturedEquityTrade trade) {
        String clearedId = "CLR-" + trade.tradeId();
        ClearedTrade cleared = new ClearedTrade(
                clearedId,
                trade.tradeId(),
                trade.buyAccountId(),
                trade.sellAccountId(),
                DEFAULT_CCP_ID,
                trade.symbol(),
                trade.quantity(),
                trade.price(),
                trade.totalValue(),
                trade.currency(),
                trade.settlementDate(),
                "NOVATED"
        );
        clearedTrades.put(clearedId, cleared);
        log.info("CCP Novated trade {}: {} vs {} for {} shares of {}",
                clearedId, trade.buyAccountId(), trade.sellAccountId(), trade.quantity(), trade.symbol());
        return cleared;
    }

    public List<NettedObligation> calculateMultilateralNetting(List<ClearedTrade> tradesBatch) {
        // Group by ClearingMember + Symbol + SettlementDate + Currency
        class Key {
            final String memberId;
            final String symbol;
            final LocalDate date;
            final String currency;

            Key(String memberId, String symbol, LocalDate date, String currency) {
                this.memberId = memberId;
                this.symbol = symbol;
                this.date = date;
                this.currency = currency;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Key k)) return false;
                return Objects.equals(memberId, k.memberId) && Objects.equals(symbol, k.symbol) &&
                       Objects.equals(date, k.date) && Objects.equals(currency, k.currency);
            }

            @Override
            public int hashCode() {
                return Objects.hash(memberId, symbol, date, currency);
            }
        }

        class Accumulator {
            long grossBuyQty = 0;
            long grossSellQty = 0;
            double grossBuyAmt = 0.0;
            double grossSellAmt = 0.0;
            int count = 0;
        }

        Map<Key, Accumulator> map = new HashMap<>();

        for (ClearedTrade trade : tradesBatch) {
            // Buyer leg
            Key buyKey = new Key(trade.clearingMemberBuyer(), trade.symbol(), trade.settlementDate(), trade.currency());
            Accumulator buyAcc = map.computeIfAbsent(buyKey, k -> new Accumulator());
            buyAcc.grossBuyQty += trade.quantity();
            buyAcc.grossBuyAmt += trade.totalValue();
            buyAcc.count++;

            // Seller leg
            Key sellKey = new Key(trade.clearingMemberSeller(), trade.symbol(), trade.settlementDate(), trade.currency());
            Accumulator sellAcc = map.computeIfAbsent(sellKey, k -> new Accumulator());
            sellAcc.grossSellQty += trade.quantity();
            sellAcc.grossSellAmt += trade.totalValue();
            sellAcc.count++;
        }

        List<NettedObligation> obligations = new ArrayList<>();
        for (Map.Entry<Key, Accumulator> entry : map.entrySet()) {
            Key k = entry.getKey();
            Accumulator acc = entry.getValue();

            long netQuantity = acc.grossBuyQty - acc.grossSellQty;
            double netCashAmount = Math.round((acc.grossBuyAmt - acc.grossSellAmt) * 100.0) / 100.0;
            String obligationId = "NET-" + k.memberId + "-" + k.symbol + "-" + k.date;

            NettedObligation obligation = new NettedObligation(
                    obligationId,
                    k.memberId,
                    k.symbol,
                    k.currency,
                    k.date,
                    acc.grossBuyQty,
                    acc.grossSellQty,
                    netQuantity,
                    acc.grossBuyAmt,
                    acc.grossSellAmt,
                    netCashAmount,
                    acc.count
            );
            obligations.add(obligation);
            log.info("Multilateral Netting computed for {}: Net Shares = {}, Net Cash = {} {}",
                    k.memberId, netQuantity, netCashAmount, k.currency);
        }

        return obligations;
    }

    public MarginRequirement calculateMargin(String clearingMemberId, List<ClearedTrade> openTrades, double initialMarginRate, Map<String, Double> priceChanges) {
        double grossExposure = 0.0;
        double uncollectedVariationMargin = 0.0;

        for (ClearedTrade t : openTrades) {
            boolean isBuyer = t.clearingMemberBuyer().equals(clearingMemberId);
            boolean isSeller = t.clearingMemberSeller().equals(clearingMemberId);
            if (!isBuyer && !isSeller) continue;

            grossExposure += t.totalValue();
            Double priceChange = priceChanges.getOrDefault(t.symbol(), 0.0);
            if (isBuyer) {
                // If price fell, buyer loses money -> variation margin required
                if (priceChange < 0) {
                    uncollectedVariationMargin += Math.abs(priceChange) * t.quantity();
                }
            } else {
                // If price rose, seller loses money -> variation margin required
                if (priceChange > 0) {
                    uncollectedVariationMargin += priceChange * t.quantity();
                }
            }
        }

        double im = Math.round(grossExposure * initialMarginRate * 100.0) / 100.0;
        double vm = Math.round(uncollectedVariationMargin * 100.0) / 100.0;

        return new MarginRequirement(clearingMemberId, im, vm, im + vm, "USD");
    }

    public Map<String, ClearedTrade> getClearedTrades() {
        return clearedTrades;
    }
}
