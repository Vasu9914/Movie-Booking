package com.vasu.bookingservice.entity;

import com.vasu.bookingservice.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "seat_holds")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SeatHold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hold_reference", nullable = false, length = 36, unique = true)
    private String holdReference;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @ElementCollection
    @CollectionTable(name = "seat_hold_seats", joinColumns = @JoinColumn(name = "hold_id"))
    @Column(name = "show_seat_id")
    private List<Long> showSeatIds;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
