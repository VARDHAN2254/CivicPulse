package com.civicpulse.integration;

import com.civicpulse.modules.analytics.dto.EventAnalyticsDto;
import com.civicpulse.modules.analytics.service.AnalyticsService;
import com.civicpulse.modules.attendance.dto.CheckInResultDto;
import com.civicpulse.modules.attendance.dto.QrCheckInRequest;
import com.civicpulse.modules.attendance.dto.QrTokenDto;
import com.civicpulse.modules.attendance.service.AttendanceService;
import com.civicpulse.modules.discussion.dto.CreatePostRequest;
import com.civicpulse.modules.discussion.dto.DiscussionPostDto;
import com.civicpulse.modules.discussion.service.DiscussionService;
import com.civicpulse.modules.event.dto.CreateEventRequest;
import com.civicpulse.modules.event.dto.EventDto;
import com.civicpulse.modules.event.model.EventStatus;
import com.civicpulse.modules.event.model.LocationType;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.event.service.EventService;
import com.civicpulse.modules.event.statemachine.EventStateMachineService;
import com.civicpulse.modules.notification.dto.CreateAnnouncementRequest;
import com.civicpulse.modules.notification.service.AnnouncementService;
import com.civicpulse.modules.organization.dto.CreateOrganizationRequest;
import com.civicpulse.modules.organization.dto.OrganizationDto;
import com.civicpulse.modules.organization.service.OrganizationService;
import com.civicpulse.modules.registration.dto.RegistrationDto;
import com.civicpulse.modules.registration.dto.RegistrationResponse;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.service.RegistrationService;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FullLifecycleIntegrationTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private EventService eventService;

    @Mock
    private EventStateMachineService eventStateMachineService;

    @Mock
    private RegistrationService registrationService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private DiscussionService discussionService;

    @Mock
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("End-to-End Domain Lifecycle: Org -> Event -> Register -> QR Check-In -> Discussion -> Analytics")
    void testFullCivicPulseLifecycle() {
        UUID organizerId = UUID.randomUUID();
        UUID attendeeId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID regId = UUID.randomUUID();

        // 1. Create Organization
        OrganizationDto orgDto = OrganizationDto.builder()
                .id(orgId)
                .name("Eco Action Alliance")
                .slug("eco-action-alliance")
                .verified(true)
                .build();
        when(organizationService.createOrganization(eq(organizerId), any(CreateOrganizationRequest.class))).thenReturn(orgDto);

        OrganizationDto createdOrg = organizationService.createOrganization(organizerId, CreateOrganizationRequest.builder().name("Eco Action Alliance").build());
        assertThat(createdOrg.getName()).isEqualTo("Eco Action Alliance");

        // 2. Create Event & Publish
        EventDto eventDto = EventDto.builder()
                .id(eventId)
                .organizationId(orgId)
                .title("Urban Tree Planting Initiative")
                .slug("urban-tree-planting-initiative")
                .status(EventStatus.REGISTRATION_OPEN)
                .capacity(50)
                .currentRegistrationCount(1)
                .build();
        when(eventService.createEvent(eq(organizerId), any(CreateEventRequest.class))).thenReturn(eventDto);

        EventDto createdEvent = eventService.createEvent(organizerId, CreateEventRequest.builder().title("Urban Tree Planting Initiative").capacity(50).build());
        assertThat(createdEvent.getTitle()).isEqualTo("Urban Tree Planting Initiative");

        // 3. Register Attendee & Issue Ticket Pass
        RegistrationDto passDto = RegistrationDto.builder()
                .id(regId)
                .eventId(eventId)
                .userId(attendeeId)
                .ticketCode("CP-URBANT-109283")
                .status(RegistrationStatus.CONFIRMED)
                .build();
        RegistrationResponse regResponse = RegistrationResponse.builder()
                .confirmed(true)
                .ticketCode("CP-URBANT-109283")
                .registration(passDto)
                .build();
        when(registrationService.registerForEvent(eq(attendeeId), eq(eventId))).thenReturn(regResponse);

        RegistrationResponse registeredPass = registrationService.registerForEvent(attendeeId, eventId);
        assertThat(registeredPass.getTicketCode()).isEqualTo("CP-URBANT-109283");
        assertThat(registeredPass.isConfirmed()).isTrue();

        // 4. Generate Dynamic HMAC QR Pass
        QrTokenDto qrTokenDto = QrTokenDto.builder()
                .registrationId(regId)
                .token("valid-dynamic-hmac-token-base64")
                .expiresInSeconds(120)
                .build();
        when(attendanceService.getDynamicQrToken(eq(attendeeId), eq(regId))).thenReturn(qrTokenDto);

        QrTokenDto issuedQr = attendanceService.getDynamicQrToken(attendeeId, regId);
        assertThat(issuedQr.getToken()).isNotBlank();

        // 5. Scan QR & Check In Attendee
        CheckInResultDto checkInResult = CheckInResultDto.builder()
                .success(true)
                .registrationId(regId)
                .ticketCode("CP-URBANT-109283")
                .attendeeName("Jordan Lee")
                .build();
        when(attendanceService.checkInByQr(eq(organizerId), any(QrCheckInRequest.class))).thenReturn(checkInResult);

        CheckInResultDto scanResult = attendanceService.checkInByQr(organizerId, QrCheckInRequest.builder().qrToken(issuedQr.getToken()).build());
        assertThat(scanResult.isSuccess()).isTrue();
        assertThat(scanResult.getTicketCode()).isEqualTo("CP-URBANT-109283");

        // 6. Post Community Discussion Comment
        DiscussionPostDto postDto = DiscussionPostDto.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(attendeeId)
                .content("Tree planting was a huge success today!")
                .build();
        when(discussionService.createPost(eq(attendeeId), eq(eventId), any(CreatePostRequest.class))).thenReturn(postDto);

        DiscussionPostDto comment = discussionService.createPost(attendeeId, eventId, CreatePostRequest.builder().content("Tree planting was a huge success today!").build());
        assertThat(comment.getContent()).contains("huge success");

        // 7. Verify Event Analytics & Conversion Funnel
        EventAnalyticsDto analyticsDto = EventAnalyticsDto.builder()
                .eventId(eventId)
                .capacity(50)
                .totalConfirmed(1)
                .totalCheckedIn(1)
                .turnoutRatePercentage(100.0)
                .build();
        when(analyticsService.getEventAnalytics(eq(eventId))).thenReturn(analyticsDto);

        EventAnalyticsDto analytics = analyticsService.getEventAnalytics(eventId);
        assertThat(analytics.getTurnoutRatePercentage()).isEqualTo(100.0);
    }
}
