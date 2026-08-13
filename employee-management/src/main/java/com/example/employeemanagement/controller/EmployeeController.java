package com.example.employeemanagement.controller;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @ModelAttribute("employeeRequest")
    public EmployeeRequest employeeRequest() {
        return new EmployeeRequest();
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/employees")
    @ResponseBody
    public ResponseEntity<Employee> createEmployeeApi(@Valid @RequestBody EmployeeRequest request) {
        Employee saved = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<Employee> updateEmployeeApi(@PathVariable Long id,
                                                      @Valid @RequestBody EmployeeRequest request) {
        Employee updated = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/employees/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteEmployeeApi(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/employees/create")
    public String createEmployee(@Valid @ModelAttribute("employeeRequest") EmployeeRequest request,
                                 BindingResult bindingResult,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("employeeRequest", request);
            return "index";
        }

        try {
            Employee saved = employeeService.createEmployee(request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Employee \"" + saved.getName() + "\" saved successfully (ID: " + saved.getId() + "). Event published to Service Bus.");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("employeeRequest", request);
            model.addAttribute("errorMessage", ex.getMessage());
            return "index";
        }
    }
}
