package com.example.hrdashboard.consumer;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.example.hrdashboard.dto.EmployeeEvent;
import com.example.hrdashboard.service.EmployeeSyncService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class EmployeeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmployeeEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final EmployeeSyncService employeeSyncService;
    private final String queueName;
    private ServiceBusProcessorClient processorClient;

    public EmployeeEventConsumer(ObjectMapper objectMapper,
                                 EmployeeSyncService employeeSyncService,
                                 @Value("${app.servicebus.connection-string:}") String connectionString,
                                 @Value("${app.servicebus.queue-name:employee-created-queue}") String queueName) {
        this.objectMapper = objectMapper;
        this.employeeSyncService = employeeSyncService;
        this.queueName = queueName;

        String trimmedConnectionString = connectionString != null ? connectionString.trim() : "";
        if (StringUtils.hasText(trimmedConnectionString) && !trimmedConnectionString.contains("PASTE_YOUR")) {
            this.processorClient = new ServiceBusClientBuilder()
                    .connectionString(trimmedConnectionString)
                    .processor()
                    .queueName(queueName)
                    .disableAutoComplete()
                    .processMessage(this::processMessage)
                    .processError(this::processError)
                    .buildProcessorClient();
        }
    }

    @PostConstruct
    void start() {
        if (processorClient == null) {
            log.warn("Azure Service Bus NOT configured. Add connection string to application-local.properties or set AZURE_SERVICEBUS_CONNECTION_STRING.");
            return;
        }

        processorClient.start();
        log.info("Listening on Azure Service Bus queue: {}", queueName);
    }

    private void processMessage(ServiceBusReceivedMessageContext context) {
        ServiceBusReceivedMessage message = context.getMessage();
        String body = message.getBody().toString();

        try {
            EmployeeEvent event = objectMapper.readValue(body, EmployeeEvent.class);
            employeeSyncService.syncFromEvent(event);
            log.info("Received {} from Service Bus: {} (id={})",
                    event.eventType(), event.name(), event.id());
            context.complete();
        } catch (Exception ex) {
            log.error("Failed to process Service Bus message. Message id={}", message.getMessageId(), ex);
            context.abandon();
        }
    }

    private void processError(ServiceBusErrorContext context) {
        Throwable ex = context.getException();
        if (ex instanceof ServiceBusException serviceBusException) {
            log.error("Service Bus error. entity={}, errorSource={}",
                    context.getEntityPath(),
                    context.getErrorSource(),
                    serviceBusException);
        } else {
            log.error("Service Bus error. entity={}, errorSource={}",
                    context.getEntityPath(),
                    context.getErrorSource(),
                    ex);
        }
    }

    @PreDestroy
    void stop() {
        if (processorClient != null) {
            processorClient.close();
        }
    }
}
