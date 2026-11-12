package com.vasu.bookingservice.repository;

import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.enums.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Slice<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);

    @Modifying
    @Query("UPDATE Booking b SET b.status = :newStatus WHERE b.id = :id AND b.status = :expectedStatus")
    int updateStatusIfMatches(@Param("id") Long id, @Param("expectedStatus") BookingStatus expectedStatus, @Param("newStatus") BookingStatus newStatus);

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime timestamp);

    List<Booking> findByStatus(BookingStatus status);
}
