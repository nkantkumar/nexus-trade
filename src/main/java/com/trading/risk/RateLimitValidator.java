package com.trading.risk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong; /**
 * Rate Limit Validator - Prevents order flooding
 */
public class RateLimitValidator implements RiskValidator {
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final int maxOrdersPerSecond = 100;
    private final int maxOrderValuePerSecond = 10_000_000; // $10M

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        String key = account.getAccountId();
        RateLimiter limiter = rateLimiters.computeIfAbsent(key,
                k -> new RateLimiter(maxOrdersPerSecond, maxOrderValuePerSecond));

        long orderValue = order.getQuantity() * (long)(order.getPrice() * 10000);

        if (!limiter.tryAcquire(orderValue)) {
            return RiskCheckResult.fail(RiskCheckType.RATE_LIMIT,
                    String.format("Rate limit exceeded. Orders/sec=%d, Value/sec=%d",
                            limiter.getCurrentOrderCount(), limiter.getCurrentValueCount()));
        }

        return RiskCheckResult.pass(RiskCheckType.RATE_LIMIT);
    }

    @Override
    public int getPriority() { return 2; }

    private static class RateLimiter {
        private final int maxOrdersPerSecond;
        private final long maxValuePerSecond;
        private final AtomicLong orderCount = new AtomicLong(0);
        private final AtomicLong valueCount = new AtomicLong(0);
        private volatile long lastResetTime = System.currentTimeMillis();

        RateLimiter(int maxOrdersPerSecond, long maxValuePerSecond) {
            this.maxOrdersPerSecond = maxOrdersPerSecond;
            this.maxValuePerSecond = maxValuePerSecond;
        }

        synchronized boolean tryAcquire(long orderValue) {
            long now = System.currentTimeMillis();
            if (now - lastResetTime >= 1000) {
                orderCount.set(0);
                valueCount.set(0);
                lastResetTime = now;
            }

            if (orderCount.incrementAndGet() > maxOrdersPerSecond) {
                return false;
            }

            if (valueCount.addAndGet(orderValue) > maxValuePerSecond) {
                return false;
            }

            return true;
        }

        long getCurrentOrderCount() { return orderCount.get(); }
        long getCurrentValueCount() { return valueCount.get(); }
    }
}
