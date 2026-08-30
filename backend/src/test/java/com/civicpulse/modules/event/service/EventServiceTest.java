package com.civicpulse.modules.event.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.modules.event.dto.CreateEventRequest;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.event.model.*;
import com.civicpulse.modules.event.repository.CategoryRepository;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.event.repository.EventTagRepository;
import com.civicpulse.modules.event.statemachine.EventStateMachineService;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.organization.repository.OrganizationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventTagRepository eventTagRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private EventStateMachineService stateMachineService;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private EventService eventService;

    private User sampleOrganizer;
    private Organization sampleOrg;
    private Category sampleCategory;
    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        sampleOrganizer = User.builder()
                .id(UUID.randomUUID())
                .email("organizer@civicpulse.org")
                .fullName("Sarah Jenkins")
                .role(UserRole.ORGANIZER)
                .build();

        sampleOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Green Earth Volunteers")
                .slug("green-earth-volunteers")
                .build();

        sampleCategory = Category.builder()
                .id(UUID.randomUUID())
                .name("Environment & Sustainability")
                .slug("environment-sustainability")
                .build();

        Instant now = Instant.now();
        sampleEvent = Event.builder()
                .id(UUID.randomUUID())
                .organization(sampleOrg)
                .createdBy(sampleOrganizer)
                .category(sampleCategory)
                .title("River Cleanup 2026")
                .slug("river-cleanup-2026")
                .description("Annual river cleaning drive")
                .status(EventStatus.DRAFT)
                .capacity(50)
                .currentRegistrationCount(0)
                .startTime(now.plus(7, ChronoUnit.DAYS))
                .endTime(now.plus(7, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS))
                .registrationDeadline(now.plus(6, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("Should successfully create draft event")
    void createEvent_Success() {
        Instant now = Instant.now();
        CreateEventRequest request = CreateEventRequest.builder()
                .organizationId(sampleOrg.getId())
                .categoryId(sampleCategory.getId())
                .title("River Cleanup 2026")
                .description("Annual river cleaning drive")
                .capacity(50)
                .startTime(now.plus(7, ChronoUnit.DAYS))
                .endTime(now.plus(7, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS))
                .registrationDeadline(now.plus(6, ChronoUnit.DAYS))
                .locationType(LocationType.IN_PERSON)
                .build();

        when(userRepository.findById(sampleOrganizer.getId())).thenReturn(Optional.of(sampleOrganizer));
        when(organizationRepository.findById(sampleOrg.getId())).thenReturn(Optional.of(sampleOrg));
        when(categoryRepository.findById(sampleCategory.getId())).thenReturn(Optional.of(sampleCategory));
        when(eventRepository.existsBySlug(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenReturn(sampleEvent);

        EventDto result = eventService.createEvent(sampleOrganizer.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("River Cleanup 2026");
        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw BadRequestException if start time is after end time")
    void createEvent_InvalidTimes_ThrowsBadRequest() {
        Instant now = Instant.now();
        CreateEventRequest request = CreateEventRequest.builder()
                .startTime(now.plus(8, ChronoUnit.DAYS))
                .endTime(now.plus(7, ChronoUnit.DAYS)) // End before start
                .registrationDeadline(now.plus(6, ChronoUnit.DAYS))
                .build();

        assertThatThrownBy(() -> eventService.createEvent(sampleOrganizer.getId(), request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("start time must be before the end time");
    }

    @Test
    @DisplayName("Should transition event from DRAFT to PUBLISHED")
    void changeStatus_DraftToPublished_Success() {
        when(eventRepository.findById(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(sampleEvent);

        EventDto result = eventService.changeStatus(sampleEvent.getId(), EventStatus.PUBLISHED);

        assertThat(result.getStatus()).isEqualTo(EventStatus.PUBLISHED);
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should throw BadRequestException on illegal transition from COMPLETED")
    void changeStatus_CompletedToDraft_ThrowsBadRequest() {
        sampleEvent.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));

        assertThatThrownBy(() -> eventService.changeStatus(sampleEvent.getId(), EventStatus.DRAFT))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid state transition");
    }
}
