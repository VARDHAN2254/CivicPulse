package com.civicpulse.modules.event.statemachine;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.model.EventStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class EventStateMachineService {

    private static final Map<EventStatus, Set<EventStatus>> VALID_TRANSITIONS = Map.of(
            EventStatus.DRAFT, Set.of(EventStatus.PUBLISHED, EventStatus.CANCELLED),
            EventStatus.PUBLISHED, Set.of(EventStatus.REGISTRATION_OPEN, EventStatus.REGISTRATION_CLOSED, EventStatus.CANCELLED),
            EventStatus.REGISTRATION_OPEN, Set.of(EventStatus.REGISTRATION_CLOSED, EventStatus.IN_PROGRESS, EventStatus.CANCELLED),
            EventStatus.REGISTRATION_CLOSED, Set.of(EventStatus.REGISTRATION_OPEN, EventStatus.IN_PROGRESS, EventStatus.CANCELLED),
            EventStatus.IN_PROGRESS, Set.of(EventStatus.COMPLETED, EventStatus.CANCELLED),
            EventStatus.COMPLETED, Set.of(),
            EventStatus.CANCELLED, Set.of()
    );

    public void validateAndTransition(Event event, EventStatus targetStatus) {
        EventStatus currentStatus = event.getStatus();

        if (currentStatus == targetStatus) {
            return;
        }

        Set<EventStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            log.warn("Illegal event state transition attempt from [{}] to [{}] for event ID: {}", currentStatus, targetStatus, event.getId());
            throw new BadRequestException(
                    String.format("Invalid state transition: Cannot change event from '%s' to '%s'.", currentStatus, targetStatus)
            );
        }

        log.info("Transitioning event ID [{}] from [{}] to [{}]", event.getId(), currentStatus, targetStatus);
        event.setStatus(targetStatus);
    }
}
