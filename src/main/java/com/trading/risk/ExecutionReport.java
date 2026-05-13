package com.trading.risk;

/**
 * Execution report from exchange (FIX format)
 */
public class ExecutionReport {
    private String orderId;
    private String accountId;
    private String symbol;
    private OrderSide side;
    private long quantity;
    private long cumQuantity;
    private double price;
    private double avgPrice;
    private String execType;
    private String ordStatus;
    private String rejectReason;
    private long timestamp;

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public OrderSide getSide() { return side; }
    public void setSide(OrderSide side) { this.side = side; }

    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }

    public long getCumQuantity() { return cumQuantity; }
    public void setCumQuantity(long cumQuantity) { this.cumQuantity = cumQuantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }

    public String getExecType() { return execType; }
    public void setExecType(String execType) { this.execType = execType; }

    public String getOrdStatus() { return ordStatus; }
    public void setOrdStatus(String ordStatus) { this.ordStatus = ordStatus; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
