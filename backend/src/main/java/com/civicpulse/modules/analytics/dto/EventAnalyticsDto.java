package com.civicpulse.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAnalyticsDto {

    private UUID eventId;
    private String eventTitle;
    private int capacity;
    private long totalConfirmed;
    private long totalWaitlisted;
    private long totalCancelled;
    private long totalCheckedIn;
    private double capacityFillPercentage;
    private double turnoutRatePercentage;
    private Map<String, Long> checkInMethodBreakdown;
    private List<TimeSeriesDataPoint> registrationVelocity;
}
