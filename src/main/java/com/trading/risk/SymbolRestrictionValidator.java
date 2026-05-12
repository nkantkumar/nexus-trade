package com.trading.risk;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Symbol restriction validator - prevents trading on restricted symbols
 */
public class SymbolRestrictionValidator implements RiskValidator {
    private final Set<String> restrictedSymbols;
    private final Set<String> restrictedForShort;
    private final Map<String, String> symbolRestrictions;

    public SymbolRestrictionValidator() {
        this.restrictedSymbols = new HashSet<>();
        this.restrictedForShort = new HashSet<>();
        this.symbolRestrictions = new ConcurrentHashMap<>();

        // Initialize with default restrictions
        restrictedSymbols.addAll(Arrays.asList("PENNY-*", "OTC-*"));
        restrictedForShort.addAll(Arrays.asList("HARD-TO-BORROW-*"));
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        String symbol = order.getSymbol();

        // Check if symbol is completely restricted
        if (isSymbolRestricted(symbol)) {
            return RiskCheckResult.fail(RiskCheckType.SYMBOL_RESTRICTION,
                    String.format("Trading restricted for symbol: %s", symbol));
        }

        // Check short selling restrictions
        if (order.getSide() == OrderSide.SELL && isShortRestricted(symbol)) {
            // Check if account has locate for short
            if (!hasShortLocate(account, symbol, order.getQuantity())) {
                return RiskCheckResult.fail(RiskCheckType.SYMBOL_RESTRICTION,
                        String.format("Short selling restricted for symbol: %s. No locate available.", symbol));
            }
        }

        // Check minimum price requirements
        double minPrice = getMinimumPrice(symbol);
        if (order.getPrice() < minPrice) {
            return RiskCheckResult.fail(RiskCheckType.SYMBOL_RESTRICTION,
                    String.format("Price %.4f below minimum allowed %.4f for %s",
                            order.getPrice(), minPrice, symbol));
        }

        // Check lot size requirements
        int lotSize = getLotSize(symbol);
        if (order.getQuantity() % lotSize != 0) {
            return RiskCheckResult.fail(RiskCheckType.SYMBOL_RESTRICTION,
                    String.format("Quantity %d not multiple of lot size %d for %s",
                            order.getQuantity(), lotSize, symbol));
        }

        return RiskCheckResult.pass(RiskCheckType.SYMBOL_RESTRICTION);
    }

    private boolean isSymbolRestricted(String symbol) {
        for (String restricted : restrictedSymbols) {
            if (matchPattern(symbol, restricted)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShortRestricted(String symbol) {
        for (String restricted : restrictedForShort) {
            if (matchPattern(symbol, restricted)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchPattern(String symbol, String pattern) {
        if (pattern.endsWith("*")) {
            return symbol.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return symbol.equals(pattern);
    }

    private boolean hasShortLocate(RiskAccount account, String symbol, long quantity) {
        // Would integrate with stock borrow/locate system
        return true; // Simplified
    }

    private double getMinimumPrice(String symbol) {
        // Would fetch from market data
        return 0.0001;
    }

    private int getLotSize(String symbol) {
        // Would fetch from exchange rules
        return 1; // Default to 1 share
    }

    public void addRestrictedSymbol(String symbol) {
        restrictedSymbols.add(symbol);
    }

    public void addShortRestrictedSymbol(String symbol) {
        restrictedForShort.add(symbol);
    }

    @Override
    public int getPriority() { return 15; }
}

class TradingSchedule {
    private final String regularOpen;
    private final String regularClose;
    private final String preOpen;
    private final String postClose;
    private final boolean noTradingOnWeekends;

    public TradingSchedule(String regularOpen, String regularClose,
                           String preOpen, String postClose,
                           boolean noTradingOnWeekends) {
        this.regularOpen = regularOpen;
        this.regularClose = regularClose;
        this.preOpen = preOpen;
        this.postClose = postClose;
        this.noTradingOnWeekends = noTradingOnWeekends;
    }

    public String getRegularOpen() { return regularOpen; }
    public String getRegularClose() { return regularClose; }
    public String getPreOpen() { return preOpen; }
    public String getPostClose() { return postClose; }
    public boolean isNoTradingOnWeekends() { return noTradingOnWeekends; }
}

enum TradingSession {
    PRE_MARKET,
    REGULAR,
    POST_MARKET,
    CLOSED
}
