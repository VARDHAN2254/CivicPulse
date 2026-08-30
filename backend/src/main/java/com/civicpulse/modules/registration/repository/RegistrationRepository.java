package com.civicpulse.modules.registration.repository;

import com.civicpulse.modules.registration.model.EventRegistration;
import com.civicpulse.modules.registration.model.RegistrationStatus;
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
public interface RegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    Optional<EventRegistration> findByEventIdAndUserId(UUID eventId, UUID userId);

    Optional<EventRegistration> findByTicketCode(String ticketCode);

    boolean existsByEventIdAndUserIdAndStatus(UUID eventId, UUID userId, RegistrationStatus status);

    @Query("SELECT r FROM EventRegistration r JOIN FETCH r.event e JOIN FETCH e.organization WHERE r.user.id = :userId ORDER BY e.startTime DESC")
    List<EventRegistration> findAllByUserIdWithEvent(@Param("userId") UUID userId);

    @Query("SELECT r FROM EventRegistration r JOIN FETCH r.user u WHERE r.event.id = :eventId ORDER BY r.registeredAt ASC")
    Page<EventRegistration> findByEventIdWithUser(@Param("eventId") UUID eventId, Pageable pageable);

    long countByEventIdAndStatus(UUID eventId, RegistrationStatus status);
}
