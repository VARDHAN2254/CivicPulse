package com.civicpulse.modules.registration.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.registration.dto.AttendeeDto;
import com.civicpulse.modules.registration.dto.RegistrationDto;
import com.civicpulse.modules.registration.dto.RegistrationResponse;
import com.civicpulse.modules.registration.dto.WaitlistEntryDto;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.model.WaitlistEntry;
import com.civicpulse.modules.registration.model.WaitlistStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.registration.repository.WaitlistRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final WaitlistRepository waitlistRepository;
    private final UserRepository userRepository;
    private final WaitlistPromotionService waitlistPromotionService;
    private final KafkaEventPublisher kafkaEventPublisher;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public RegistrationResponse registerForEvent(UUID userId, UUID eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Acquire Pessimistic DB Write Lock on event record to prevent race conditions & overselling
        Event event = eventRepository.findByIdWithPessimisticLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        // Validate event lifecycle and deadline
        event.validateAcceptingRegistrations();

        // Check if user is already confirmed
        Optional<EventRegistration> existingReg = registrationRepository.findByEventIdAndUserId(eventId, userId);
        if (existingReg.isPresent() && existingReg.get().getStatus() == RegistrationStatus.CONFIRMED) {
            throw new ConflictException("You are already registered for this event.");
        }

        // Check if user is already waitlisted
        Optional<WaitlistEntry> existingWaitlist = waitlistRepository.findByEventIdAndUserId(eventId, userId);
        if (existingWaitlist.isPresent() && existingWaitlist.get().getStatus() == WaitlistStatus.PENDING) {
            throw new ConflictException(
                    String.format("You are already on the waitlist for this event at position #%d.",
                            existingWaitlist.get().getPosition())
            );
        }

        // CASE 1: Event has available capacity
        if (event.hasAvailableCapacity()) {
            event.incrementRegistrationCount();
            eventRepository.save(event);

            String ticketCode = generateTicketCode(event.getSlug());
            EventRegistration registration = EventRegistration.builder()
                    .event(event)
                    .user(user)
                    .ticketCode(ticketCode)
                    .status(RegistrationStatus.CONFIRMED)
                    .build();

            EventRegistration savedReg = registrationRepository.save(registration);
            log.info("User [{}] registered for event [{}] with ticket [{}]", user.getEmail(), event.getTitle(), ticketCode);

            // Kafka Event for notifications & analytics
            DomainEvent<RegistrationDto> domainEvent = DomainEvent.of(
                    "REGISTRATION_CREATED", "REGISTRATION", savedReg.getId().toString(), RegistrationDto.fromEntity(savedReg)
            );
            kafkaEventPublisher.publish(KafkaConfig.TOPIC_REGISTRATIONS, savedReg.getId().toString(), domainEvent);

            return RegistrationResponse.builder()
                    .confirmed(true)
                    .waitlisted(false)
                    .ticketCode(ticketCode)
                    .message("Registration confirmed! Your ticket pass is ready.")
                    .registration(RegistrationDto.fromEntity(savedReg))
                    .build();
        }

        // CASE 2: Event is full -> Waitlist evaluation
        if (!event.isWaitlistEnabled()) {
            throw new BadRequestException("Event is at maximum capacity and waitlist is disabled.");
        }

        long currentWaitlistCount = waitlistRepository.countByEventIdAndStatus(eventId, WaitlistStatus.PENDING);
        if (currentWaitlistCount >= event.getWaitlistCapacity()) {
            throw new BadRequestException("Event and automated waitlist are both at full capacity.");
        }

        int nextPosition = waitlistRepository.findMaxPositionByEventId(eventId) + 1;
        WaitlistEntry waitlistEntry = WaitlistEntry.builder()
                .event(event)
                .user(user)
                .position(nextPosition)
                .status(WaitlistStatus.PENDING)
                .build();

        WaitlistEntry savedWaitlist = waitlistRepository.save(waitlistEntry);
        log.info("User [{}] joined waitlist for event [{}] at position #{}", user.getEmail(), event.getTitle(), nextPosition);

        DomainEvent<WaitlistEntryDto> domainEvent = DomainEvent.of(
                "WAITLIST_JOINED", "WAITLIST", savedWaitlist.getId().toString(), WaitlistEntryDto.fromEntity(savedWaitlist)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_REGISTRATIONS, savedWaitlist.getId().toString(), domainEvent);

        return RegistrationResponse.builder()
                .confirmed(false)
                .waitlisted(true)
                .waitlistPosition(nextPosition)
                .message(String.format("Event is full. You joined the automated waitlist at position #%d.", nextPosition))
                .waitlistEntry(WaitlistEntryDto.fromEntity(savedWaitlist))
                .build();
    }

    @Transactional
    public void cancelRegistration(UUID userId, UUID registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if (!registration.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only cancel your own registrations.");
        }

        if (registration.getStatus() != RegistrationStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed registrations can be cancelled.");
        }

        // Acquire lock on event before modifying capacity & promoting waitlist
        Event event = eventRepository.findByIdWithPessimisticLock(registration.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(Instant.now());
        registrationRepository.save(registration);
        log.info("Cancelled registration [{}] for user [{}] on event [{}]",
                registration.getTicketCode(), registration.getUser().getEmail(), event.getTitle());

        // Trigger automated FIFO waitlist promotion
        waitlistPromotionService.promoteNextCandidate(event);

        DomainEvent<RegistrationDto> domainEvent = DomainEvent.of(
                "REGISTRATION_CANCELLED", "REGISTRATION", registration.getId().toString(), RegistrationDto.fromEntity(registration)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_REGISTRATIONS, registration.getId().toString(), domainEvent);
    }

    @Transactional(readOnly = true)
    public List<RegistrationDto> getUserRegistrations(UUID userId) {
        return registrationRepository.findAllByUserIdWithEvent(userId)
                .stream()
                .map(RegistrationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WaitlistEntryDto> getUserWaitlistEntries(UUID userId) {
        return waitlistRepository.findAllByUserIdWithEvent(userId)
                .stream()
                .map(WaitlistEntryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RegistrationDto getRegistrationByTicketCode(String ticketCode) {
        EventRegistration reg = registrationRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket pass not found: " + ticketCode));
        return RegistrationDto.fromEntity(reg);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AttendeeDto> getEventAttendees(UUID eventId, Pageable pageable) {
        Page<EventRegistration> regPage = registrationRepository.findByEventIdWithUser(eventId, pageable);

        Page<AttendeeDto> attendeePage = regPage.map(reg -> AttendeeDto.builder()
                .registrationId(reg.getId())
                .userId(reg.getUser().getId())
                .fullName(reg.getUser().getFullName())
                .email(reg.getUser().getEmail())
                .ticketCode(reg.getTicketCode())
                .status(reg.getStatus())
                .registeredAt(reg.getRegisteredAt())
                .build());

        return PagedResponse.from(attendeePage);
    }

    private String generateTicketCode(String eventSlug) {
        String prefix = eventSlug != null && !eventSlug.isBlank() ? eventSlug.toUpperCase().replaceAll("[^A-Z0-9]", "") : "EVENT";
        if (prefix.length() > 6) prefix = prefix.substring(0, 6);
        int randNum = 100000 + RANDOM.nextInt(900000);
        return String.format("CP-%s-%d", prefix, randNum);
    }
}
