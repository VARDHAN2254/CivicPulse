package com.civicpulse.modules.registration.repository;

import com.civicpulse.modules.registration.model.WaitlistEntry;
import com.civicpulse.modules.registration.model.WaitlistStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

    Optional<WaitlistEntry> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, WaitlistStatus status);

    List<WaitlistEntry> findByEventIdAndStatusOrderByPositionAsc(UUID eventId, WaitlistStatus status);

    @Query("SELECT w FROM WaitlistEntry w JOIN FETCH w.user WHERE w.event.id = :eventId ORDER BY w.position ASC")
    Page<WaitlistEntry> findByEventIdWithUser(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT w FROM WaitlistEntry w JOIN FETCH w.event e JOIN FETCH e.organization WHERE w.user.id = :userId ORDER BY w.joinedAt DESC")
    List<WaitlistEntry> findAllByUserIdWithEvent(@Param("userId") UUID userId);

    long countByEventIdAndStatus(UUID eventId, WaitlistStatus status);

    @Query("SELECT COALESCE(MAX(w.position), 0) FROM WaitlistEntry w WHERE w.event.id = :eventId AND w.status = 'PENDING'")
    int findMaxPositionByEventId(@Param("eventId") UUID eventId);
}
