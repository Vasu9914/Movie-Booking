package com.vasu.bookingservice.service.impl;

import com.vasu.bookingservice.dto.common.PageResponse;
import com.vasu.bookingservice.dto.request.BookingCreateRequest;
import com.vasu.bookingservice.dto.request.BookingHoldRequest;
import com.vasu.bookingservice.dto.request.SeatHoldRequest;
import com.vasu.bookingservice.dto.response.BookingResponse;
import com.vasu.bookingservice.dto.response.BookingSeatResponse;
import com.vasu.bookingservice.dto.response.HoldResponse;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.entity.Booking;
import com.vasu.bookingservice.entity.BookingSeat;
import com.vasu.bookingservice.entity.SeatHold;
import com.vasu.bookingservice.entity.ShowSeat;
import com.vasu.bookingservice.enums.BookingStatus;
import com.vasu.bookingservice.enums.HoldStatus;
import com.vasu.bookingservice.enums.SeatStatus;
import com.vasu.bookingservice.event.BookingCancelledEvent;
import com.vasu.bookingservice.event.BookingEventProcessor;
import com.vasu.bookingservice.event.BookingRefundedEvent;
import com.vasu.bookingservice.exception.ResourceNotFoundException;
import com.vasu.bookingservice.exception.SeatUnavailableException;
import com.vasu.bookingservice.mapper.BookingMapper;
import com.vasu.bookingservice.repository.BookingRepository;
import com.vasu.bookingservice.repository.BookingSeatRepository;
import com.vasu.bookingservice.repository.SeatHoldRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class BookingServiceImpl implements BookingService {

    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BookingMapper bookingMapper;
    private final BookingEventProcessor bookingEventProcessor;
    private final RestTemplate restTemplate;

    @Value("${movie.service.url}")
    private String movieServiceUrl;

    @Value("${payment.service.url:http://localhost:8084}")
    private String paymentServiceUrl;

    @Value("${booking.seat-hold.ttl:900}")
    private long seatHoldTtlSeconds;

    public BookingServiceImpl(ShowSeatRepository showSeatRepository,
                              BookingRepository bookingRepository,
                              BookingSeatRepository bookingSeatRepository,
                              SeatHoldRepository seatHoldRepository,
                              BookingMapper bookingMapper,
                              @Lazy BookingEventProcessor bookingEventProcessor,
                              RestTemplate restTemplate) {
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.bookingMapper = bookingMapper;
        this.bookingEventProcessor = bookingEventProcessor;
        this.restTemplate = restTemplate;
    }

    private static final int HOLD_MINUTES = 15;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByShowId(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        return bookingMapper.toSeatResponseList(seats);
    }

    @Override
    public HoldResponse holdSeats(Long showId, SeatHoldRequest request) {
        return holdSeatsInternal(showId, request.getSeatIds(), null);
    }

    @Override
    public HoldResponse holdSeats(BookingHoldRequest request) {
        return holdSeatsInternal(request.getShowId(), request.getSeatIds(), request.getUserId());
    }

    private HoldResponse holdSeatsInternal(Long showId, List<Long> seatIds, Long userId) {
        LocalDateTime reservedUntil = LocalDateTime.now().plusMinutes(HOLD_MINUTES);

        int updated = showSeatRepository.holdSeats(seatIds, SeatStatus.HELD, reservedUntil);
        if (updated != seatIds.size()) {
            throw new SeatUnavailableException("One or more seats are no longer available");
        }

        List<ShowSeat> seats = new ArrayList<>();
        for (Long id : seatIds) {
            ShowSeat seat = showSeatRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("ShowSeat", id));
            seats.add(seat);
        }

        BigDecimal totalAmount = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String holdReference = UUID.randomUUID().toString();

        // Persist the hold record so it can be used later when creating a booking
        SeatHold seatHold = SeatHold.builder()
                .holdReference(holdReference)
                .userId(userId)
                .showId(showId)
                .showSeatIds(seatIds)
                .totalAmount(totalAmount)
                .expiresAt(reservedUntil)
                .status(HoldStatus.ACTIVE)
                .build();
        seatHoldRepository.save(seatHold);

        return HoldResponse.builder()
                .holdReference(holdReference)
                .seats(bookingMapper.toSeatResponseList(seats))
                .expiresAt(reservedUntil)
                .totalAmount(totalAmount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public HoldResponse getHoldStatus(String holdReference) {
        SeatHold seatHold = seatHoldRepository.findByHoldReferenceAndStatus(holdReference, HoldStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Hold", holdReference));

        List<ShowSeat> seats = new ArrayList<>();
        for (Long seatId : seatHold.getShowSeatIds()) {
            ShowSeat seat = showSeatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("ShowSeat", seatId));
            seats.add(seat);
        }

        return HoldResponse.builder()
                .holdReference(seatHold.getHoldReference())
                .seats(bookingMapper.toSeatResponseList(seats))
                .expiresAt(seatHold.getExpiresAt())
                .totalAmount(seatHold.getTotalAmount())
                .build();
    }

    @Override
    public BookingResponse createBooking(Long userId, BookingCreateRequest request) {
        // Validate that the hold exists and is active in the database
        SeatHold seatHold = seatHoldRepository.findByHoldReferenceAndStatus(
                        request.getHoldReference(), HoldStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active hold", request.getHoldReference()));

        // Check if the hold has expired in the database
        if (LocalDateTime.now().isAfter(seatHold.getExpiresAt())) {
            seatHoldRepository.updateStatusByHoldReference(request.getHoldReference(), HoldStatus.EXPIRED);
            throw new SeatUnavailableException("Hold has expired");
        }

        // Mark the hold as consumed
        seatHoldRepository.updateStatusByHoldReference(request.getHoldReference(), HoldStatus.CONSUMED);

        // Get seat IDs from the hold (no longer needed in the request)
        List<Long> seatIds = seatHold.getShowSeatIds();

        String bookingReference = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        List<ShowSeat> seats = new ArrayList<>();
        for (Long seatId : seatIds) {
            ShowSeat seat = showSeatRepository.findById(seatId)
                    .orElseThrow(() -> new ResourceNotFoundException("ShowSeat", seatId));
            if (seat.getStatus() != SeatStatus.HELD) {
                throw new SeatUnavailableException("Seat " + seatId + " is not held");
            }
            seats.add(seat);
        }

        BigDecimal totalAmount = seats.stream()
                .map(ShowSeat::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .userId(userId)
                .showId(seatHold.getShowId())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .currency(request.getCurrency())
                .expiresAt(expiresAt)
                .build();
        Booking savedBooking = bookingRepository.save(booking);

        // Set bookingId on seats immediately so expireHolds() won't release them
        showSeatRepository.assignBookingId(seatIds, savedBooking.getId());

        List<BookingSeat> bookingSeats = seats.stream()
                .map(seat -> BookingSeat.builder()
                        .bookingId(savedBooking.getId())
                        .showSeatId(seat.getId())
                        .price(seat.getPrice())
                        .build())
                .toList();
        bookingSeatRepository.saveAll(bookingSeats);

        // Seats remain HELD until payment is confirmed.
        // The confirmBooking() method will transition them to BOOKED via bookSeats().
        // If payment fails or expires, expireHolds() in the scheduler will release them.

        return bookingMapper.toBookingResponse(savedBooking);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", reference));
        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        Slice<Booking> slice = bookingRepository.findByUserId(userId, pageable);
        return PageResponse.<BookingResponse>builder()
                .content(slice.getContent().stream().map(bookingMapper::toBookingResponse).toList())
                .page(slice.getNumber())
                .size(slice.getSize())
                .first(slice.isFirst())
                .last(slice.isLast())
                .build();
    }

    @Override
    public BookingResponse confirmBooking(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        // Idempotent: if already confirmed, return the existing booking
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("Booking {} is already confirmed, returning existing booking", bookingReference);
            return bookingMapper.toBookingResponse(booking);
        }

        // If booking is CANCELLED, payment succeeded but booking was cancelled — trigger refund
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("Payment success for cancelled booking {}, triggering refund", bookingReference);
            refundPayment(bookingReference);
            booking.setStatus(BookingStatus.PAYMENT_FAILED);
            bookingRepository.save(booking);
            return bookingMapper.toBookingResponse(booking);
        }

        // Conditional update: only change status if it is still PENDING_PAYMENT
        int updated = bookingRepository.updateStatusIfMatches(
                booking.getId(), BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED);
        if (updated == 0) {
            log.info("Booking {} was not PENDING_PAYMENT anymore, skipping confirm", bookingReference);
            return bookingMapper.toBookingResponse(booking);
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        // Mark seats as BOOKED so they are no longer subject to expireHolds()
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Long> seatIds = bookingSeats.stream().map(BookingSeat::getShowSeatId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.bookSeats(seatIds, booking.getId());
            log.info("Marked {} seats as BOOKED for booking {}", seatIds.size(), bookingReference);
        }

        return bookingMapper.toBookingResponse(booking);
    }

    @Override
    public void cancelBookingForConsumer(String bookingReference, String correlationId) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        // Idempotent: if already cancelled, skip silently
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.info("Booking {} is already cancelled, skipping", bookingReference);
            return;
        }

        // If booking is CONFIRMED, log warning and return without cancelling
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Booking {} is already CONFIRMED, cannot cancel from consumer path", bookingReference);
            return;
        }

        // Conditional update: only change status if it matches current status
        int updated = bookingRepository.updateStatusIfMatches(
                booking.getId(), booking.getStatus(), BookingStatus.CANCELLED);
        if (updated == 0) {
            log.info("Booking {} status changed concurrently, skipping cancel", bookingReference);
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Long> seatIds = bookingSeats.stream().map(BookingSeat::getShowSeatId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.releaseSeats(seatIds);
        }

        bookingEventProcessor.publishBookingCancelled(
                BookingCancelledEvent.builder()
                        .bookingReference(bookingReference)
                        .userId(booking.getUserId())
                        .correlationId(correlationId)
                        .build()
        );
    }

    @Override
    public void cancelBooking(String bookingReference, String correlationId) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        // REST endpoint: strict check for already cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed bookings cannot be cancelled directly");
        }

        // Conditional update: only change status if it matches current status
        int updated = bookingRepository.updateStatusIfMatches(
                booking.getId(), booking.getStatus(), BookingStatus.CANCELLED);
        if (updated == 0) {
            log.info("Booking {} status changed concurrently, skipping cancel", bookingReference);
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Long> seatIds = bookingSeats.stream().map(BookingSeat::getShowSeatId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.releaseSeats(seatIds);
        }

        bookingEventProcessor.publishBookingCancelled(
                BookingCancelledEvent.builder()
                        .bookingReference(bookingReference)
                        .userId(booking.getUserId())
                        .correlationId(correlationId)
                        .build()
        );
    }

    @Override
    public void handleRefund(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        // Idempotent: if already refunded, skip silently
        if (booking.getStatus() == BookingStatus.REFUNDED) {
            log.info("Booking {} is already refunded, skipping", bookingReference);
            return;
        }

        // Conditional update: only change status if it is still CONFIRMED
        int updated = bookingRepository.updateStatusIfMatches(
                booking.getId(), BookingStatus.CONFIRMED, BookingStatus.REFUNDED);
        if (updated == 0) {
            log.info("Booking {} was not CONFIRMED anymore, skipping refund", bookingReference);
            return;
        }

        booking.setStatus(BookingStatus.REFUNDED);
        log.info("Booking {} marked as refunded", bookingReference);

        // Release the seats back to the AVAILABLE pool
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Long> seatIds = bookingSeats.stream().map(BookingSeat::getShowSeatId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.releaseBookedSeats(seatIds);
            log.info("Released {} seats for refunded booking {}", seatIds.size(), bookingReference);
        }

        // Publish refund event to notify other services
        bookingEventProcessor.publishBookingRefunded(
                BookingRefundedEvent.builder()
                        .bookingReference(bookingReference)
                        .userId(booking.getUserId())
                        .correlationId("refund-" + bookingReference)
                        .build()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingSeatResponse> getBookingSeats(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));
        List<BookingSeat> seats = bookingSeatRepository.findByBookingId(booking.getId());
        return bookingMapper.toBookingSeatResponseList(seats);
    }

    @Override
    public void releaseHold(String holdReference) {
        // Find the active hold
        SeatHold seatHold = seatHoldRepository.findByHoldReferenceAndStatus(
                        holdReference, HoldStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Active hold", holdReference));

        // Mark the hold as released
        seatHoldRepository.updateStatusByHoldReference(holdReference, HoldStatus.RELEASED);

        // Release the seats back to AVAILABLE
        List<Long> seatIds = seatHold.getShowSeatIds();
        if (!seatIds.isEmpty()) {
            showSeatRepository.releaseSeats(seatIds);
        }
    }

    @Override
    public List<com.vasu.bookingservice.dto.response.UserHoldInfo> getUserActiveHolds(Long userId) {
        List<SeatHold> activeHolds = seatHoldRepository.findByUserIdAndStatus(userId, HoldStatus.ACTIVE);
        List<com.vasu.bookingservice.dto.response.UserHoldInfo> result = new ArrayList<>();

        for (SeatHold hold : activeHolds) {
            // Skip expired holds
            if (LocalDateTime.now().isAfter(hold.getExpiresAt())) {
                seatHoldRepository.updateStatusByHoldReference(hold.getHoldReference(), HoldStatus.EXPIRED);
                continue;
            }

            long ttlSeconds = java.time.Duration.between(LocalDateTime.now(), hold.getExpiresAt()).getSeconds();

            result.add(com.vasu.bookingservice.dto.response.UserHoldInfo.builder()
                    .holdReference(hold.getHoldReference())
                    .showId(hold.getShowId())
                    .seatIds(hold.getShowSeatIds())
                    .totalAmount(hold.getTotalAmount())
                    .status(hold.getStatus().name())
                    .expiresAt(hold.getExpiresAt())
                    .ttlSeconds(ttlSeconds)
                    .build());
        }
        return result;
    }

    @Override
    public void releaseAllUserHolds(Long userId) {
        List<SeatHold> activeHolds = seatHoldRepository.findByUserIdAndStatus(userId, HoldStatus.ACTIVE);

        for (SeatHold hold : activeHolds) {
            seatHoldRepository.updateStatusByHoldReference(hold.getHoldReference(), HoldStatus.RELEASED);
            List<Long> seatIds = hold.getShowSeatIds();
            if (!seatIds.isEmpty()) {
                showSeatRepository.releaseSeats(seatIds);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasValidBookingForMovie(Long userId, Long movieId) {
        // Fetch all confirmed bookings for the user
        List<Booking> confirmedBookings = bookingRepository.findByUserIdAndStatusIn(
                userId, List.of(BookingStatus.CONFIRMED)
        );

        if (confirmedBookings.isEmpty()) {
            return false;
        }

        // For each booking, check if its show belongs to the requested movie
        for (Booking booking : confirmedBookings) {
            Long showId = booking.getShowId();
            Long resolvedMovieId = getMovieIdByShowId(showId);
            if (resolvedMovieId != null && resolvedMovieId.equals(movieId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Calls the Payment Service to refund a payment by booking reference.
     */
    private void refundPayment(String bookingReference) {
        try {
            String url = paymentServiceUrl + "/api/payments/by-booking/" + bookingReference;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> payment = response.getBody().get(0);
                String paymentReference = (String) payment.get("paymentReference");
                if (paymentReference != null) {
                    String refundUrl = paymentServiceUrl + "/api/payments/" + paymentReference + "/refunds";
                    Map<String, Object> refundRequest = Map.of(
                            "amount", payment.get("amount") != null ? new BigDecimal(payment.get("amount").toString()) : BigDecimal.ZERO,
                            "idempotencyKey", "refund-" + bookingReference + "-" + System.currentTimeMillis()
                    );
                    HttpEntity<Map<String, Object>> refundEntity = new HttpEntity<>(refundRequest, headers);
                    restTemplate.postForEntity(refundUrl, refundEntity, Map.class);
                    log.info("Refund initiated for booking {}: payment {}", bookingReference, paymentReference);
                }
            }
        } catch (Exception e) {
            log.error("Failed to refund payment for booking {}: {}", bookingReference, e.getMessage());
        }
    }

    /**
     * Calls the Payment Service to get payment status by booking reference.
     */
    private String getPaymentStatusByBookingReference(String bookingReference) {
        try {
            String url = paymentServiceUrl + "/api/payments/status/" + bookingReference;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("status");
            }
        } catch (Exception e) {
            log.error("Failed to get payment status for booking {}: {}", bookingReference, e.getMessage());
        }
        return "NOT_FOUND";
    }

    /**
     * Calls the Movie Service to resolve showId -> movieId.
     */
    private Long getMovieIdByShowId(Long showId) {
        try {
            String url = movieServiceUrl + "/api/shows/" + showId;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> movie = (Map<String, Object>) response.getBody().get("movie");
                if (movie != null) {
                    Object id = movie.get("id");
                    if (id instanceof Number) {
                        return ((Number) id).longValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch show {} from movie service: {}", showId, e.getMessage());
        }
        return null;
    }
}
