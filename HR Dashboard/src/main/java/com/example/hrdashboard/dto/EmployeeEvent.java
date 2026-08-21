package com.example.hrdashboard.dto;

import java.time.Instant;

public record EmployeeEvent(
        Long id,
        String name,
        String department,
        String email,
        String eventType,
        Instant timestamp
) {
}
