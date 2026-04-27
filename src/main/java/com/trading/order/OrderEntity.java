package com.trading.order;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    private UUID id;
    private String symbol;
    private long quantity;
    private long filledQuantity;
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private OrderState state;
    private Instant updatedAt;
    @Version
    private long version;

    public static OrderEntity create(CreateOrderCommand command) {
        OrderEntity entity = new OrderEntity();
        entity.id = UUID.randomUUID();
        entity.symbol = command.symbol();
        entity.quantity = command.quantity();
        entity.price = command.limitPrice();
        entity.state = OrderState.NEW;
        entity.updatedAt = Instant.now();
        return entity;
    }

    public UUID getId() { return id; }
    public String getSymbol() { return symbol; }
    public long getQuantity() { return quantity; }
    public long getFilledQuantity() { return filledQuantity; }
    public BigDecimal getPrice() { return price; }
    public OrderState getState() { return state; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setQuantity(long quantity) { this.quantity = quantity; this.updatedAt = Instant.now(); }
    public void setPrice(BigDecimal price) { this.price = price; this.updatedAt = Instant.now(); }
    public void setState(OrderState state) { this.state = state; this.updatedAt = Instant.now(); }
}
