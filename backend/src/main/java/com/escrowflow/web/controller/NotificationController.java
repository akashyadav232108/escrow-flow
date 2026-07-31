package com.escrowflow.web.controller;

import com.escrowflow.security.SecurityUtils;
import com.escrowflow.service.NotificationService;
import com.escrowflow.web.dto.NotificationListResponse;
import com.escrowflow.web.dto.NotificationResponse;
import com.escrowflow.web.dto.UnreadCountResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return notificationService.listForUser(SecurityUtils.getCurrentUserId(), page, size);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return notificationService.unreadCount(SecurityUtils.getCurrentUserId());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(SecurityUtils.getCurrentUserId(), id);
    }

    @PostMapping("/read-all")
    public UnreadCountResponse markAllRead() {
        return notificationService.markAllRead(SecurityUtils.getCurrentUserId());
    }
}
