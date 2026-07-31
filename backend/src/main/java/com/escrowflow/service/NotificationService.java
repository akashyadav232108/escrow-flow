package com.escrowflow.service;

import com.escrowflow.domain.Notification;
import com.escrowflow.domain.User;
import com.escrowflow.domain.enums.NotificationReferenceType;
import com.escrowflow.domain.enums.NotificationType;
import com.escrowflow.repository.NotificationRepository;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
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
}
