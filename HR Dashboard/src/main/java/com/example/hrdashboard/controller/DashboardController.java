package com.example.hrdashboard.controller;

import com.example.hrdashboard.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("employees", dashboardService.getAllEmployees());
        model.addAttribute("employeeCount", dashboardService.getEmployeeCount());
        dashboardService.getLastReceivedEvent().ifPresent(event ->
                model.addAttribute("lastEvent", event)
        );
        return "index";
    }
}
