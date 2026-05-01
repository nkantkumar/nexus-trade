package com.trading.order;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final Map<OrderState, OrderState> flow = new EnumMap<>(Map.of(
            OrderState.NEW, OrderState.PENDING_ACK,
            OrderState.PENDING_ACK, OrderState.PARTIALLY_FILLED,
            OrderState.PARTIALLY_FILLED, OrderState.FILLED));

    public OrderService(
            OrderRepository repository,
            KafkaTemplate<String, OrderEvent> kafkaTemplate,
            StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
    }

    public UUID create(CreateOrderCommand command) {
        OrderEntity order = OrderEntity.create(command);
        repository.save(order);
        cache(order);
        publish(order.getId(), "CREATED");
        return order.getId();
    }

    public void modify(ModifyOrderCommand command) {
        retry(3, () -> {
            OrderEntity order = repository.getReferenceById(command.orderId());
            order.setQuantity(command.quantity());
            order.setPrice(command.limitPrice());
            repository.save(order);
            cache(order);
            publish(order.getId(), "MODIFIED");
        });
    }

    public void cancel(UUID orderId) {
        retry(3, () -> {
            OrderEntity order = repository.getReferenceById(orderId);
            order.setState(OrderState.CANCELLED);
            repository.save(order);
            cache(order);
            publish(order.getId(), "CANCELLED");
        });
    }

    public void advanceState(UUID orderId) {
        OrderEntity order = repository.getReferenceById(orderId);
        OrderState next = flow.getOrDefault(order.getState(), order.getState());
        order.setState(next);
        repository.save(order);
        cache(order);
        publish(order.getId(), next.name());
    }

    private void cache(OrderEntity order) {
        redisTemplate.opsForValue().set("hot-order:" + order.getId(), order.getState().name(), Duration.ofMinutes(30));
    }

    private void publish(UUID orderId, String action) {
        kafkaTemplate.send("order-events", orderId.toString(), new OrderEvent(orderId, action));
    }

    private static void retry(int maxAttempt, Runnable operation) {
        long delayMs = 25;
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempt; attempt++) {
            try {
                operation.run();
                return;
            } catch (RuntimeException ex) {
                last = ex;
                sleep(delayMs);
                delayMs *= 2;
            }
        }
        throw last == null ? new IllegalStateException("Retry failed") : last;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}

enum OrderState { NEW, PENDING_ACK, PARTIALLY_FILLED, FILLED, CANCELLED, REJECTED }

record ModifyOrderCommand(UUID orderId, long quantity, BigDecimal limitPrice) {}
record OrderEvent(UUID orderId, String eventType) {}
