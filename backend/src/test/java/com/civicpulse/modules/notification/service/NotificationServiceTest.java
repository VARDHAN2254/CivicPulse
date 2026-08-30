package com.civicpulse.modules.notification.service;

import com.civicpulse.modules.notification.dto.NotificationDto;
import com.civicpulse.modules.notification.model.Notification;
import com.civicpulse.modules.notification.model.NotificationType;
import com.civicpulse.modules.notification.repository.NotificationRepository;
import com.civicpulse.modules.user.model.User;
import com.civicpulse.modules.user.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User sampleUser;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("alex@civicpulse.org")
                .fullName("Alex Rivera")
                .role(UserRole.MEMBER)
                .build();

        sampleNotification = Notification.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .type(NotificationType.REGISTRATION_CONFIRMED)
                .title("Registration Confirmed!")
                .message("You have secured your spot for the tree drive.")
                .read(false)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("Should save notification and dispatch real-time message to STOMP WebSocket")
    void createAndSendNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationDto result = notificationService.createAndSendNotification(
                sampleUser,
                NotificationType.REGISTRATION_CONFIRMED,
                "Registration Confirmed!",
                "You have secured your spot for the tree drive.",
                "/my-events"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Registration Confirmed!");
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/notifications/" + sampleUser.getId()), any(NotificationDto.class));
    }

    @Test
    @DisplayName("Should mark notification as read")
    void markAsRead_Success() {
        when(notificationRepository.findById(sampleNotification.getId())).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationDto result = notificationService.markAsRead(sampleUser.getId(), sampleNotification.getId());

        assertThat(result).isNotNull();
        assertThat(sampleNotification.isRead()).isTrue();
        assertThat(sampleNotification.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("Should return unread count")
    void getUnreadCount_Success() {
        when(notificationRepository.countByUserIdAndReadFalse(sampleUser.getId())).thenReturn(5L);

        long count = notificationService.getUnreadCount(sampleUser.getId());

        assertThat(count).isEqualTo(5L);
    }
}
