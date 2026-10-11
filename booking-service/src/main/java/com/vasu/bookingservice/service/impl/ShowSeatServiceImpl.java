package com.vasu.bookingservice.service.impl;

import com.vasu.bookingservice.dto.request.ShowSeatCreateRequest;
import com.vasu.bookingservice.dto.response.SeatResponse;
import com.vasu.bookingservice.dto.response.ShowSeatSummaryResponse;
import com.vasu.bookingservice.entity.ShowSeat;
import com.vasu.bookingservice.enums.SeatStatus;
import com.vasu.bookingservice.exception.ResourceNotFoundException;
import com.vasu.bookingservice.mapper.BookingMapper;
import com.vasu.bookingservice.repository.ShowSeatRepository;
import com.vasu.bookingservice.service.ShowSeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShowSeatServiceImpl implements ShowSeatService {

    private final ShowSeatRepository showSeatRepository;
    private final BookingMapper bookingMapper;

    @Override
    public List<SeatResponse> createShowSeats(ShowSeatCreateRequest request) {
        Long showId = request.getShowId();

        // Check if seats already exist for this show
        long existingCount = showSeatRepository.findByShowId(showId).size();
        if (existingCount > 0) {
            throw new IllegalArgumentException(
                "Show seats already exist for show ID " + showId +
                " (" + existingCount + " seats found). Delete existing seats first if you want to recreate them."
            );
        }

        List<ShowSeat> showSeats = request.getSeats().stream()
            .map(item -> ShowSeat.builder()
                .showId(showId)
                .screenSeatId(item.getScreenSeatId())
                .price(item.getPrice())
                .status(SeatStatus.AVAILABLE)
                .reservedUntil(null)
                .bookingId(null)
                .version(0L)
                .build())
            .toList();

        List<ShowSeat> saved = showSeatRepository.saveAll(showSeats);
        log.info("Created {} show seats for show ID {}", saved.size(), showId);

        return bookingMapper.toSeatResponseList(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowSeatSummaryResponse getShowSeatSummary(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("Show seats", showId);
        }

        long available = seats.stream().filter(s -> s.getStatus() == SeatStatus.AVAILABLE).count();
        long held = seats.stream().filter(s -> s.getStatus() == SeatStatus.HELD).count();
        long booked = seats.stream().filter(s -> s.getStatus() == SeatStatus.BOOKED).count();

        return ShowSeatSummaryResponse.builder()
            .showId(showId)
            .totalSeats(seats.size())
            .availableSeats((int) available)
            .heldSeats((int) held)
            .bookedSeats((int) booked)
            .build();
    }

    @Override
    public void deleteShowSeatsByShowId(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        if (seats.isEmpty()) {
            throw new ResourceNotFoundException("Show seats", showId);
        }

        showSeatRepository.deleteAll(seats);
        log.info("Deleted {} show seats for show ID {}", seats.size(), showId);
    }
}
