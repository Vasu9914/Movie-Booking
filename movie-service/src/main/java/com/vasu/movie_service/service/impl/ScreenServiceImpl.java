package com.vasu.movie_service.service;

import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.ScreenUpdateRequest;
import com.vasu.movie_service.dto.request.SeatLayoutRequest;
import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.entity.ScreenSeat;
import com.vasu.movie_service.entity.Theatre;
import com.vasu.movie_service.enums.EntityStatus;
import com.vasu.movie_service.exception.DuplicateResourceException;
import com.vasu.movie_service.exception.ResourceNotFoundException;
import com.vasu.movie_service.mapper.ScreenMapper;
import com.vasu.movie_service.repository.ScreenRepository;
import com.vasu.movie_service.repository.ScreenSeatRepository;
import com.vasu.movie_service.repository.TheatreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final ScreenSeatRepository screenSeatRepository;
    private final TheatreRepository theatreRepository;
    private final ScreenMapper screenMapper;

    @Override
    public ScreenResponse createScreen(Long theatreId, ScreenCreateRequest request) {
        log.info("createScreen");
        log.info("theatreId:{}", theatreId);
        log.info("request:{}", request);
        Theatre theatre = theatreRepository.findByIdAndStatus(theatreId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", theatreId));

        boolean nameExists = screenRepository.findByTheatreIdAndStatus(theatreId, EntityStatus.ACTIVE)
                .stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new DuplicateResourceException(
                    "Screen with name '" + request.getName() + "' already exists in this theatre");
        }

        Screen screen = screenMapper.toEntity(request);
        screen.setTheatre(theatre);
        screen.setStatus(EntityStatus.ACTIVE);
        Screen saved = screenRepository.save(screen);
        return screenMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenResponse getScreenById(Long id) {
        Screen screen = screenRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
        return screenMapper.toResponse(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ScreenResponse> getScreensByTheatreId(Long theatreId, Pageable pageable) {
        Slice<Screen> screens = screenRepository.findByTheatreIdAndStatus(theatreId, EntityStatus.ACTIVE, pageable);
        return screens.map(screenMapper::toResponse);
    }

    @Override
    public ScreenResponse updateScreen(Long id, ScreenUpdateRequest request) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
        screenMapper.updateEntity(request, screen);
        Screen updated = screenRepository.save(screen);
        return screenMapper.toResponse(updated);
    }

    @Override
    public void updateScreenStatus(Long id, StatusChangeRequest request) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", id));
        screen.setStatus(request.getStatus());
        screenRepository.save(screen);
    }

    @Override
    public List<ScreenSeatResponse> createSeats(Long screenId, SeatLayoutRequest request) {
        Screen screen = screenRepository.findByIdAndStatus(screenId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));

        List<ScreenSeat> seats = request.getSeats().stream()
                .map(screenMapper::toEntity)
                .map(seat -> {
                    seat.setScreen(screen);
                    return seat;
                })
                .toList();

        long existingCount = screenSeatRepository.countByScreenId(screenId);
        long newCount = seats.size();
        if (existingCount + newCount > screen.getTotalSeats()) {
            throw new IllegalArgumentException(
                    "Total seats exceed screen capacity. Current: " + existingCount +
                    ", Adding: " + newCount + ", Capacity: " + screen.getTotalSeats());
        }

        List<ScreenSeat> saved = screenSeatRepository.saveAll(seats);
        return screenMapper.toResponseList(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenSeatResponse> getSeatsByScreenId(Long screenId) {
        screenRepository.findByIdAndStatus(screenId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", screenId));
        List<ScreenSeat> seats = screenSeatRepository.findByScreenId(screenId);
        return screenMapper.toResponseList(seats);
    }
}
