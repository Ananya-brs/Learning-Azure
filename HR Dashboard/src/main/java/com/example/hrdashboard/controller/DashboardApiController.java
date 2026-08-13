package com.example.hrdashboard.controller;

import com.example.hrdashboard.dto.DashboardResponse;
import com.example.hrdashboard.dto.EmployeeEvent;
import com.example.hrdashboard.model.EmployeeView;
import com.example.hrdashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardApiController {

    private final DashboardService dashboardService;

    public DashboardApiController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        List<EmployeeView> employees = dashboardService.getAllEmployees();
        EmployeeEvent lastEvent = dashboardService.getLastReceivedEvent().orElse(null);
        return new DashboardResponse(employees, dashboardService.getEmployeeCount(), lastEvent);
    }
}
