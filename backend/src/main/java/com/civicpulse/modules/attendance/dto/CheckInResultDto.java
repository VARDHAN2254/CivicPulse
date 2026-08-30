package com.civicpulse.modules.attendance.dto;

import com.civicpulse.modules.attendance.model.CheckInMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckInResultDto {

    private boolean success;
    private String message;
    private UUID registrationId;
    private String ticketCode;
    private UUID eventId;
    private String eventTitle;
    private UUID attendeeUserId;
    private String attendeeName;
    private String attendeeEmail;
    private CheckInMethod checkInMethod;
    private Instant checkedInAt;
    private String checkedInByOrganizer;
}
