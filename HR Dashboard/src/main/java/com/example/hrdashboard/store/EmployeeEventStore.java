package com.example.hrdashboard.store;

import com.example.hrdashboard.dto.EmployeeEvent;
import com.example.hrdashboard.model.EmployeeView;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmployeeEventStore {

    private volatile EmployeeEvent lastEvent;
    private final ConcurrentHashMap<Long, EmployeeView> employees = new ConcurrentHashMap<>();

    public void addOrUpdate(EmployeeEvent event) {
        lastEvent = event;
        employees.put(event.id(), EmployeeView.from(event));
    }

    public void remove(Long employeeId) {
        employees.remove(employeeId);
    }

    public void recordLastEvent(EmployeeEvent event) {
        lastEvent = event;
    }

    public List<EmployeeView> findAll() {
        return employees.values().stream()
                .sorted(Comparator.comparing(EmployeeView::id))
                .toList();
    }

    public Optional<EmployeeEvent> getLastEvent() {
        return Optional.ofNullable(lastEvent);
    }

    public int count() {
        return employees.size();
    }
}
