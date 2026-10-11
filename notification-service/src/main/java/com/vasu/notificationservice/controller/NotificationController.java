package com.vasu.notificationservice.controller;

import com.vasu.notificationservice.dto.common.PageResponse;
import com.vasu.notificationservice.dto.response.NotificationResponse;
import com.vasu.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/api/notifications/me")
    public PageResponse<NotificationResponse> getUserNotifications(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return notificationService.getUserNotifications(userId, pageable);
    }

    @GetMapping("/api/notifications/{notificationId}")
    public NotificationResponse getNotification(@PathVariable Long notificationId) {
        return notificationService.getNotificationById(notificationId);
    }

    @PatchMapping("/api/notifications/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @PatchMapping("/api/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@RequestParam Long userId) {
        notificationService.markAllAsRead(userId);
    }
}
