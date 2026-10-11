package com.vasu.bookingservice.repository;

import com.vasu.bookingservice.entity.ShowSeat;
import com.vasu.bookingservice.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    List<ShowSeat> findByShowId(Long showId);

    List<ShowSeat> findByShowIdAndStatus(Long showId, SeatStatus status);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = :status, s.reservedUntil = :reservedUntil WHERE s.id IN :ids AND s.status = com.vasu.bookingservice.enums.SeatStatus.AVAILABLE")
    int holdSeats(@Param("ids") List<Long> ids, @Param("status") SeatStatus status, @Param("reservedUntil") LocalDateTime reservedUntil);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = com.vasu.bookingservice.enums.SeatStatus.AVAILABLE, s.reservedUntil = null WHERE s.id IN :ids AND s.status = com.vasu.bookingservice.enums.SeatStatus.HELD")
    int releaseSeats(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = com.vasu.bookingservice.enums.SeatStatus.BOOKED, s.bookingId = :bookingId WHERE s.id IN :ids AND s.status = com.vasu.bookingservice.enums.SeatStatus.HELD")
    int bookSeats(@Param("ids") List<Long> ids, @Param("bookingId") Long bookingId);

    @Modifying
    @Query("UPDATE ShowSeat s SET s.status = com.vasu.bookingservice.enums.SeatStatus.AVAILABLE, s.reservedUntil = null WHERE s.status = com.vasu.bookingservice.enums.SeatStatus.HELD AND s.reservedUntil < CURRENT_TIMESTAMP")
    int expireHolds();
}
