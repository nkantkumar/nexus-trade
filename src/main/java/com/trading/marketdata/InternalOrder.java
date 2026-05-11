package com.trading.marketdata;

/**
 * Order representation for venue adapters (FIX / OUCH). Kept separate from the matching engine domain model.
 */
public class InternalOrder {

    private final String orderId;
    private final String symbol;
    private final VenueSide side;
    private long quantity;
    private final double price;
    private volatile VenueOrderStatus status;

    public InternalOrder(String orderId, String symbol, VenueSide side, long quantity, double price) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
        this.status = VenueOrderStatus.SENT;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public VenueSide getSide() {
        return side;
    }

    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public VenueOrderStatus getStatus() {
        return status;
    }

    public void setStatus(VenueOrderStatus status) {
        this.status = status;
    }
}
