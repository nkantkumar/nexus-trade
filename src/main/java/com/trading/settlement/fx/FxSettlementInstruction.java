package com.trading.settlement.fx;

import java.time.LocalDate;

public class FxSettlementInstruction {
    private final String instructionId;
    private final String tradeId;
    private final String buyerAccountId;
    private final String sellerAccountId;
    private final String baseCurrency;
    private final double baseAmount;
    private final String quoteCurrency;
    private final double quoteAmount;
    private final LocalDate valueDate;
    private String status;

    public FxSettlementInstruction(
            String instructionId, String tradeId, String buyerAccountId, String sellerAccountId,
            String baseCurrency, double baseAmount, String quoteCurrency, double quoteAmount,
            LocalDate valueDate, String status
    ) {
        this.instructionId = instructionId;
        this.tradeId = tradeId;
        this.buyerAccountId = buyerAccountId;
        this.sellerAccountId = sellerAccountId;
        this.baseCurrency = baseCurrency;
        this.baseAmount = baseAmount;
        this.quoteCurrency = quoteCurrency;
        this.quoteAmount = quoteAmount;
        this.valueDate = valueDate;
        this.status = status;
    }

    public String instructionId() { return instructionId; }
    public String tradeId() { return tradeId; }
    public String buyerAccountId() { return buyerAccountId; }
    public String sellerAccountId() { return sellerAccountId; }
    public String baseCurrency() { return baseCurrency; }
    public double baseAmount() { return baseAmount; }
    public String quoteCurrency() { return quoteCurrency; }
    public double quoteAmount() { return quoteAmount; }
    public LocalDate valueDate() { return valueDate; }
    public String status() { return status; }
    public void setStatus(String status) { this.status = status; }
}
