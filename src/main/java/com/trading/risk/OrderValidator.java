package com.trading.risk;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates basic order fields before risk checks
 */
public class OrderValidator {
    private final RiskEngine riskEngine;

    // Validation rules
    private static final long MAX_QUANTITY = 10_000_000; // 10 million shares max
    private static final double MIN_PRICE = 0.0001;
    private static final double MAX_PRICE = 1_000_000.00;
    private static final int MAX_SYMBOL_LENGTH = 20;
    private static final Set<String> RESTRICTED_SYMBOLS = Set.of("XXX", "YYY", "ZZZ");

    public OrderValidator(RiskEngine riskEngine) {
        this.riskEngine = riskEngine;
    }

    public ValidationResult validate(OrderRequest order) {
        List<String> errors = new ArrayList<>();

        // Check symbol
        if (order.getSymbol() == null || order.getSymbol().trim().isEmpty()) {
            errors.add("Symbol is required");
        } else if (order.getSymbol().length() > MAX_SYMBOL_LENGTH) {
            errors.add("Symbol too long (max " + MAX_SYMBOL_LENGTH + " characters)");
        } else if (RESTRICTED_SYMBOLS.contains(order.getSymbol())) {
            errors.add("Trading restricted for symbol: " + order.getSymbol());
        }

        // Check quantity
        if (order.getQuantity() <= 0) {
            errors.add("Quantity must be positive");
        } else if (order.getQuantity() > MAX_QUANTITY) {
            errors.add("Quantity exceeds maximum allowed (" + MAX_QUANTITY + ")");
        }

        // Check price based on order type
        if (order.getOrderType().isLimitOrder()) {
            if (order.getPrice() <= 0) {
                errors.add("Limit price must be positive");
            } else if (order.getPrice() < MIN_PRICE) {
                errors.add("Price too low (min " + MIN_PRICE + ")");
            } else if (order.getPrice() > MAX_PRICE) {
                errors.add("Price too high (max " + MAX_PRICE + ")");
            }
        }

        // Check stop price for stop orders
        if (order.getOrderType().isStopOrder()) {
            if (order.getStopPrice() == null || order.getStopPrice() <= 0) {
                errors.add("Stop price required and must be positive");
            }
        }

        // Check account ID
        if (order.getAccountId() == null || order.getAccountId().trim().isEmpty()) {
            errors.add("Account ID is required");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }
}

/**
 * Validation result
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;

    public ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public String getErrorsAsString() { return String.join(", ", errors); }
}

/**
 * Order submission result
 */
public class OrderSubmissionResult {
    private final boolean accepted;
    private final String orderId;
    private final String message;
    private final long timestamp;

    private OrderSubmissionResult(boolean accepted, String orderId, String message) {
        this.accepted = accepted;
        this.orderId = orderId;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public static OrderSubmissionResult accepted(String orderId) {
        return new OrderSubmissionResult(true, orderId, "Order accepted");
    }

    public static OrderSubmissionResult rejected(String reason) {
        return new OrderSubmissionResult(false, null, reason);
    }

    public boolean isAccepted() { return accepted; }
    public String getOrderId() { return orderId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
