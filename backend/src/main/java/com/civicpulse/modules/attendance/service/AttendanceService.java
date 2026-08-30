package com.civicpulse.modules.attendance.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ConflictException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.attendance.dto.*;
import com.civicpulse.modules.attendance.model.AttendanceRecord;
import com.civicpulse.modules.attendance.model.CheckInMethod;
import com.civicpulse.modules.attendance.repository.AttendanceRecordRepository;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
import com.civicpulse.modules.registration.repository.RegistrationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final QrCodeService qrCodeService;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional(readOnly = true)
    public QrTokenDto getDynamicQrToken(UUID userId, UUID registrationId) {
        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        if (!registration.getUser().getId().equals(userId)) {
            throw new BadRequestException("You can only generate QR tokens for your own ticket passes.");
        }

        if (registration.getStatus() != RegistrationStatus.CONFIRMED && registration.getStatus() != RegistrationStatus.ATTENDED) {
            throw new BadRequestException("Cannot generate dynamic QR pass for a cancelled registration.");
        }

        String token = qrCodeService.generateDynamicQrToken(registrationId);
        return QrTokenDto.builder()
                .registrationId(registrationId)
                .token(token)
                .expiresInSeconds(120)
                .expiresAt(Instant.now().plus(120, ChronoUnit.SECONDS))
                .build();
    }

    @Transactional
    public CheckInResultDto checkInByQr(UUID organizerUserId, QrCheckInRequest request) {
        UUID registrationId = qrCodeService.validateAndExtractRegistrationId(request.getQrToken());
        return processCheckIn(organizerUserId, registrationId, CheckInMethod.QR_SCAN, request.getDeviceInfo());
    }

    @Transactional
    public CheckInResultDto checkInByManualCode(UUID organizerUserId, ManualCheckInRequest request) {
        EventRegistration registration = registrationRepository.findByTicketCode(request.getTicketCode().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket code not found: " + request.getTicketCode()));

        if (!registration.getEvent().getId().equals(request.getEventId())) {
            throw new BadRequestException("This ticket code belongs to a different event.");
        }

        return processCheckIn(organizerUserId, registration.getId(), CheckInMethod.MANUAL_CODE, request.getDeviceInfo());
    }

    private CheckInResultDto processCheckIn(UUID organizerUserId, UUID registrationId, CheckInMethod method, String deviceInfo) {
        User organizer = userRepository.findById(organizerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer user not found"));

        EventRegistration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found"));

        Event event = registration.getEvent();

        // 1. Guard against duplicate check-ins
        if (registration.getStatus() == RegistrationStatus.ATTENDED || attendanceRecordRepository.existsByRegistrationId(registrationId)) {
            log.warn("Duplicate check-in attempted for ticket [{}] by organizer [{}]", registration.getTicketCode(), organizer.getEmail());
            throw new ConflictException(
                    String.format("Duplicate check-in: Attendee %s (%s) was already checked in.",
                            registration.getUser().getFullName(), registration.getTicketCode())
            );
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Check-in rejected: This ticket registration has been cancelled.");
        }

        // 2. Mark as ATTENDED
        registration.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(registration);

        // 3. Create Immutable Attendance Record
        AttendanceRecord attendance = AttendanceRecord.builder()
                .registration(registration)
                .event(event)
                .user(registration.getUser())
                .checkedInBy(organizer)
                .checkInMethod(method)
                .deviceInfo(deviceInfo)
                .build();

        AttendanceRecord savedAttendance = attendanceRecordRepository.save(attendance);
        log.info("Verified and checked in attendee [{}] with ticket [{}] via [{}]",
                registration.getUser().getEmail(), registration.getTicketCode(), method);

        CheckInResultDto result = CheckInResultDto.builder()
                .success(true)
                .message("Check-in verified successfully!")
                .registrationId(registration.getId())
                .ticketCode(registration.getTicketCode())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .attendeeUserId(registration.getUser().getId())
                .attendeeName(registration.getUser().getFullName())
                .attendeeEmail(registration.getUser().getEmail())
                .checkInMethod(method)
                .checkedInAt(savedAttendance.getCheckedInAt())
                .checkedInByOrganizer(organizer.getFullName())
                .build();

        // 4. Publish Kafka Attendance Event
        DomainEvent<CheckInResultDto> domainEvent = DomainEvent.of(
                "ATTENDANCE_RECORDED", "ATTENDANCE", savedAttendance.getId().toString(), result
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_ATTENDANCE, savedAttendance.getId().toString(), domainEvent);

        return result;
    }

    @Transactional(readOnly = true)
    public AttendanceMetricsDto getAttendanceMetrics(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        long confirmedCount = registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED) +
                             registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.ATTENDED);
        long checkedInCount = attendanceRecordRepository.countByEventId(eventId);

        double rate = confirmedCount > 0 ? ((double) checkedInCount / confirmedCount) * 100.0 : 0.0;

        return AttendanceMetricsDto.builder()
                .eventId(eventId)
                .capacity(event.getCapacity())
                .confirmedCount(confirmedCount)
                .checkedInCount(checkedInCount)
                .attendanceRatePercentage(Math.round(rate * 10.0) / 10.0)
                .build();
    }
}
