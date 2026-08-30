package com.civicpulse.modules.event.model;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.modules.organization.model.Organization;
import com.civicpulse.modules.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EventVisibility visibility = EventVisibility.PUBLIC;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "registration_deadline", nullable = false)
    private Instant registrationDeadline;

    @Column(nullable = false)
    @Builder.Default
    private int capacity = 50;

    @Column(name = "current_registration_count", nullable = false)
    @Builder.Default
    private int currentRegistrationCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    @Builder.Default
    private LocationType locationType = LocationType.IN_PERSON;

    @Column(name = "venue_name")
    private String venueName;

    @Column
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "virtual_meeting_url", length = 500)
    private String virtualMeetingUrl;

    @Column(name = "banner_image_url", length = 500)
    private String bannerImageUrl;

    @Column(name = "waitlist_enabled", nullable = false)
    @Builder.Default
    private boolean waitlistEnabled = true;

    @Column(name = "waitlist_capacity", nullable = false)
    @Builder.Default
    private int waitlistCapacity = 100;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private int version = 0;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "event_tag_mappings",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<EventTag> tags = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void validateAcceptingRegistrations() {
        if (this.status != EventStatus.REGISTRATION_OPEN && this.status != EventStatus.PUBLISHED) {
            throw new BadRequestException("This event is currently not accepting registrations (Status: " + this.status + ").");
        }
        if (Instant.now().isAfter(this.registrationDeadline)) {
            throw new BadRequestException("The registration deadline for this event has passed.");
        }
    }

    public boolean hasAvailableCapacity() {
        return this.currentRegistrationCount < this.capacity;
    }

    public void incrementRegistrationCount() {
        if (this.currentRegistrationCount >= this.capacity) {
            throw new BadRequestException("Event capacity reached.");
        }
        this.currentRegistrationCount++;
    }

    public void decrementRegistrationCount() {
        if (this.currentRegistrationCount > 0) {
            this.currentRegistrationCount--;
        }
    }
}
