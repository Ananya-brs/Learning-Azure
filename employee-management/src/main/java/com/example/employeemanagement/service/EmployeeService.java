package com.example.employeemanagement.service;

import com.example.employeemanagement.dto.EmployeeRequest;
import com.example.employeemanagement.event.EmployeeChangedApplicationEvent;
import com.example.employeemanagement.event.EmployeeEvent;
import com.example.employeemanagement.model.Employee;
import com.example.employeemanagement.repository.EmployeeRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EmployeeService(EmployeeRepository employeeRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.employeeRepository = employeeRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Employee createEmployee(EmployeeRequest request) {
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Employee with email already exists: " + request.getEmail());
        }

        Employee employee = new Employee(
                request.getName(),
                request.getDepartment(),
                request.getEmail()
        );

        Employee saved = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeeChangedApplicationEvent(saved, EmployeeEvent.EMPLOYEE_CREATED));
        return saved;
    }

    @Transactional
    public Employee updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        if (!employee.getEmail().equals(request.getEmail())
                && employeeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Employee with email already exists: " + request.getEmail());
        }

        employee.setName(request.getName());
        employee.setDepartment(request.getDepartment());
        employee.setEmail(request.getEmail());

        Employee saved = employeeRepository.save(employee);
        eventPublisher.publishEvent(new EmployeeChangedApplicationEvent(saved, EmployeeEvent.EMPLOYEE_UPDATED));
        return saved;
    }

    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));

        employeeRepository.delete(employee);
        eventPublisher.publishEvent(new EmployeeChangedApplicationEvent(employee, EmployeeEvent.EMPLOYEE_DELETED));
    }

    public List<Employee> findAllEmployees() {
        return employeeRepository.findAll();
    }

    public Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }
}
