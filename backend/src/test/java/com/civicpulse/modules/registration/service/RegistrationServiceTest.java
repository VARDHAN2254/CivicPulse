package com.civicpulse.modules.registration.service;

import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.model.EventStatus;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.registration.dto.RegistrationResponse;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.model.WaitlistEntry;
import com.civicpulse.modules.registration.model.WaitlistStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.registration.repository.WaitlistRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class RegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WaitlistPromotionService waitlistPromotionService;

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private RegistrationService registrationService;

    private User sampleUser;
    private Event sampleEvent;
    private Organization sampleOrg;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("attendee@civicpulse.org")
                .fullName("Alex Rivera")
                .role(UserRole.MEMBER)
                .build();

        sampleOrg = Organization.builder()
                .id(UUID.randomUUID())
                .name("Green Earth Volunteers")
                .slug("green-earth-volunteers")
                .build();

        Instant now = Instant.now();
        sampleEvent = Event.builder()
                .id(UUID.randomUUID())
                .organization(sampleOrg)
                .title("Clean Rivers Community Clean-Up")
                .slug("clean-rivers-clean-up")
                .status(EventStatus.REGISTRATION_OPEN)
                .capacity(10)
                .currentRegistrationCount(5)
                .waitlistEnabled(true)
                .waitlistCapacity(20)
                .startTime(now.plus(7, ChronoUnit.DAYS))
                .endTime(now.plus(7, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS))
                .registrationDeadline(now.plus(6, ChronoUnit.DAYS))
                .build();
    }

    @Test
    @DisplayName("Should successfully confirm registration when capacity is available")
    void registerForEvent_CapacityAvailable_ConfirmsRegistration() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(eventRepository.findByIdWithPessimisticLock(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.findByEventIdAndUserId(sampleEvent.getId(), sampleUser.getId())).thenReturn(Optional.empty());
        when(waitlistRepository.findByEventIdAndUserId(sampleEvent.getId(), sampleUser.getId())).thenReturn(Optional.empty());

        EventRegistration savedReg = EventRegistration.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .user(sampleUser)
                .ticketCode("CP-CLEANR-123456")
                .status(RegistrationStatus.CONFIRMED)
                .registeredAt(Instant.now())
                .build();
        when(registrationRepository.save(any(EventRegistration.class))).thenReturn(savedReg);

        RegistrationResponse response = registrationService.registerForEvent(sampleUser.getId(), sampleEvent.getId());

        assertThat(response).isNotNull();
        assertThat(response.isConfirmed()).isTrue();
        assertThat(response.isWaitlisted()).isFalse();
        assertThat(response.getTicketCode()).isNotNull();

        verify(eventRepository, times(1)).save(sampleEvent);
        verify(registrationRepository, times(1)).save(any(EventRegistration.class));
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should add user to waitlist when capacity is full")
    void registerForEvent_CapacityFull_JoinsWaitlist() {
        sampleEvent.setCurrentRegistrationCount(10); // Full capacity

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(eventRepository.findByIdWithPessimisticLock(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.findByEventIdAndUserId(sampleEvent.getId(), sampleUser.getId())).thenReturn(Optional.empty());
        when(waitlistRepository.findByEventIdAndUserId(sampleEvent.getId(), sampleUser.getId())).thenReturn(Optional.empty());
        when(waitlistRepository.countByEventIdAndStatus(sampleEvent.getId(), WaitlistStatus.PENDING)).thenReturn(2L);
        when(waitlistRepository.findMaxPositionByEventId(sampleEvent.getId())).thenReturn(2);

        WaitlistEntry savedEntry = WaitlistEntry.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .user(sampleUser)
                .position(3)
                .status(WaitlistStatus.PENDING)
                .build();
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenReturn(savedEntry);

        RegistrationResponse response = registrationService.registerForEvent(sampleUser.getId(), sampleEvent.getId());

        assertThat(response).isNotNull();
        assertThat(response.isConfirmed()).isFalse();
        assertThat(response.isWaitlisted()).isTrue();
        assertThat(response.getWaitlistPosition()).isEqualTo(3);

        verify(waitlistRepository, times(1)).save(any(WaitlistEntry.class));
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should reject duplicate registration if user is already confirmed")
    void registerForEvent_Duplicate_ThrowsConflictException() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(eventRepository.findByIdWithPessimisticLock(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));

        EventRegistration existingReg = EventRegistration.builder()
                .status(RegistrationStatus.CONFIRMED)
                .build();
        when(registrationRepository.findByEventIdAndUserId(sampleEvent.getId(), sampleUser.getId())).thenReturn(Optional.of(existingReg));

        assertThatThrownBy(() -> registrationService.registerForEvent(sampleUser.getId(), sampleEvent.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Should cancel confirmed registration and trigger waitlist promotion")
    void cancelRegistration_Success_TriggersPromotion() {
        EventRegistration registration = EventRegistration.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .user(sampleUser)
                .ticketCode("CP-CLEANR-123456")
                .status(RegistrationStatus.CONFIRMED)
                .build();

        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(eventRepository.findByIdWithPessimisticLock(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));

        registrationService.cancelRegistration(sampleUser.getId(), registration.getId());

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        verify(waitlistPromotionService, times(1)).promoteNextCandidate(sampleEvent);
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }
}
