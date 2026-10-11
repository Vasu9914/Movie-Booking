package com.vasu.bookingservice.repository;

import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.enums.BookingStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    Slice<Booking> findByUserId(Long userId, Pageable pageable);

    List<Booking> findByUserIdAndStatusIn(Long userId, List<BookingStatus> statuses);
}
