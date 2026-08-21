package com.example.hrdashboard.service;

import com.example.hrdashboard.dto.EmployeeEvent;
import com.example.hrdashboard.store.EmployeeEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmployeeSyncService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeSyncService.class);

    private final EmployeeEventStore employeeEventStore;

    public EmployeeSyncService(EmployeeEventStore employeeEventStore) {
        this.employeeEventStore = employeeEventStore;
    }

    public void syncFromEvent(EmployeeEvent event) {
        switch (event.eventType()) {
            case "EMPLOYEE_CREATED", "EMPLOYEE_UPDATED" -> {
                employeeEventStore.addOrUpdate(event);
                log.info("Stored {} from Service Bus: {} (id={})", event.eventType(), event.name(), event.id());
            }
            case "EMPLOYEE_DELETED" -> {
                employeeEventStore.remove(event.id());
                employeeEventStore.recordLastEvent(event);
                log.info("Removed EMPLOYEE_DELETED from Service Bus: id={}", event.id());
            }
            default -> log.warn("Unknown event type: {}", event.eventType());
        }
    }
}
