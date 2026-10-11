package com.vasu.notificationservice.service.impl;

import com.vasu.notificationservice.dto.common.PageResponse;
import com.vasu.notificationservice.dto.response.NotificationResponse;
import com.vasu.notificationservice.entity.Notification;
import com.vasu.notificationservice.enums.NotificationStatus;
import com.vasu.notificationservice.exception.ResourceNotFoundException;
import com.vasu.notificationservice.mapper.NotificationMapper;
import com.vasu.notificationservice.repository.NotificationRepository;
import com.vasu.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Slice<Notification> slice = notificationRepository.findByUserId(userId, pageable);
        return PageResponse.<NotificationResponse>builder()
                .content(slice.getContent().stream().map(notificationMapper::toNotificationResponse).toList())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        return notificationMapper.toNotificationResponse(notification);
    }

    @Override
    public void markAsRead(Long id) {
        int updated = notificationRepository.markAsRead(id);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found: " + id);
        }
    }

    @Override
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    public void createNotification(Long userId, String bookingReference, String channel, String recipient, String eventId) {
        Notification notification = Notification.builder()
                .eventId(eventId != null ? eventId : UUID.randomUUID().toString())
                .userId(userId)
                .bookingReference(bookingReference)
                .channel(channel)
                .recipient(recipient)
                .status(NotificationStatus.PENDING)
                .attemptCount(0)
                .build();
        notificationRepository.save(notification);
    }
}
