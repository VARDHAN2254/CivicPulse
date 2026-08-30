package com.civicpulse.modules.attendance.repository;

import com.civicpulse.modules.attendance.model.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    Optional<AttendanceRecord> findByRegistrationId(UUID registrationId);

    boolean existsByRegistrationId(UUID registrationId);

    long countByEventId(UUID eventId);

    @Query("SELECT a FROM AttendanceRecord a JOIN FETCH a.user JOIN FETCH a.registration WHERE a.event.id = :eventId ORDER BY a.checkedInAt DESC")
    Page<AttendanceRecord> findByEventIdWithUser(@Param("eventId") UUID eventId, Pageable pageable);
}
