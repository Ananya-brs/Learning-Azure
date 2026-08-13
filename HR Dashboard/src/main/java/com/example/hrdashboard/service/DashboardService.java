package com.example.hrdashboard.service;

import com.example.hrdashboard.dto.EmployeeEvent;
import com.example.hrdashboard.model.EmployeeView;
import com.example.hrdashboard.store.EmployeeEventStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private final EmployeeEventStore employeeEventStore;

    public DashboardService(EmployeeEventStore employeeEventStore) {
        this.employeeEventStore = employeeEventStore;
    }

    public List<EmployeeView> getAllEmployees() {
        return employeeEventStore.findAll();
    }

    public Optional<EmployeeEvent> getLastReceivedEvent() {
        return employeeEventStore.getLastEvent();
    }

    public int getEmployeeCount() {
        return employeeEventStore.count();
    }
}
