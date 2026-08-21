package com.example.hrdashboard.model;

import com.example.hrdashboard.dto.EmployeeEvent;

import java.time.Instant;

public record EmployeeView(
        Long id,
        String name,
        String department,
        String email,
        Instant receivedAt
) {

    public static EmployeeView from(EmployeeEvent event) {
        return new EmployeeView(
                event.id(),
                event.name(),
                event.department(),
                event.email(),
                event.timestamp()
        );
    }
}
