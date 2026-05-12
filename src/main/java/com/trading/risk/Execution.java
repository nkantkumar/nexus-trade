package com.trading.risk;

import java.util.UUID;package com.trading.risk.model;

/**
 * Execution report from matching engine
 */
public class Execution {
    private final String executionId;
    private final String orderId;
    private final String symbol;
    private final OrderSide side;
    private final long quantity;
    private final double price;
    private final long timestamp;
    private final ExecutionType executionType;
    private final String liquidityProvider;

    public Execution(String executionId, String orderId, String symbol,
                     OrderSide side, long quantity, double price,
                     ExecutionType executionType) {
        this.executionId = executionId;
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.executionType = executionType;
        this.timestamp = System.nanoTime();
        this.liquidityProvider = null;
    }

    // Getters
    public String getExecutionId() { return executionId; }
    public String getOrderId() { return orderId; }
    public String getSymbol() { return symbol; }
    public OrderSide getSide() { return side; }
    public long getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public long getTimestamp() { return timestamp; }
    public ExecutionType getExecutionType() { return executionType; }
    public String getLiquidityProvider() { return liquidityProvider; }

    public double getValue() {
        return quantity * price;
    }
}

/**
 * Execution type
 */
public enum ExecutionType {
    NEW,
    PARTIAL_FILL,
    FILL,
    CANCELLED,
    REPLACED,
    REJECTED,
    EXPIRED,
    TRADE
}

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

/**
 * Trade execution for internal matching engine
 */
public class Trade {
    private final String tradeId;
    private final String buyOrderId;
    private final String sellOrderId;
    private final double price;
    private final long quantity;
    private final long timestamp;

    public Trade(String buyOrderId, String sellOrderId, double price, long quantity) {
        this(UUID.randomUUID().toString(), buyOrderId, sellOrderId, price, quantity, System.nanoTime());
    }

    public Trade(String tradeId, String buyOrderId, String sellOrderId,
                 double price, long quantity, long timestamp) {
        this.tradeId = tradeId;
        this.buyOrderId = buyOrderId;
        this.sellOrderId = sellOrderId;
        this.price = price;
        this.quantity = quantity;
        this.timestamp = timestamp;
    }

    // Getters
    public String getTradeId() { return tradeId; }
    public String getBuyOrderId() { return buyOrderId; }
    public String getSellOrderId() { return sellOrderId; }
    public double getPrice() { return price; }
    public long getQuantity() { return quantity; }
    public long getTimestamp() { return timestamp; }

    public double getValue() {
        return price * quantity;
    }
}
