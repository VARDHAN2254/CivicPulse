package com.civicpulse.modules.notification.repository;

import com.civicpulse.modules.notification.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    @Query("SELECT a FROM Announcement a JOIN FETCH a.createdBy WHERE a.event.id = :eventId ORDER BY a.createdAt DESC")
    List<Announcement> findByEventIdOrderByCreatedAtDesc(@Param("eventId") UUID eventId);

    Page<Announcement> findByEventIdOrderByCreatedAtDesc(UUID eventId, Pageable pageable);
}
