package com.civicpulse.modules.registration.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.registration.dto.AttendeeDto;
import com.civicpulse.modules.registration.dto.RegistrationDto;
import com.civicpulse.modules.registration.dto.RegistrationResponse;
import com.civicpulse.modules.registration.dto.WaitlistEntryDto;
import com.civicpulse.modules.registration.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Registrations & Waitlist", description = "Endpoints for event ticket reservations, waitlist promotion, and cancellations")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/registrations/events/{eventId}")
    @Operation(summary = "Register for an event or join automated waitlist")
    public ResponseEntity<ApiResponse<RegistrationResponse>> registerForEvent(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID eventId
    ) {
        RegistrationResponse response = registrationService.registerForEvent(currentUser.getId(), eventId);
        HttpStatus status = response.isConfirmed() ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return new ResponseEntity<>(ApiResponse.success(response.getMessage(), response), status);
    }

    @PostMapping("/registrations/{registrationId}/cancel")
    @Operation(summary = "Cancel confirmed registration and trigger waitlist promotion")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID registrationId
    ) {
        registrationService.cancelRegistration(currentUser.getId(), registrationId);
        return ResponseEntity.ok(ApiResponse.message("Registration cancelled successfully. Next waitlist attendee promoted."));
    }

    @GetMapping("/registrations/my")
    @Operation(summary = "Get all registrations for the authenticated user")
    public ResponseEntity<ApiResponse<List<RegistrationDto>>> getMyRegistrations(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<RegistrationDto> registrations = registrationService.getUserRegistrations(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(registrations));
    }

    @GetMapping("/registrations/my-waitlists")
    @Operation(summary = "Get all active waitlist positions for the authenticated user")
    public ResponseEntity<ApiResponse<List<WaitlistEntryDto>>> getMyWaitlists(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<WaitlistEntryDto> waitlists = registrationService.getUserWaitlistEntries(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(waitlists));
    }

    @GetMapping("/registrations/ticket/{ticketCode}")
    @Operation(summary = "Get ticket pass details by unique ticket code")
    public ResponseEntity<ApiResponse<RegistrationDto>> getTicket(@PathVariable String ticketCode) {
        RegistrationDto ticket = registrationService.getRegistrationByTicketCode(ticketCode);
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/events/{eventId}/attendees")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "List registered attendees for an event (Organizer only)")
    public ResponseEntity<ApiResponse<PagedResponse<AttendeeDto>>> getEventAttendees(
            @PathVariable UUID eventId,
            @PageableDefault(size = 25, sort = "registeredAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        PagedResponse<AttendeeDto> attendees = registrationService.getEventAttendees(eventId, pageable);
        return ResponseEntity.ok(ApiResponse.success(attendees));
    }
}
