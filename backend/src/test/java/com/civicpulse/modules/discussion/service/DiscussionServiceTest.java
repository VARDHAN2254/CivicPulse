package com.civicpulse.modules.discussion.service;

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
import com.civicpulse.modules.messaging.producer.KafkaEventPublisher;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import com.civicpulse.modules.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceTest {

    @Mock
    private DiscussionPostRepository postRepository;

    @Mock
    private ModerationFlagRepository moderationFlagRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ProfanityFilterService profanityFilterService = new ProfanityFilterService();

    @Mock
    private KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private DiscussionService discussionService;

    private User sampleUser;
    private Event sampleEvent;
    private DiscussionPost samplePost;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("volunteer@civicpulse.org")
                .fullName("Alex Rivera")
                .role(UserRole.MEMBER)
                .build();

        sampleEvent = Event.builder()
                .id(UUID.randomUUID())
                .title("Clean Rivers Community Clean-Up")
                .build();

        samplePost = DiscussionPost.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .user(sampleUser)
                .content("Looking forward to joining the team on Saturday!")
                .status(PostStatus.ACTIVE)
                .pinned(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should successfully create a clean root discussion post")
    void createPost_CleanContent_Success() {
        CreatePostRequest request = CreatePostRequest.builder()
                .content("Looking forward to joining the team on Saturday!")
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(eventRepository.findById(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(postRepository.save(any(DiscussionPost.class))).thenReturn(samplePost);

        DiscussionPostDto result = discussionService.createPost(sampleUser.getId(), sampleEvent.getId(), request);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(request.getContent());
        assertThat(result.getStatus()).isEqualTo(PostStatus.ACTIVE);
        verify(postRepository, times(1)).save(any(DiscussionPost.class));
    }

    @Test
    @DisplayName("Should automatically flag post containing toxic/prohibited keywords")
    void createPost_ToxicContent_AutoFlags() {
        CreatePostRequest request = CreatePostRequest.builder()
                .content("Check out this free crypto scam deal!")
                .build();

        DiscussionPost flaggedPost = DiscussionPost.builder()
                .id(UUID.randomUUID())
                .event(sampleEvent)
                .user(sampleUser)
                .content(request.getContent())
                .status(PostStatus.FLAGGED)
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(eventRepository.findById(sampleEvent.getId())).thenReturn(Optional.of(sampleEvent));
        when(postRepository.save(any(DiscussionPost.class))).thenReturn(flaggedPost);

        DiscussionPostDto result = discussionService.createPost(sampleUser.getId(), sampleEvent.getId(), request);

        assertThat(result.getStatus()).isEqualTo(PostStatus.FLAGGED);
        verify(moderationFlagRepository, times(1)).save(any(ModerationFlag.class));
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should toggle post pinned state")
    void togglePin_Success() {
        when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(DiscussionPost.class))).thenReturn(samplePost);

        DiscussionPostDto result = discussionService.togglePin(samplePost.getId());

        assertThat(result).isNotNull();
        assertThat(samplePost.isPinned()).isTrue();
    }

    @Test
    @DisplayName("Should report post for moderator review")
    void reportPost_Success() {
        ReportPostRequest request = ReportPostRequest.builder()
                .reason("Inappropriate advertisement")
                .build();

        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(postRepository.findById(samplePost.getId())).thenReturn(Optional.of(samplePost));

        discussionService.reportPost(sampleUser.getId(), samplePost.getId(), request);

        verify(moderationFlagRepository, times(1)).save(any(ModerationFlag.class));
        verify(kafkaEventPublisher, times(1)).publish(anyString(), anyString(), any());
    }
}
