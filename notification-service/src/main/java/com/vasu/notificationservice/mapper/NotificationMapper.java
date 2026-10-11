package com.vasu.notificationservice.mapper;

import com.vasu.notificationservice.dto.response.NotificationResponse;
import com.vasu.notificationservice.entity.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toNotificationResponse(Notification notification);
    List<NotificationResponse> toNotificationResponseList(List<Notification> notifications);
}
