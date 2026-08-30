package com.civicpulse.modules.analytics.service;

import com.civicpulse.modules.analytics.dto.EventAnalyticsDto;
import com.civicpulse.modules.analytics.dto.PlatformKpiDto;
import com.civicpulse.modules.attendance.repository.AttendanceRecordRepository;
import com.civicpulse.modules.discussion.repository.ModerationFlagRepository;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.organization.repository.OrganizationRepository;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.model.WaitlistStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.registration.repository.WaitlistRepository;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private WaitlistRepository waitlistRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModerationFlagRepository moderationFlagRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Event sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = Event.builder()
                .id(UUID.randomUUID())
                .title("Clean Rivers Community Clean-Up")
                .capacity(100)
                .build();
    }

    @Test
    @DisplayName("Should compute correct capacity fill and turnout rate for an event")
    void getEventAnalytics_Success() {
        when(eventRepository.findById(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(registrationRepository.countByEventIdAndStatus(sampleEvent.getId(), RegistrationStatus.CONFIRMED)).thenReturn(40L);
        when(registrationRepository.countByEventIdAndStatus(sampleEvent.getId(), RegistrationStatus.ATTENDED)).thenReturn(40L);
        when(registrationRepository.countByEventIdAndStatus(sampleEvent.getId(), RegistrationStatus.CANCELLED)).thenReturn(5L);
        when(waitlistRepository.countByEventIdAndStatus(sampleEvent.getId(), WaitlistStatus.PENDING)).thenReturn(10L);
        when(attendanceRecordRepository.findByEventIdWithUser(eq(sampleEvent.getId()), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        when(registrationRepository.findByEventIdWithUser(eq(sampleEvent.getId()), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        EventAnalyticsDto result = analyticsService.getEventAnalytics(sampleEvent.getId());

        assertThat(result).isNotNull();
        assertThat(result.getCapacity()).isEqualTo(100);
        assertThat(result.getTotalConfirmed()).isEqualTo(40L);
        assertThat(result.getTotalCheckedIn()).isEqualTo(40L);
        assertThat(result.getCapacityFillPercentage()).isEqualTo(80.0);
        assertThat(result.getTurnoutRatePercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should retrieve platform-wide KPIs")
    void getPlatformKpis_Success() {
        when(userRepository.count()).thenReturn(1500L);
        when(organizationRepository.count()).thenReturn(50L);
        when(organizationRepository.countByVerifiedTrue()).thenReturn(35L);
        when(eventRepository.count()).thenReturn(120L);
        when(registrationRepository.count()).thenReturn(4500L);
        when(attendanceRecordRepository.count()).thenReturn(3800L);
        when(moderationFlagRepository.countByStatus("PENDING")).thenReturn(2L);

        PlatformKpiDto result = analyticsService.getPlatformKpis();

        assertThat(result).isNotNull();
        assertThat(result.getTotalUsers()).isEqualTo(1500L);
        assertThat(result.getTotalVerifiedOrganizations()).isEqualTo(35L);
        assertThat(result.getTotalAttendeesCheckedIn()).isEqualTo(3800L);
    }
}
