package com.trading.matching;

import java.util.ArrayList;
import java.util.List;

public final class OrderValidator {

    public ValidationResult validate(Order order) {
        List<String> errors = new ArrayList<>();
        if (order == null) {
            return ValidationResult.failure("order is null");
        }
        if (order.getPrice() <= 0) {
            errors.add("price must be positive");
        }
        if (order.getQuantity() <= 0) {
            errors.add("quantity must be positive");
        }
        if (order.getOrderId() == null || order.getOrderId().isBlank()) {
            errors.add("orderId required");
        }
        if (order.getSymbol() == null || order.getSymbol().isBlank()) {
            errors.add("symbol required");
        }
        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.failures(errors);
    }
}
