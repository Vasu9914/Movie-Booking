package com.vasu.paymentservice.repository;

import com.vasu.paymentservice.entity.Payment;
import com.vasu.paymentservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentReference(String paymentReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByBookingReference(String bookingReference);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime threshold);

    @Modifying
    @Query("""
        update Payment p
           set p.status = com.vasu.paymentservice.enums.PaymentStatus.EXPIRED
         where p.id = :id
           and p.status = com.vasu.paymentservice.enums.PaymentStatus.PENDING
    """)
    int expireIfPending(@Param("id") Long id);

}
