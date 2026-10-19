package com.payment;

import java.util.function.Predicate;

public class EmployeeFilters {

    public static Predicate<Employee> isActive() {
        return e -> e.getStatus() == Employee.EmployeeStatus.ACTIVE;
    }

    public static Predicate<Employee> inDepartment(String department) {
        return e -> e.getDepartment().equalsIgnoreCase(department);
    }

    public static Predicate<Employee> hasMinimumTenure(int years) {
        return e -> e.getYearsOfService() >= years;
    }

    public static Predicate<Employee> isEligibleForPromotion() {
        return isActive()
                .and(hasMinimumTenure(2))
                .negate().negate(); // composable — real logic can be as complex as needed
    }
}
