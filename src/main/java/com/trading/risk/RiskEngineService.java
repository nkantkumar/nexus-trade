package com.trading.risk;

import io.aeron.Aeron;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {
    private final List<RiskCheck> checks;
    private final Aeron aeron;

    public RiskEngineService(StringRedisTemplate redisTemplate) {
        this.checks = List.of(
                new PositionLimitCheck(redisTemplate),
                new NotionalLimitCheck(),
                new FatFingerCheck(),
                new MarginCheck(),
                new CreditLimitCheck());
        this.aeron = Aeron.connect();
    }

    public RiskResult preTradeCheck(TradeIntent intent) {
        for (RiskCheck check : checks) {
            RiskResult result = check.apply(intent);
            if (!(result instanceof RiskResult.Approved)) {
                return result;
            }
        }
        return new RiskResult.Approved("all checks passed");
    }
}

record TradeIntent(String accountId, String symbol, long quantity, BigDecimal price, BigDecimal vwap, BigDecimal margin) {}

sealed interface RiskResult permits RiskResult.Approved, RiskResult.Rejected, RiskResult.Breach {
    record Approved(String detail) implements RiskResult {}
    record Rejected(String reason) implements RiskResult {}
    record Breach(String severity, String reason) implements RiskResult {}
}

interface RiskCheck { RiskResult apply(TradeIntent intent); }

class PositionLimitCheck implements RiskCheck {
    private final StringRedisTemplate redisTemplate;
    PositionLimitCheck(StringRedisTemplate redisTemplate) { this.redisTemplate = redisTemplate; }
    @Override public RiskResult apply(TradeIntent intent) {
        String raw = redisTemplate.opsForValue().get("pos:" + intent.accountId() + ":" + intent.symbol());
        long current = raw == null ? 0L : Long.parseLong(raw);
        return Math.abs(current + intent.quantity()) > 1_000_000
                ? new RiskResult.Rejected("position limit exceeded")
                : new RiskResult.Approved("position ok");
    }
}
class NotionalLimitCheck implements RiskCheck {
    @Override public RiskResult apply(TradeIntent intent) {
        return intent.price().multiply(BigDecimal.valueOf(intent.quantity())).compareTo(BigDecimal.valueOf(50_000_000)) > 0
                ? new RiskResult.Rejected("notional exposure exceeded")
                : new RiskResult.Approved("notional ok");
    }
}
class FatFingerCheck implements RiskCheck {
    @Override public RiskResult apply(TradeIntent intent) {
        BigDecimal delta = intent.price().subtract(intent.vwap()).abs().divide(intent.vwap(), BigDecimal.ROUND_HALF_UP);
        return delta.compareTo(BigDecimal.valueOf(0.10)) > 0
                ? new RiskResult.Breach("HIGH", "fat finger threshold exceeded")
                : new RiskResult.Approved("fat finger ok");
    }
}
class MarginCheck implements RiskCheck {
    @Override public RiskResult apply(TradeIntent intent) {
        BigDecimal required = intent.price().multiply(BigDecimal.valueOf(intent.quantity())).multiply(BigDecimal.valueOf(0.2));
        return intent.margin().compareTo(required) < 0
                ? new RiskResult.Rejected("insufficient margin")
                : new RiskResult.Approved("margin ok");
    }
}
class CreditLimitCheck implements RiskCheck {
    @Override public RiskResult apply(TradeIntent intent) {
        return intent.price().multiply(BigDecimal.valueOf(intent.quantity())).compareTo(BigDecimal.valueOf(100_000_000)) > 0
                ? new RiskResult.Breach("CRITICAL", "credit limit exceeded")
                : new RiskResult.Approved("credit ok");
    }
}
