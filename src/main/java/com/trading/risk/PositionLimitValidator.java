package com.trading.risk;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Position Limit Validator - Checks position limits per symbol
 */
public class PositionLimitValidator implements RiskValidator {
    private static final Logger logger = LoggerFactory.getLogger(PositionLimitValidator.class);

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // Get current position for this symbol
        Position currentPosition = account.getPosition(order.getSymbol());
        long currentQuantity = currentPosition.getQuantity();

        // Calculate new position after order
        long newQuantity = order.getSide() == OrderSide.BUY
                ? currentQuantity + order.getQuantity()
                : currentQuantity - order.getQuantity();

        // Check absolute position limit
        long positionLimit = getPositionLimit(account, order.getSymbol());
        if (Math.abs(newQuantity) > positionLimit) {
            return RiskCheckResult.fail(RiskCheckType.POSITION_LIMIT,
                    String.format("Position limit exceeded. Symbol=%s, Current=%d, New=%d, Limit=%d",
                            order.getSymbol(), currentQuantity, newQuantity, positionLimit));
        }

        // Check net position limits for the account
        long netLimit = account.getNetExposureLimit();
        if (netLimit > 0) {
            long currentNet = account.getNetExposure().get();
            long orderValue = order.getQuantity() * (long)(order.getPrice() * 10000);
            long newNet = order.getSide() == OrderSide.BUY ? currentNet + orderValue : currentNet - orderValue;

            if (Math.abs(newNet) > netLimit) {
                return RiskCheckResult.fail(RiskCheckType.POSITION_LIMIT,
                        String.format("Net position limit exceeded. Current=%d, New=%d, Limit=%d",
                                currentNet, newNet, netLimit));
            }
        }

        return RiskCheckResult.pass(RiskCheckType.POSITION_LIMIT);
    }

    private long getPositionLimit(RiskAccount account, String symbol) {
        // Check symbol-specific limit first
        ProductLimit productLimit = account.getProductLimits().get(symbol);
        if (productLimit != null && productLimit.getPositionLimit() > 0) {
            return productLimit.getPositionLimit();
        }
        // Fall back to account default
        return account.getPositionLimit();
    }

    @Override
    public int getPriority() { return 10; }
}
