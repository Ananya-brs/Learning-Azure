package com.example.employeemanagement.event;

import com.example.employeemanagement.model.Employee;

import java.time.Instant;

public record EmployeeEvent(
        Long id,
        String name,
        String department,
        String email,
        String eventType,
        Instant timestamp
) {

    public static final String EMPLOYEE_CREATED = "EMPLOYEE_CREATED";
    public static final String EMPLOYEE_UPDATED = "EMPLOYEE_UPDATED";
    public static final String EMPLOYEE_DELETED = "EMPLOYEE_DELETED";

    public static EmployeeEvent of(Employee employee, String eventType) {
        return new EmployeeEvent(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getEmail(),
                eventType,
                Instant.now()
        );
    }

    public static EmployeeEvent deleted(Employee employee) {
        return of(employee, EMPLOYEE_DELETED);
    }
}
