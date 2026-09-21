package com.trading.settlement.dvp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Settlement Instruction tracking the Delivery versus Payment (DvP) state machine.
 */
public class SettlementInstruction {

    public enum Status {
        INITIATED,
        MATCHED,
        CASH_RESERVED,
        SECURITIES_RESERVED,
        SETTLED,
        FAILED_CASH_SHORTAGE,
        FAILED_SECURITIES_SHORTAGE,
        PARTIALLY_SETTLED,
        RECONCILED
    }

    private final String instructionId;
    private final String obligationOrTradeId;
    private final String delivererAccountId;
    private final String receiverAccountId;
    private final String symbol;
    private final long quantity;
    private final double cashAmount;
    private final String currency;
    private final LocalDate settlementDate;
    private final DvPModel dvpModel;
    private Status status;
    private String pacs008MessageId;
    private String sese023MessageId;
    private final Instant createdAt;
    private Instant settledAt;

    public SettlementInstruction(String instructionId, String obligationOrTradeId,
                                 String delivererAccountId, String receiverAccountId,
                                 String symbol, long quantity, double cashAmount,
                                 String currency, LocalDate settlementDate, DvPModel dvpModel) {
        this.instructionId = instructionId;
        this.obligationOrTradeId = obligationOrTradeId;
        this.delivererAccountId = delivererAccountId;
        this.receiverAccountId = receiverAccountId;
        this.symbol = symbol;
        this.quantity = quantity;
        this.cashAmount = cashAmount;
        this.currency = currency;
        this.settlementDate = settlementDate;
        this.dvpModel = dvpModel;
        this.status = Status.INITIATED;
        this.createdAt = Instant.now();
    }

    public String getInstructionId() { return instructionId; }
    public String getObligationOrTradeId() { return obligationOrTradeId; }
    public String getDelivererAccountId() { return delivererAccountId; }
    public String getReceiverAccountId() { return receiverAccountId; }
    public String getSymbol() { return symbol; }
    public long getQuantity() { return quantity; }
    public double getCashAmount() { return cashAmount; }
    public String getCurrency() { return currency; }
    public LocalDate getSettlementDate() { return settlementDate; }
    public DvPModel getDvpModel() { return dvpModel; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getPacs008MessageId() { return pacs008MessageId; }
    public void setPacs008MessageId(String id) { this.pacs008MessageId = id; }
    public String getSese023MessageId() { return sese023MessageId; }
    public void setSese023MessageId(String id) { this.sese023MessageId = id; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant timestamp) { this.settledAt = timestamp; }
}
