package com.civicpulse.modules.discussion.repository;

import com.civicpulse.modules.discussion.model.DiscussionPost;
import com.civicpulse.modules.discussion.model.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DiscussionPostRepository extends JpaRepository<DiscussionPost, UUID> {

    @Query("SELECT p FROM DiscussionPost p JOIN FETCH p.user WHERE p.event.id = :eventId AND p.parentPost IS NULL AND p.status != 'REMOVED' ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<DiscussionPost> findRootPostsByEventId(@Param("eventId") UUID eventId, Pageable pageable);

    @Query("SELECT p FROM DiscussionPost p JOIN FETCH p.user WHERE p.parentPost.id = :parentPostId AND p.status != 'REMOVED' ORDER BY p.createdAt ASC")
    List<DiscussionPost> findRepliesByParentPostId(@Param("parentPostId") UUID parentPostId);

    long countByEventIdAndStatusNot(UUID eventId, PostStatus status);
}
