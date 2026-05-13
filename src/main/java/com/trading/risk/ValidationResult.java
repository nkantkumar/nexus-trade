package com.trading.risk;

import java.util.List; /**
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
