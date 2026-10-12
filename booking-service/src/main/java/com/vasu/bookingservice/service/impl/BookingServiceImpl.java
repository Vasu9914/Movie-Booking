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
import com.vasu.bookingservice.exception.ResourceNotFoundException;
import com.vasu.bookingservice.exception.SeatUnavailableException;
import com.vasu.bookingservice.mapper.BookingMapper;
import com.vasu.bookingservice.repository.BookingRepository;
import com.vasu.bookingservice.repository.BookingSeatRepository;
import com.vasu.bookingservice.repository.SeatHoldRepository;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.BookingService;
import com.vasu.bookingservice.service.SeatHoldRedisService;
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
    private final SeatHoldRedisService seatHoldRedisService;

    @Value("${movie.service.url}")
    private String movieServiceUrl;

    @Value("${booking.seat-hold.ttl:900}")
    private long seatHoldTtlSeconds;

    public BookingServiceImpl(ShowSeatRepository showSeatRepository,
                              BookingRepository bookingRepository,
                              BookingSeatRepository bookingSeatRepository,
                              SeatHoldRepository seatHoldRepository,
                              BookingMapper bookingMapper,
                              @Lazy BookingEventProcessor bookingEventProcessor,
                              RestTemplate restTemplate,
                              SeatHoldRedisService seatHoldRedisService) {
        this.showSeatRepository = showSeatRepository;
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.bookingMapper = bookingMapper;
        this.bookingEventProcessor = bookingEventProcessor;
        this.restTemplate = restTemplate;
        this.seatHoldRedisService = seatHoldRedisService;
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

        // Store in Redis with TTL for automatic expiry
        seatHoldRedisService.storeHold(holdReference, userId, showId, seatIds, seatHoldTtlSeconds);

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

        // Also verify the hold is still active in Redis (TTL-based check)
        if (!seatHoldRedisService.isHoldActive(request.getHoldReference())) {
            // Redis TTL expired - mark DB hold as expired and release seats
            seatHoldRepository.updateStatusByHoldReference(request.getHoldReference(), HoldStatus.EXPIRED);
            List<Long> seatIds = seatHold.getShowSeatIds();
            if (!seatIds.isEmpty()) {
                showSeatRepository.releaseSeats(seatIds);
            }
            throw new SeatUnavailableException("Hold has expired (session timeout)");
        }

        // Verify that the seat IDs match the hold
        if (!seatHold.getShowSeatIds().containsAll(request.getSeatIds())
                || !request.getSeatIds().containsAll(seatHold.getShowSeatIds())) {
            throw new SeatUnavailableException("Seat IDs do not match the hold");
        }

        // Mark the hold as consumed
        seatHoldRepository.updateStatusByHoldReference(request.getHoldReference(), HoldStatus.CONSUMED);

        // Remove from Redis since the hold is consumed
        seatHoldRedisService.releaseHold(request.getHoldReference());

        String bookingReference = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

        List<ShowSeat> seats = new ArrayList<>();
        for (Long seatId : request.getSeatIds()) {
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
                .showId(request.getShowId())
                .status(BookingStatus.PENDING_PAYMENT)
                .totalAmount(totalAmount)
                .currency(request.getCurrency())
                .expiresAt(expiresAt)
                .build();
        Booking savedBooking = bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = seats.stream()
                .map(seat -> BookingSeat.builder()
                        .bookingId(savedBooking.getId())
                        .showSeatId(seat.getId())
                        .price(seat.getPrice())
                        .build())
                .toList();
        bookingSeatRepository.saveAll(bookingSeats);

        int booked = showSeatRepository.bookSeats(request.getSeatIds(), savedBooking.getId());
        if (booked != seats.size()) {
            throw new SeatUnavailableException("Failed to book all seats");
        }

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

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking updated = bookingRepository.save(booking);
        return bookingMapper.toBookingResponse(updated);
    }

    @Override
    public void cancelBooking(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingReference));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Confirmed bookings cannot be cancelled directly");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        List<Long> seatIds = bookingSeats.stream().map(BookingSeat::getShowSeatId).toList();
        if (!seatIds.isEmpty()) {
            showSeatRepository.releaseSeats(seatIds);
        }

        bookingEventProcessor.publishBookingCancelled(
                BookingCancelledEvent.builder()
                        .bookingReference(bookingReference)
                        .userId(booking.getUserId())
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

        // Remove from Redis
        seatHoldRedisService.releaseHold(holdReference);
    }

    @Override
    public List<com.vasu.bookingservice.dto.response.UserHoldInfo> getUserActiveHolds(Long userId) {
        return seatHoldRedisService.getUserHoldDetails(userId);
    }

    @Override
    public void releaseAllUserHolds(Long userId) {
        // Release all Redis holds first to get the hold references
        List<String> holdRefs = seatHoldRedisService.getUserActiveHolds(userId);

        // Release each hold in the database as well
        for (String holdRef : holdRefs) {
            seatHoldRepository.updateStatusByHoldReference(holdRef, HoldStatus.RELEASED);
            List<Long> seatIds = seatHoldRedisService.getHoldSeatIds(holdRef);
            if (!seatIds.isEmpty()) {
                showSeatRepository.releaseSeats(seatIds);
            }
        }

        // Release all from Redis
        seatHoldRedisService.releaseAllUserHolds(userId);
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
