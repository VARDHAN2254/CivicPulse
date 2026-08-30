package com.civicpulse.modules.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMetricsDto {

    private UUID eventId;
    private int capacity;
    private long confirmedCount;
    private long checkedInCount;
    private double attendanceRatePercentage;
}
