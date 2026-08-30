package com.civicpulse.modules.attendance.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.common.security.CustomUserDetails;
import com.civicpulse.modules.attendance.dto.*;
import com.civicpulse.modules.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "QR Attendance & Verification", description = "Endpoints for dynamic HMAC QR tokens, organizer check-in scanning, and live metrics")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/registrations/{registrationId}/qr-token")
    @Operation(summary = "Get dynamic time-expiring HMAC-SHA256 QR code token for an event pass")
    public ResponseEntity<ApiResponse<QrTokenDto>> getQrToken(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable UUID registrationId
    ) {
        QrTokenDto tokenDto = attendanceService.getDynamicQrToken(currentUser.getId(), registrationId);
        return ResponseEntity.ok(ApiResponse.success(tokenDto));
    }

    @PostMapping("/attendance/scan")
    @Operation(summary = "Organizer scans and verifies attendee dynamic QR token")
    public ResponseEntity<ApiResponse<CheckInResultDto>> checkInByQr(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody QrCheckInRequest request
    ) {
        CheckInResultDto result = attendanceService.checkInByQr(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Check-in successful", result));
    }

    @PostMapping("/attendance/manual")
    @Operation(summary = "Organizer checks in attendee by manual ticket code fallback")
    public ResponseEntity<ApiResponse<CheckInResultDto>> checkInByManualCode(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ManualCheckInRequest request
    ) {
        CheckInResultDto result = attendanceService.checkInByManualCode(currentUser.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Manual check-in successful", result));
    }

    @GetMapping("/events/{eventId}/attendance-metrics")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Get real-time attendance rate and check-in counts (Organizer only)")
    public ResponseEntity<ApiResponse<AttendanceMetricsDto>> getAttendanceMetrics(
            @PathVariable UUID eventId
    ) {
        AttendanceMetricsDto metrics = attendanceService.getAttendanceMetrics(eventId);
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
