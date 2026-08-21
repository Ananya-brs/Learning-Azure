package com.example.employeemanagement.event;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.example.employeemanagement.model.Employee;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EmployeeEventPublisher.class);

    private final ObjectMapper objectMapper;
    private final ServiceBusSenderClient senderClient;
    private final String queueName;

    public EmployeeEventPublisher(ObjectMapper objectMapper,
                                  @Value("${app.servicebus.connection-string:}") String connectionString,
                                  @Value("${app.servicebus.queue-name:employee-created-queue}") String queueName) {
        this.objectMapper = objectMapper;
        this.queueName = queueName;
        String trimmedConnectionString = connectionString != null ? connectionString.trim() : "";

        if (StringUtils.hasText(trimmedConnectionString) && !trimmedConnectionString.contains("PASTE_YOUR")) {
            this.senderClient = new ServiceBusClientBuilder()
                    .connectionString(trimmedConnectionString)
                    .sender()
                    .queueName(queueName)
                    .buildClient();
        } else {
            this.senderClient = null;
        }
    }

    @PostConstruct
    void logConfigurationStatus() {
        if (senderClient == null) {
            log.warn("Azure Service Bus NOT configured. Add connection string to application-local.properties or set AZURE_SERVICEBUS_CONNECTION_STRING.");
        } else {
            log.info("Azure Service Bus configured. Events will be published to queue: {}", queueName);
        }
    }

    public void publish(Employee employee, String eventType) {
        if (senderClient == null) {
            log.warn("Service Bus is not configured. Skipping {} event for employee id={}", eventType, employee.getId());
            return;
        }

        try {
            EmployeeEvent event = EmployeeEvent.of(employee, eventType);
            String payload = objectMapper.writeValueAsString(event);
            senderClient.sendMessage(new ServiceBusMessage(payload));
            log.info("Published {} event for employee id={} to queue {}", eventType, employee.getId(), queueName);
        } catch (JsonProcessingException ex) {
            log.error("Failed to serialize employee event for id={}", employee.getId(), ex);
        } catch (Exception ex) {
            log.error("Failed to publish employee event for id={}", employee.getId(), ex);
        }
    }

    @PreDestroy
    public void close() {
        if (senderClient != null) {
            senderClient.close();
        }
    }
}
