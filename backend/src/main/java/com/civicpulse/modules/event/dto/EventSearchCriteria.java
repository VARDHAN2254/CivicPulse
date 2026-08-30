package com.civicpulse.modules.event.dto;

import com.civicpulse.modules.event.model.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSearchCriteria {

    private String query;
    private String category;
    private UUID organizationId;
    private LocationType locationType;
    private String city;
    private Instant fromDate;
    private Instant toDate;
    private Boolean availableOnly;
    private List<String> tags;
    @Builder.Default
    private String sortBy = "startTime"; // startTime, popularity, relevance
    @Builder.Default
    private String sortDirection = "ASC";
}
