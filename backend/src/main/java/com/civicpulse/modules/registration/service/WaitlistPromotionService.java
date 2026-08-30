package com.civicpulse.modules.registration.service;

import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.registration.dto.RegistrationDto;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.model.WaitlistEntry;
import com.civicpulse.modules.registration.model.WaitlistStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.registration.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitlistPromotionService {

    private final WaitlistRepository waitlistRepository;
    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public void promoteNextCandidate(Event event) {
        List<WaitlistEntry> pendingEntries = waitlistRepository.findByEventIdAndStatusOrderByPositionAsc(
                event.getId(), WaitlistStatus.PENDING
        );

        if (pendingEntries.isEmpty()) {
            // No waitlist candidates available -> decrement confirmed count
            log.info("No waitlist candidates for event [{}]. Decrementing registration count.", event.getTitle());
            event.decrementRegistrationCount();
            eventRepository.save(event);
            return;
        }

        // Promote first in FIFO order
        WaitlistEntry topCandidate = pendingEntries.get(0);
        topCandidate.setStatus(WaitlistStatus.PROMOTED);
        topCandidate.setPromotedAt(Instant.now());
        waitlistRepository.save(topCandidate);

        String ticketCode = generateTicketCode(event.getSlug());
        EventRegistration promotedRegistration = EventRegistration.builder()
                .event(event)
                .user(topCandidate.getUser())
                .ticketCode(ticketCode)
                .status(RegistrationStatus.CONFIRMED)
                .build();

        EventRegistration savedReg = registrationRepository.save(promotedRegistration);
        log.info("Promoted user [{}] from waitlist to confirmed registration [{}] for event [{}]",
                topCandidate.getUser().getEmail(), ticketCode, event.getTitle());

        // Re-index remaining pending entries
        for (int i = 1; i < pendingEntries.size(); i++) {
            WaitlistEntry entry = pendingEntries.get(i);
            entry.setPosition(i); // New 1-based position
            waitlistRepository.save(entry);
        }

        // Publish Kafka Domain Event for real-time notification
        DomainEvent<RegistrationDto> domainEvent = DomainEvent.of(
                "WAITLIST_PROMOTED", "REGISTRATION", savedReg.getId().toString(), RegistrationDto.fromEntity(savedReg)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_NOTIFICATIONS, savedReg.getId().toString(), domainEvent);
    }

    private String generateTicketCode(String eventSlug) {
        String prefix = eventSlug != null && !eventSlug.isBlank() ? eventSlug.toUpperCase().replaceAll("[^A-Z0-9]", "") : "EVENT";
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        int randNum = 100000 + RANDOM.nextInt(900000);
        return String.format("CP-%s-%d", prefix, randNum);
    }
}
