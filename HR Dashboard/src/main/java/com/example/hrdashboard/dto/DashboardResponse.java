package com.example.hrdashboard.dto;

import com.example.hrdashboard.model.EmployeeView;

import java.util.List;

public record DashboardResponse(
        List<EmployeeView> employees,
        int employeeCount,
        EmployeeEvent lastEvent
) {
}
