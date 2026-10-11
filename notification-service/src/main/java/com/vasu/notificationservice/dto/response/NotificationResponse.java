package com.vasu.notificationservice.dto.response;

import com.vasu.notificationservice.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String eventId;
    private Long userId;
    private String bookingReference;
    private String channel;
    private String recipient;
    private NotificationStatus status;
    private Integer attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}
