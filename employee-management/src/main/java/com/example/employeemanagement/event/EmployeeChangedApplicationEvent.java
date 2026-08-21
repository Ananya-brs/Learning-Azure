package com.example.employeemanagement.event;

import com.example.employeemanagement.model.Employee;
import org.springframework.context.ApplicationEvent;

public class EmployeeChangedApplicationEvent extends ApplicationEvent {

    private final Employee employee;
    private final String eventType;

    public EmployeeChangedApplicationEvent(Employee employee, String eventType) {
        super(employee);
        this.employee = employee;
        this.eventType = eventType;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getEventType() {
        return eventType;
    }
}
