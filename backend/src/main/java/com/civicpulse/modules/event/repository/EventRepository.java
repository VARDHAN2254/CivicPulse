package com.civicpulse.modules.event.repository;

import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.model.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithPessimisticLock(@Param("id") UUID id);

    @Query("SELECT e FROM Event e WHERE e.status IN ('PUBLISHED', 'REGISTRATION_OPEN', 'REGISTRATION_CLOSED') AND e.visibility = 'PUBLIC' AND e.endTime > :now")
    Page<Event> findUpcomingPublicEvents(@Param("now") Instant now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organization.id = :orgId")
    Page<Event> findByOrganizationId(@Param("orgId") UUID orgId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.category.slug = :categorySlug AND e.status IN ('PUBLISHED', 'REGISTRATION_OPEN') AND e.visibility = 'PUBLIC'")
    Page<Event> findByCategorySlug(@Param("categorySlug") String categorySlug, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organization.id IN (SELECT m.organization.id FROM OrganizationMembership m WHERE m.user.id = :userId)")
    Page<Event> findAllManageableByUser(@Param("userId") UUID userId, Pageable pageable);

    long countByStatus(EventStatus status);
}
