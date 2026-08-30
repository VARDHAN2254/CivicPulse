package com.civicpulse.modules.messaging.consumer;

import com.civicpulse.modules.audit.service.AuditLogService;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventLifecycleConsumerTest {

    @Mock
    private AuditLogService auditLogService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private EventLifecycleConsumer consumer;

    private String sampleJson;
    private UUID sampleUserId;
    private UUID sampleEventId;

    @BeforeEach
    void setUp() throws Exception {
        sampleUserId = UUID.randomUUID();
        sampleEventId = UUID.randomUUID();

        EventDto eventDto = EventDto.builder()
                .id(sampleEventId)
                .title("Clean Rivers Community Clean-Up")
                .createdByUserId(sampleUserId)
                .build();

        DomainEvent<EventDto> domainEvent = DomainEvent.of(
                "EVENT_PUBLISHED", "EVENT", sampleEventId.toString(), eventDto
        );

        sampleJson = objectMapper.writeValueAsString(domainEvent);
    }

    @Test
    @DisplayName("Should successfully consume event lifecycle message, record audit, and acknowledge")
    void handleEventLifecycle_Success() {
        consumer.handleEventLifecycle(sampleJson, sampleEventId.toString(), acknowledgment);

        verify(auditLogService, times(1)).record(
                eq(sampleUserId),
                eq("EVENT_PUBLISHED"),
                eq("EVENT"),
                eq(sampleEventId.toString()),
                anyString(),
                eq("SYSTEM_KAFKA")
        );

        verify(acknowledgment, times(1)).acknowledge();
    }
}
