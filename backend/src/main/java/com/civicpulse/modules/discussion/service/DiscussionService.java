package com.civicpulse.modules.discussion.service;

import com.civicpulse.common.exception.BadRequestException;
import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.config.KafkaConfig;
import com.civicpulse.modules.discussion.dto.CreatePostRequest;
import com.civicpulse.modules.discussion.dto.DiscussionPostDto;
import com.civicpulse.modules.discussion.dto.ReportPostRequest;
import com.civicpulse.modules.discussion.model.DiscussionPost;
import com.civicpulse.modules.discussion.model.ModerationFlag;
import com.civicpulse.modules.discussion.model.PostStatus;
import com.civicpulse.modules.discussion.repository.DiscussionPostRepository;
import com.civicpulse.modules.discussion.repository.ModerationFlagRepository;
import com.civicpulse.modules.event.model.Event;
import com.civicpulse.modules.event.repository.EventRepository;
import com.civicpulse.modules.messaging.event.DomainEvent;
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionPostRepository postRepository;
    private final ModerationFlagRepository moderationFlagRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ProfanityFilterService profanityFilterService;
    private final KafkaEventPublisher kafkaEventPublisher;

    @Transactional
    public DiscussionPostDto createPost(UUID userId, UUID eventId, CreatePostRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        DiscussionPost parentPost = null;
        if (request.getParentPostId() != null) {
            parentPost = postRepository.findById(request.getParentPostId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent post not found"));
        }

        // Automated Content & Profanity Filter
        boolean isToxic = profanityFilterService.containsInappropriateContent(request.getContent());
        PostStatus initialStatus = isToxic ? PostStatus.FLAGGED : PostStatus.ACTIVE;

        DiscussionPost post = DiscussionPost.builder()
                .event(event)
                .user(user)
                .parentPost(parentPost)
                .content(request.getContent().trim())
                .status(initialStatus)
                .pinned(false)
                .build();

        DiscussionPost saved = postRepository.save(post);
        log.info("Created discussion post ID [{}] on event [{}] by [{}] (Status: {})",
                saved.getId(), event.getTitle(), user.getEmail(), initialStatus);

        if (isToxic) {
            // Automatically log a moderation flag
            ModerationFlag autoFlag = ModerationFlag.builder()
                    .post(saved)
                    .reportedBy(user)
                    .reason("Automated AI/Keyword Profanity Filter Detection")
                    .status("PENDING")
                    .build();
            moderationFlagRepository.save(autoFlag);

            DomainEvent<String> domainEvent = DomainEvent.of(
                    "CONTENT_FLAGGED", "POST", saved.getId().toString(), "Automated filter flag"
            );
            kafkaEventPublisher.publish(KafkaConfig.TOPIC_MODERATION, saved.getId().toString(), domainEvent);
        }

        return DiscussionPostDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<DiscussionPostDto> getEventDiscussion(UUID eventId, Pageable pageable) {
        Page<DiscussionPost> rootPosts = postRepository.findRootPostsByEventId(eventId, pageable);

        Page<DiscussionPostDto> dtos = rootPosts.map(post -> {
            DiscussionPostDto dto = DiscussionPostDto.fromEntity(post);
            List<DiscussionPost> replies = postRepository.findRepliesByParentPostId(post.getId());
            dto.setReplies(replies.stream().map(DiscussionPostDto::fromEntity).collect(Collectors.toList()));
            return dto;
        });

        return PagedResponse.from(dtos);
    }

    @Transactional
    public DiscussionPostDto togglePin(UUID postId) {
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        post.setPinned(!post.isPinned());
        DiscussionPost updated = postRepository.save(post);
        log.info("Toggled pin state for post [{}] to: {}", postId, updated.isPinned());
        return DiscussionPostDto.fromEntity(updated);
    }

    @Transactional
    public void deletePost(UUID currentUserId, UUID postId) {
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        // Soft remove
        post.setStatus(PostStatus.REMOVED);
        post.setContent("[This post has been deleted by author or moderator]");
        postRepository.save(post);
        log.info("Post ID [{}] marked as REMOVED by user ID [{}]", postId, currentUserId);
    }

    @Transactional
    public void reportPost(UUID reporterUserId, UUID postId, ReportPostRequest request) {
        User reporter = userRepository.findById(reporterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reporter not found"));

        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        ModerationFlag flag = ModerationFlag.builder()
                .post(post)
                .reportedBy(reporter)
                .reason(request.getReason().trim())
                .status("PENDING")
                .build();

        moderationFlagRepository.save(flag);
        log.info("Report filed on post [{}] by user [{}]: {}", postId, reporter.getEmail(), request.getReason());

        DomainEvent<String> domainEvent = DomainEvent.of(
                "USER_REPORTED_POST", "POST", postId.toString(), request.getReason()
        );
        kafkaEventPublisher.publish(KafkaConfig.TOPIC_MODERATION, postId.toString(), domainEvent);
    }
}
