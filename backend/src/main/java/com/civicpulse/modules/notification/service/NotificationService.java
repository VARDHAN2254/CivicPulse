package com.civicpulse.modules.notification.service;

import com.civicpulse.common.exception.ResourceNotFoundException;
import com.civicpulse.common.response.PagedResponse;
import com.civicpulse.modules.notification.dto.NotificationDto;
import com.civicpulse.modules.notification.model.Notification;
import com.civicpulse.modules.notification.model.NotificationType;
import com.civicpulse.modules.notification.repository.NotificationRepository;
import com.civicpulse.modules.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public NotificationDto createAndSendNotification(User user, NotificationType type, String title, String message, String linkUrl) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .linkUrl(linkUrl)
                .read(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        NotificationDto dto = NotificationDto.fromEntity(saved);

        // Real-time WebSocket delivery
        try {
            String destination = "/topic/notifications/" + user.getId();
            messagingTemplate.convertAndSend(destination, dto);
            log.debug("Dispatched real-time notification to user [{}] over [{}]", user.getEmail(), destination);
        } catch (Exception e) {
            log.warn("Failed to push real-time WebSocket notification to user [{}]: {}", user.getId(), e.getMessage());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public PagedResponse<NotificationDto> getUserNotifications(UUID userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.from(page.map(NotificationDto::fromEntity));
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationDto markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);
        return NotificationDto.fromEntity(updated);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
