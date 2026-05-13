package com.trading.risk;

import java.util.UUID;

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

