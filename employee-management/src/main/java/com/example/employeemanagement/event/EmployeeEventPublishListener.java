package com.example.employeemanagement.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmployeeEventPublishListener {

    private final EmployeeEventPublisher employeeEventPublisher;

    public EmployeeEventPublishListener(EmployeeEventPublisher employeeEventPublisher) {
        this.employeeEventPublisher = employeeEventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmployeeChanged(EmployeeChangedApplicationEvent event) {
        employeeEventPublisher.publish(event.getEmployee(), event.getEventType());
    }
}
