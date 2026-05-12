package com.trading.risk;


/**
 * Credit Validator - Now properly receives CreditManager via constructor
 */
public class CreditValidator implements RiskValidator {
    private final CreditManager creditManager;

    // Constructor injection
    public CreditValidator(CreditManager creditManager) {
        this.creditManager = creditManager;
    }

    @Override
    public RiskCheckResult validate(OrderRequest order, RiskAccount account, RiskContext context) {
        // 1. Check if account has sufficient credit
        double availableCredit = creditManager.getAvailableCredit(account.getAccountId());
        double orderCost = order.getQuantity() * order.getPrice();

        if (orderCost > availableCredit) {
            return RiskCheckResult.fail(RiskCheckType.CREDIT,
                    String.format("Insufficient credit. Required=%.2f, Available=%.2f",
                            orderCost, availableCredit));
        }

        // 2. Check credit rating for large orders
        if (orderCost > account.getCreditLimit() * 0.20) { // Order > 20% of credit line
            CreditRating rating = creditManager.getCreditRating(account.getAccountId());
            if (rating == CreditRating.POOR || rating == CreditRating.LIMITED) {
                return RiskCheckResult.fail(RiskCheckType.CREDIT,
                        String.format("Large order rejected for credit rating %s", rating));
            }
        }

        // 3. For market makers, check if they have good standing
        if (account.getAccountType() == AccountType.MARKET_MAKER) {
            if (!creditManager.isMarketMakerInGoodStanding(account.getAccountId())) {
                return RiskCheckResult.fail(RiskCheckType.CREDIT,
                        "Market maker not in good standing");
            }
        }

        // 4. Reserve credit for this order (if passed)
        if (creditManager.reserveCredit(account.getAccountId(), orderCost)) {
            return RiskCheckResult.pass(RiskCheckType.CREDIT);
        } else {
            return RiskCheckResult.fail(RiskCheckType.CREDIT,
                    "Failed to reserve credit");
        }
    }

    @Override
    public int getPriority() { return 40; }
}
