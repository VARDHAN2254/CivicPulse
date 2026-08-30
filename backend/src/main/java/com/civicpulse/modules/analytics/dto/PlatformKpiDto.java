package com.civicpulse.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformKpiDto {

    private long totalUsers;
    private long totalOrganizations;
    private long totalVerifiedOrganizations;
    private long totalEventsHosted;
    private long activeEvents;
    private long totalRegistrations;
    private long totalAttendeesCheckedIn;
    private long pendingModerationFlags;
    private Map<String, Long> eventsByCategory;
    private List<TimeSeriesDataPoint> userGrowthVelocity;
}
