package com.civicpulse.modules.analytics.controller;

import com.civicpulse.common.response.ApiResponse;
import com.civicpulse.modules.analytics.dto.EventAnalyticsDto;
import com.civicpulse.modules.analytics.dto.PlatformKpiDto;
import com.civicpulse.modules.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Dashboards", description = "Endpoints for organizer event conversions and platform metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/events/{eventId}")
    @PreAuthorize("@orgPermissionEvaluator.canManageEvent(authentication, #eventId)")
    @Operation(summary = "Get deep conversion and attendance analytics for an event (Organizer only)")
    public ResponseEntity<ApiResponse<EventAnalyticsDto>> getEventAnalytics(@PathVariable UUID eventId) {
        EventAnalyticsDto analytics = analyticsService.getEventAnalytics(eventId);
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }

    @GetMapping("/platform")
    @Operation(summary = "Get high-level platform KPIs and ecosystem health metrics")
    public ResponseEntity<ApiResponse<PlatformKpiDto>> getPlatformKpis() {
        PlatformKpiDto kpis = analyticsService.getPlatformKpis();
        return ResponseEntity.ok(ApiResponse.success(kpis));
    }
}
