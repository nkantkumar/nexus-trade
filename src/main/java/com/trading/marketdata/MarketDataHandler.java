package com.trading.marketdata;

import quickfix.FieldNotFound;
import quickfix.field.MDEntryDate;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.NoMDEntries;
import quickfix.fix44.MarketDataSnapshotFullRefresh;

/**
 * Parses FIX 4.4 {@link MarketDataSnapshotFullRefresh} repeating groups into best bid/ask.
 */
public final class MarketDataHandler {

    private final BestQuoteBook quoteBook;

    public MarketDataHandler(BestQuoteBook quoteBook) {
        this.quoteBook = quoteBook;
    }

    public void processMarketData(MarketDataSnapshotFullRefresh message) throws FieldNotFound {
        String symbol = message.getSymbol().getValue();

        NoMDEntries countField = new NoMDEntries();
        message.get(countField);
        int totalEntries = countField.getValue();

        double bestBid = 0;
        double bestAsk = Double.MAX_VALUE;
        long bestBidSize = 0;
        long bestAskSize = 0;

        for (int i = 1; i <= totalEntries; i++) {
            MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
            message.getGroup(i, group);

            MDEntryType type = new MDEntryType();
            MDEntryPx price = new MDEntryPx();
            MDEntrySize size = new MDEntrySize();
            MDEntryDate date = new MDEntryDate();

            group.get(type);
            group.get(price);
            if (group.isSetField(date)) {
                group.get(date);
            }

            char entryType = type.getValue();
            double entryPrice = price.getValue();
            long entrySize = group.isSetField(size) ? (long) size.getValue() : 0L;

            switch (entryType) {
                case MDEntryType.BID -> {
                    if (entryPrice > bestBid) {
                        bestBid = entryPrice;
                        bestBidSize = entrySize;
                    }
                }
                case MDEntryType.OFFER -> {
                    if (entryPrice < bestAsk) {
                        bestAsk = entryPrice;
                        bestAskSize = entrySize;
                    }
                }
                case MDEntryType.TRADE -> {
                    // Trade prints do not update BBO; extend BestQuoteBook if you need last-sale handling.
                }
                default -> {
                    // other entry types ignored
                }
            }
        }

        if (bestAsk == Double.MAX_VALUE) {
            bestAsk = 0;
        }
        quoteBook.updateBestQuote(symbol, bestBid, bestBidSize, bestAsk, bestAskSize);
    }
}
