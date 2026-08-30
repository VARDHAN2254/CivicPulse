package com.civicpulse.modules.discussion.service;

import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.discussion.dto.ModerationFlagDto;
import com.civicpulse.modules.discussion.dto.ResolveFlagRequest;
import com.civicpulse.modules.discussion.model.DiscussionPost;
import com.civicpulse.modules.discussion.model.ModerationFlag;
import com.civicpulse.modules.discussion.model.PostStatus;
import com.civicpulse.modules.discussion.repository.DiscussionPostRepository;
import com.civicpulse.modules.discussion.repository.ModerationFlagRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModerationService {

    private final ModerationFlagRepository moderationFlagRepository;
    private final DiscussionPostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ModerationFlagDto> getPendingFlags(Pageable pageable) {
        Page<ModerationFlag> flags = moderationFlagRepository.findPendingFlags(pageable);
        return PagedResponse.from(flags.map(ModerationFlagDto::fromEntity));
    }

    @Transactional
    public void resolveFlag(UUID moderatorUserId, UUID flagId, ResolveFlagRequest request) {
        User moderator = userRepository.findById(moderatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderator not found"));

        ModerationFlag flag = moderationFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException("Moderation flag not found"));

        flag.setReviewedBy(moderator);
        flag.setResolutionNotes(request.getResolutionNotes());

        if ("REMOVE_POST".equalsIgnoreCase(request.getAction())) {
            flag.setStatus("ACTIONED");
            DiscussionPost post = flag.getPost();
            post.setStatus(PostStatus.REMOVED);
            post.setContent("[This post has been removed by platform moderators for violating community guidelines]");
            postRepository.save(post);
            log.info("Moderator [{}] removed flagged post ID [{}]", moderator.getEmail(), post.getId());
        } else {
            flag.setStatus("DISMISSED");
            DiscussionPost post = flag.getPost();
            if (post.getStatus() == PostStatus.FLAGGED) {
                post.setStatus(PostStatus.ACTIVE);
                postRepository.save(post);
            }
            log.info("Moderator [{}] dismissed flag ID [{}]", moderator.getEmail(), flag.getId());
        }

        moderationFlagRepository.save(flag);
    }
}
