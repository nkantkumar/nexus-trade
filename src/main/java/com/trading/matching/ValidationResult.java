package com.trading.matching;

import java.util.Collections;
import java.util.List;

public record ValidationResult(boolean valid, List<String> errors) {

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, List.of(message));
    }

    public static ValidationResult failures(List<String> errors) {
        return new ValidationResult(false, Collections.unmodifiableList(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
