package com.civicpulse.modules.event.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.event.dto.CreateEventRequest;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.event.dto.UpdateEventRequest;
import com.civicpulse.modules.event.model.*;
import com.civicpulse.modules.event.repository.CategoryRepository;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.event.repository.EventTagRepository;
import com.civicpulse.modules.event.statemachine.EventStateMachineService;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.organization.repository.OrganizationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final CategoryRepository categoryRepository;
    private final EventTagRepository eventTagRepository;
    private final UserRepository userRepository;
    private final EventStateMachineService stateMachineService;
    private final KafkaEventPublisher kafkaEventPublisher;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    @Transactional
    public EventDto createEvent(UUID creatorUserId, CreateEventRequest request) {
        validateEventTimes(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String baseSlug = toSlug(request.getTitle());
        String slug = baseSlug;
        int counter = 1;
        while (eventRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Set<EventTag> tags = resolveTags(request.getTagNames());

        Event event = Event.builder()
                .organization(org)
                .createdBy(creator)
                .category(category)
                .title(request.getTitle().trim())
                .slug(slug)
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .status(EventStatus.DRAFT)
                .visibility(request.getVisibility() != null ? request.getVisibility() : EventVisibility.PUBLIC)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .registrationDeadline(request.getRegistrationDeadline())
                .capacity(request.getCapacity())
                .currentRegistrationCount(0)
                .locationType(request.getLocationType())
                .venueName(request.getVenueName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .virtualMeetingUrl(request.getVirtualMeetingUrl())
                .bannerImageUrl(request.getBannerImageUrl())
                .waitlistEnabled(request.isWaitlistEnabled())
                .waitlistCapacity(request.getWaitlistCapacity())
                .tags(tags)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Created event ID [{}] with title [{}] and slug [{}]", savedEvent.getId(), savedEvent.getTitle(), savedEvent.getSlug());

        DomainEvent<EventDto> domainEvent = DomainEvent.of(
                "EVENT_CREATED", "EVENT", savedEvent.getId().toString(), EventDto.fromEntity(savedEvent)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_EVENTS_LIFECYCLE, savedEvent.getId().toString(), domainEvent);

        return EventDto.fromEntity(savedEvent);
    }

    @Transactional
    public EventDto updateEvent(UUID eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (event.getStatus() == EventStatus.COMPLETED || event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot edit an event that is completed or cancelled.");
        }

        validateEventTimes(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        if (request.getCapacity() < event.getCurrentRegistrationCount()) {
            throw new BadRequestException(
                    String.format("Capacity (%d) cannot be lower than existing confirmed registrations (%d).",
                            request.getCapacity(), event.getCurrentRegistrationCount())
            );
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        event.setCategory(category);
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription());
        event.setShortDescription(request.getShortDescription());
        if (request.getVisibility() != null) {
            event.setVisibility(request.getVisibility());
        }
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setRegistrationDeadline(request.getRegistrationDeadline());
        event.setCapacity(request.getCapacity());
        event.setLocationType(request.getLocationType());
        event.setVenueName(request.getVenueName());
        event.setAddress(request.getAddress());
        event.setCity(request.getCity());
        event.setState(request.getState());
        event.setPostalCode(request.getPostalCode());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setVirtualMeetingUrl(request.getVirtualMeetingUrl());
        event.setBannerImageUrl(request.getBannerImageUrl());
        event.setWaitlistEnabled(request.isWaitlistEnabled());
        event.setWaitlistCapacity(request.getWaitlistCapacity());
        event.setTags(resolveTags(request.getTagNames()));

        Event updatedEvent = eventRepository.save(event);
        log.info("Updated event ID [{}]", updatedEvent.getId());

        DomainEvent<EventDto> domainEvent = DomainEvent.of(
                "EVENT_UPDATED", "EVENT", updatedEvent.getId().toString(), EventDto.fromEntity(updatedEvent)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_EVENTS_LIFECYCLE, updatedEvent.getId().toString(), domainEvent);

        return EventDto.fromEntity(updatedEvent);
    }

    @Transactional
    public EventDto changeStatus(UUID eventId, EventStatus targetStatus) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        stateMachineService.validateAndTransition(event, targetStatus);
        Event saved = eventRepository.save(event);

        String eventType = "EVENT_" + targetStatus.name();
        DomainEvent<EventDto> domainEvent = DomainEvent.of(
                eventType, "EVENT", saved.getId().toString(), EventDto.fromEntity(saved)
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_EVENTS_LIFECYCLE, saved.getId().toString(), domainEvent);

        return EventDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public EventDto getEventBySlugOrId(String identifier) {
        Event event;
        try {
            UUID id = UUID.fromString(identifier);
            event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + identifier));
        } catch (IllegalArgumentException e) {
            event = eventRepository.findBySlug(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with slug: " + identifier));
        }

        return EventDto.fromEntity(event);
    }

    @Transactional(readOnly = true)
    public PagedResponse<EventDto> getManageableEvents(UUID userId, Pageable pageable) {
        Page<Event> page = eventRepository.findAllManageableByUser(userId, pageable);
        return PagedResponse.from(page.map(EventDto::fromEntity));
    }

    @Transactional(readOnly = true)
    public PagedResponse<EventDto> getOrganizationEvents(UUID orgId, Pageable pageable) {
        Page<Event> page = eventRepository.findByOrganizationId(orgId, pageable);
        return PagedResponse.from(page.map(EventDto::fromEntity));
    }

    private void validateEventTimes(Instant startTime, Instant endTime, Instant registrationDeadline) {
        if (!startTime.isBefore(endTime)) {
            throw new BadRequestException("Event start time must be before the end time.");
        }
        if (registrationDeadline.isAfter(startTime)) {
            throw new BadRequestException("Registration deadline cannot be after the event start time.");
        }
    }

    private Set<EventTag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new HashSet<>();
        }

        Set<EventTag> tags = new HashSet<>();
        for (String name : tagNames) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            String slug = toSlug(trimmed);

            EventTag tag = eventTagRepository.findBySlug(slug)
                    .orElseGet(() -> eventTagRepository.save(EventTag.builder().name(trimmed).slug(slug).build()));
            tags.add(tag);
        }
        return tags;
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
