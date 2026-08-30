package com.civicpulse.modules.event.dto;

import com.civicpulse.modules.event.model.EventVisibility;
import com.civicpulse.modules.event.model.LocationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String shortDescription;

    private EventVisibility visibility;

    @NotNull(message = "Start time is required")
    private Instant startTime;

    @NotNull(message = "End time is required")
    private Instant endTime;

    @NotNull(message = "Registration deadline is required")
    private Instant registrationDeadline;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacity;

    @NotNull(message = "Location type is required")
    private LocationType locationType;

    private String venueName;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String virtualMeetingUrl;
    private String bannerImageUrl;

    private boolean waitlistEnabled;
    private int waitlistCapacity;
    private List<String> tagNames;
}
