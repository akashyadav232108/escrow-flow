package com.escrowflow.service;

import com.escrowflow.domain.Notification;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.NotificationReferenceType;
import com.escrowflow.domain.enums.NotificationType;
import com.escrowflow.repository.NotificationRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.web.dto.NotificationListResponse;
import com.escrowflow.web.dto.NotificationResponse;
import com.escrowflow.web.dto.UnreadCountResponse;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Notification notify(
            Long userId,
            NotificationType type,
            String title,
            String message,
            NotificationReferenceType referenceType,
            Long referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .read(false)
                .build());

        log.info("Notification created: id={} userId={} type={}", notification.getId(), userId, type);
        return notification;
    }

    @Transactional
    public List<Notification> notifyMany(
            Collection<Long> userIds,
            NotificationType type,
            String title,
            String message,
            NotificationReferenceType referenceType,
            Long referenceId) {
        Set<Long> uniqueIds = new LinkedHashSet<>(userIds);
        return uniqueIds.stream()
                .map(userId -> notify(userId, type, title, message, referenceType, referenceId))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationListResponse listForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notificationPage =
                notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        List<NotificationResponse> content = notificationPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new NotificationListResponse(
                content,
                notificationPage.getNumber(),
                notificationPage.getSize(),
                notificationPage.getTotalElements(),
                notificationPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    @Transactional
    public UnreadCountResponse markAllRead(Long userId) {
        int updated = notificationRepository.markAllReadForUser(userId);
        log.info("Marked notifications read: userId={} count={}", userId, updated);
        return new UnreadCountResponse(0);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
