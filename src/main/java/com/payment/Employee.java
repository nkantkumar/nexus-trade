package com.payment;

public class Employee {
    private final String id;
    private final String name;
    private final String department;
    private final double baseSalary;
    private final int yearsOfService;
    private final EmployeeStatus status;
    private final String managerId;

    public Employee(String id, String name, String department, double baseSalary,
                    int yearsOfService, EmployeeStatus status, String managerId) {
        this.id = id; this.name = name; this.department = department;
        this.baseSalary = baseSalary; this.yearsOfService = yearsOfService;
        this.status = status; this.managerId = managerId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public double getBaseSalary() { return baseSalary; }
    public int getYearsOfService() { return yearsOfService; }
    public EmployeeStatus getStatus() { return status; }
    public String getManagerId() { return managerId; }

    public enum EmployeeStatus { ACTIVE, ON_LEAVE, TERMINATED, PROBATION }
}
