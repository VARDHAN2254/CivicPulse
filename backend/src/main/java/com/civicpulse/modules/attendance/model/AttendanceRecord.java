package com.civicpulse.modules.attendance.model;

import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.user.model.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attendance_records", uniqueConstraints = {
    @UniqueConstraint(name = "uq_attendance_registration", columnNames = {"registration_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private EventRegistration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_in_by_user_id", nullable = false)
    private User checkedInBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_in_method", nullable = false, length = 50)
    @Builder.Default
    private CheckInMethod checkInMethod = CheckInMethod.QR_SCAN;

    @CreationTimestamp
    @Column(name = "checked_in_at", nullable = false, updatable = false)
    private Instant checkedInAt;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;
}
