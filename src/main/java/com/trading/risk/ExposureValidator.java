package com.trading.risk;

/**
 * Exposure Limit Validator - Checks total exposure across all positions
 */
public class ExposureValidator implements RiskValidator {
    private final double maxExposurePercentOfEquity = 0.80; // Max 80% of equity

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        long currentGross = account.getGrossExposure();
        long orderValue = order.getQuantity() * (long)(order.getPrice() * 10000);
        long newGross = currentGross + orderValue;

        // Check absolute gross exposure limit
        long grossLimit = account.getGrossExposureLimit();
        if (grossLimit > 0 && newGross > grossLimit) {
            return RiskCheckResult.fail(RiskCheckType.EXPOSURE_LIMIT,
                    String.format("Gross exposure limit exceeded. Current=%d, New=%d, Limit=%d",
                            currentGross, newGross, grossLimit));
        }

        // Check exposure as percentage of equity (for accounts with credit)
        double equity = account.getCreditLimit() - account.getUsedCredit();
        if (equity > 0 && newGross > equity * maxExposurePercentOfEquity) {
            return RiskCheckResult.fail(RiskCheckType.EXPOSURE_LIMIT,
                    String.format("Exposure exceeds %.0f%% of equity. Equity=%.2f, Current exposure=%d, New=%d",
                            maxExposurePercentOfEquity * 100, equity, currentGross, newGross));
        }

        return RiskCheckResult.pass(RiskCheckType.EXPOSURE_LIMIT);
    }

    @Override
    public int getPriority() { return 20; }
}
