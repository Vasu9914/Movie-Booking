package com.vasu.bookingservice.repository;

import com.vasu.bookingservice.entity.SeatHold;
import com.vasu.bookingservice.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    Optional<SeatHold> findByHoldReferenceAndStatus(String holdReference, HoldStatus status);

    List<SeatHold> findByStatus(HoldStatus status);

    @Modifying
    @Query("UPDATE SeatHold h SET h.status = :status WHERE h.holdReference = :holdReference AND h.status = HoldStatus.ACTIVE")
    int updateStatusByHoldReference(@Param("holdReference") String holdReference, @Param("status") HoldStatus status);
}
