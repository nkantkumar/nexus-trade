package com.trading.risk;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {
    private final List<RiskCheck> checks;

    public RiskEngineService(StringRedisTemplate redisTemplate) {
        this.checks = List.of(
                new PositionLimitCheck(redisTemplate),
                new NotionalLimitCheck(),
                new FatFingerCheck(),
                new MarginCheck(),
                new CreditLimitCheck());
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
        if (intent.vwap().signum() == 0) {
            return new RiskResult.Approved("fat finger ok");
        }
        BigDecimal delta = intent.price()
                .subtract(intent.vwap())
                .abs()
                .divide(intent.vwap(), 12, RoundingMode.HALF_UP);
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
