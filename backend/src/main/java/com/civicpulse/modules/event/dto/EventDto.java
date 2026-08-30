package com.civicpulse.modules.event.dto;

import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.model.EventStatus;
import com.civicpulse.modules.event.model.EventVisibility;
import com.civicpulse.modules.event.model.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private String organizationSlug;
    private String organizationLogoUrl;
    private UUID createdByUserId;
    private String createdByUserName;
    private UUID categoryId;
    private String categoryName;
    private String categorySlug;
    private String title;
    private String slug;
    private String description;
    private String shortDescription;
    private EventStatus status;
    private EventVisibility visibility;
    private Instant startTime;
    private Instant endTime;
    private Instant registrationDeadline;
    private int capacity;
    private int currentRegistrationCount;
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
    private List<EventTagDto> tags;
    private Instant createdAt;
    private Instant updatedAt;

    public static EventDto fromEntity(Event event) {
        if (event == null) return null;
        return EventDto.builder()
                .id(event.getId())
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getName())
                .organizationSlug(event.getOrganization().getSlug())
                .organizationLogoUrl(event.getOrganization().getLogoUrl())
                .createdByUserId(event.getCreatedBy().getId())
                .createdByUserName(event.getCreatedBy().getFullName())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .categorySlug(event.getCategory().getSlug())
                .title(event.getTitle())
                .slug(event.getSlug())
                .description(event.getDescription())
                .shortDescription(event.getShortDescription())
                .status(event.getStatus())
                .visibility(event.getVisibility())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .registrationDeadline(event.getRegistrationDeadline())
                .capacity(event.getCapacity())
                .currentRegistrationCount(event.getCurrentRegistrationCount())
                .locationType(event.getLocationType())
                .venueName(event.getVenueName())
                .address(event.getAddress())
                .city(event.getCity())
                .state(event.getState())
                .postalCode(event.getPostalCode())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .virtualMeetingUrl(event.getVirtualMeetingUrl())
                .bannerImageUrl(event.getBannerImageUrl())
                .waitlistEnabled(event.isWaitlistEnabled())
                .waitlistCapacity(event.getWaitlistCapacity())
                .tags(event.getTags() != null ? event.getTags().stream().map(EventTagDto::fromEntity).collect(Collectors.toList()) : List.of())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
