package com.civicpulse.modules.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent<T> {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    private String eventType;

    private String aggregateType;

    private String aggregateId;

    @Builder.Default
    private String source = "civicpulse-backend";

    @Builder.Default
    private String version = "1.0";

    @Builder.Default
    private Instant timestamp = Instant.now();

    private T payload;

    public static <T> DomainEvent<T> of(String eventType, String aggregateType, String aggregateId, T payload) {
        return DomainEvent.<T>builder()
                .eventId(UUID.randomUUID())
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .timestamp(Instant.now())
                .build();
    }
}
