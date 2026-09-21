package com.trading.settlement.allocation;

import java.time.LocalDate;
import java.util.List;

/**
 * Data structures for Equity Block Trade Allocation to underlying accounts/funds.
 */
public class AllocationInstruction {

    public record AccountAllocation(
            String targetAccountId,
            String custodianAccountId,
            double percentage,
            long allocatedQuantity,
            double allocatedAmount
    ) {}

    private final String blockTradeId;
    private final String symbol;
    private final String side;
    private final long totalQuantity;
    private final double price;
    private final String currency;
    private final LocalDate tradeDate;
    private final LocalDate settlementDate;
    private final List<AccountAllocation> allocations;

    public AllocationInstruction(String blockTradeId, String symbol, String side,
                                 long totalQuantity, double price, String currency,
                                 LocalDate tradeDate, LocalDate settlementDate,
                                 List<AccountAllocation> allocations) {
        this.blockTradeId = blockTradeId;
        this.symbol = symbol;
        this.side = side;
        this.totalQuantity = totalQuantity;
        this.price = price;
        this.currency = currency;
        this.tradeDate = tradeDate;
        this.settlementDate = settlementDate;
        this.allocations = allocations;
    }

    public String getBlockTradeId() { return blockTradeId; }
    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public long getTotalQuantity() { return totalQuantity; }
    public double getPrice() { return price; }
    public String getCurrency() { return currency; }
    public LocalDate getTradeDate() { return tradeDate; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public List<AccountAllocation> getAllocations() { return allocations; }
}
