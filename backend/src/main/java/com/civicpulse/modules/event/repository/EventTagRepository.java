package com.civicpulse.modules.event.repository;

import com.civicpulse.modules.event.model.EventTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventTagRepository extends JpaRepository<EventTag, UUID> {

    Optional<EventTag> findByNameIgnoreCase(String name);

    Optional<EventTag> findBySlug(String slug);
}
