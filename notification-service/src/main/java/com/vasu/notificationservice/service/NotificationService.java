package com.vasu.notificationservice.service;

import com.vasu.notificationservice.dto.common.PageResponse;
import com.vasu.notificationservice.dto.response.NotificationResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);

    NotificationResponse getNotificationById(Long id);

    void markAsRead(Long id);

    void markAllAsRead(Long userId);

    void createNotification(Long userId, String bookingReference, String channel, String recipient, String eventId);
}
