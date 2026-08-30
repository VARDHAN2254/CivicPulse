package com.civicpulse.modules.notification.service;

import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.notification.dto.AnnouncementDto;
import com.civicpulse.modules.notification.dto.CreateAnnouncementRequest;
import com.civicpulse.modules.notification.model.Announcement;
import com.civicpulse.modules.notification.model.NotificationType;
import com.civicpulse.modules.notification.repository.AnnouncementRepository;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public AnnouncementDto createAnnouncement(UUID organizerUserId, UUID eventId, CreateAnnouncementRequest request) {
        User organizer = userRepository.findById(organizerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Announcement announcement = Announcement.builder()
                .event(event)
                .createdBy(organizer)
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .build();

        Announcement saved = announcementRepository.save(announcement);
        AnnouncementDto dto = AnnouncementDto.fromEntity(saved);

        // 1. Broadcast over Event WebSocket channel
        try {
            String topic = "/topic/events/" + eventId + "/announcements";
            messagingTemplate.convertAndSend(topic, dto);
            log.info("Broadcasted live announcement on event [{}] over [{}]", event.getTitle(), topic);
        } catch (Exception e) {
            log.warn("Failed to broadcast announcement to event topic: {}", e.getMessage());
        }

        // 2. Notify all confirmed attendees
        Page<EventRegistration> attendeesPage = registrationRepository.findByEventIdWithUser(eventId, Pageable.unpaged());
        for (EventRegistration reg : attendeesPage) {
            if (reg.getStatus() == RegistrationStatus.CONFIRMED && !reg.getUser().getId().equals(organizerUserId)) {
                notificationService.createAndSendNotification(
                        reg.getUser(),
                        NotificationType.ANNOUNCEMENT,
                        String.format("Announcement: %s", event.getTitle()),
                        request.getTitle() + " — " + request.getMessage(),
                        "/events/" + (event.getSlug() != null ? event.getSlug() : event.getId())
                );
            }
        }

        // 3. Kafka Event Publication
        DomainEvent<AnnouncementDto> domainEvent = DomainEvent.of(
                "ANNOUNCEMENT_BROADCAST", "EVENT", eventId.toString(), dto
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_ANNOUNCEMENTS, eventId.toString(), domainEvent);

        return dto;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getEventAnnouncements(UUID eventId) {
        return announcementRepository.findByEventIdOrderByCreatedAtDesc(eventId)
                .stream()
                .map(AnnouncementDto::fromEntity)
                .collect(Collectors.toList());
    }
}
