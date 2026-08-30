package com.civicpulse.modules.messaging.consumer;

import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.audit.service.AuditLogService;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.registration.dto.RegistrationDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegistrationEventConsumer {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_REGISTRATIONS,
            groupId = "civicpulse-registration-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRegistrationEvent(
            @Payload String messagePayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("Received Kafka Registration Stream message with key: {}", key);
            DomainEvent<RegistrationDto> event = objectMapper.readValue(messagePayload, new TypeReference<>() {});

            RegistrationDto payload = event.getPayload();
            String eventType = event.getEventType();

            log.info("Processing registration event [{}] for registration ID: {} (Ticket: {})",
                    eventType, event.getAggregateId(), payload != null ? payload.getTicketCode() : "N/A");

            auditLogService.record(
                    payload != null ? payload.getUserId() : null,
                    eventType,
                    "REGISTRATION",
                    event.getAggregateId(),
                    String.format("Registration action: %s on event: %s", eventType, payload != null ? payload.getEventTitle() : event.getAggregateId()),
                    "SYSTEM_KAFKA"
            );

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process registration stream message: {}", messagePayload, e);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
}
