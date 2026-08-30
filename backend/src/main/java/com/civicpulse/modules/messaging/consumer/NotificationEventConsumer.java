package com.civicpulse.modules.messaging.consumer;

import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.audit.service.AuditLogService;
import com.civicpulse.modules.messaging.event.DomainEvent;
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

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = KafkaConfig.TOPIC_NOTIFICATIONS,
            groupId = "civicpulse-notification-dispatcher",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleNotification(
            @Payload String messagePayload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment
    ) {
        try {
            log.info("Received Kafka Notification message with key: {}", key);
            DomainEvent<Map<String, Object>> event = objectMapper.readValue(messagePayload, new TypeReference<>() {});

            log.info("Dispatched in-app notification for event type [{}] on aggregate: [{}]",
                    event.getEventType(), event.getAggregateId());

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process notification message: {}", messagePayload, e);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        }
    }
}
