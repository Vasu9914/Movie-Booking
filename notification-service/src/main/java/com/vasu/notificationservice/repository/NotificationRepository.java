package com.vasu.notificationservice.repository;

import com.vasu.notificationservice.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Slice<Notification> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.sentAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.status <> 'READ'")
    int markAsRead(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.sentAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.status <> 'READ'")
    int markAllAsRead(@Param("userId") Long userId);

    List<Notification> findByBookingReference(String bookingReference);
}
