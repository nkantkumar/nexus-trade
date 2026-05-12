package com.trading.risk;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque; /**
 * Duplicate Order Validator - Prevents duplicate order submissions
 */
public class DuplicateOrderValidator implements RiskValidator {
    private final Map<String, Deque<OrderRequest>> recentOrders = new ConcurrentHashMap<>();
    private final long duplicateWindowMs = 100; // 100ms window

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        String key = account.getAccountId() + ":" + order.getSymbol();
        Deque<OrderRequest> orders = recentOrders.computeIfAbsent(key,
                k -> new ConcurrentLinkedDeque<>());

        long now = System.currentTimeMillis();

        // Clean old entries
        while (!orders.isEmpty() && orders.peekFirst().getTimestamp() < now - duplicateWindowMs) {
            orders.pollFirst();
        }

        // Check for duplicates
        for (OrderRequest existing : orders) {
            if (existing.getSide() == order.getSide() &&
                    existing.getPrice() == order.getPrice() &&
                    existing.getQuantity() == order.getQuantity()) {
                return RiskCheckResult.fail(RiskCheckType.DUPLICATE_ORDER,
                        String.format("Duplicate order detected within %d ms", duplicateWindowMs));
            }
        }

        orders.addLast(order);
        return RiskCheckResult.pass(RiskCheckType.DUPLICATE_ORDER);
    }

    @Override
    public int getPriority() { return 1; } // Highest priority
}
