package com.civicpulse.modules.discussion.repository;

import com.civicpulse.modules.discussion.model.ModerationFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ModerationFlagRepository extends JpaRepository<ModerationFlag, UUID> {

    @Query("SELECT f FROM ModerationFlag f JOIN FETCH f.post p JOIN FETCH p.user JOIN FETCH f.reportedBy WHERE f.status = 'PENDING' ORDER BY f.createdAt DESC")
    Page<ModerationFlag> findPendingFlags(Pageable pageable);

    long countByStatus(String status);
}
