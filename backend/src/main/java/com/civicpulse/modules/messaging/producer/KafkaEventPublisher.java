package com.civicpulse.modules.messaging.producer;

import com.civicpulse.modules.messaging.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public <T> void publish(String topic, String key, DomainEvent<T> event) {
        log.info("Publishing domain event [{}] with key [{}] to topic [{}]", event.getEventType(), key, topic);
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event [{}] to topic [{}]: {}", event.getEventType(), topic, ex.getMessage());
                        } else {
                            log.debug("Successfully published event [{}] at offset [{}]", event.getEventType(), result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception ex) {
            log.error("Error initiating Kafka publish for event [{}]: {}", event.getEventType(), ex.getMessage());
        }
    }
}
