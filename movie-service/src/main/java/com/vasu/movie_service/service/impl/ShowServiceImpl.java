package com.vasu.movie_service.service.impl;

import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.request.ShowStatusChangeRequest;
import com.vasu.movie_service.dto.request.ShowUpdateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import com.vasu.movie_service.entity.Movie;
import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.entity.Show;
import com.vasu.movie_service.enums.EntityStatus;
import com.vasu.movie_service.enums.ShowStatus;
import com.vasu.movie_service.exception.OverlappingShowException;
import com.vasu.movie_service.exception.ResourceNotFoundException;
import com.vasu.movie_service.mapper.ShowMapper;
import com.vasu.movie_service.repository.MovieRepository;
import com.vasu.movie_service.repository.ScreenRepository;
import com.vasu.movie_service.repository.ShowRepository;
import com.vasu.movie_service.service.ShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final ScreenRepository screenRepository;
    private final ShowMapper showMapper;

    @Override
    public ShowResponse createShow(ShowCreateRequest request) {
        Movie movie = movieRepository.findByIdAndStatus(request.getMovieId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));

        Screen screen = screenRepository.findByIdAndStatus(request.getScreenId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", request.getScreenId()));

        validateShowTime(request.getStartTime(), request.getEndTime());

        List<Show> overlapping = showRepository.findOverlappingShows(
                request.getScreenId(), ShowStatus.SCHEDULED, request.getStartTime(), request.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new OverlappingShowException("Screen has overlapping shows at the specified time");
        }

        Show show = showMapper.toEntity(request);
        show.setMovie(movie);
        show.setScreen(screen);
        show.setStatus(ShowStatus.SCHEDULED);

        Show saved = showRepository.save(show);
        return showMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        return showMapper.toResponse(show);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ShowResponse> getAllShows(Pageable pageable) {
        Slice<Show> shows = showRepository.findByStatus(ShowStatus.SCHEDULED, pageable);
        return shows.map(showMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ShowResponse> getShowsByMovieId(Long movieId, Pageable pageable) {
        Slice<Show> shows = showRepository.findByMovieIdAndStatus(movieId, ShowStatus.SCHEDULED, pageable);
        return shows.map(showMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ShowResponse> getShowsByCity(String city, Pageable pageable) {
        Slice<Show> shows = showRepository.findByScreenTheatreCityAndStatus(city, ShowStatus.SCHEDULED, pageable);
        return shows.map(showMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<ShowResponse> getShowsByMovieIdAndDate(Long movieId, LocalDateTime dateFrom, Pageable pageable) {
        Slice<Show> shows = showRepository.findByMovieIdAndStatusAndStartTimeAfter(
                movieId, ShowStatus.SCHEDULED, dateFrom, pageable);
        return shows.map(showMapper::toResponse);
    }

    @Override
    public ShowResponse updateShow(Long id, ShowUpdateRequest request) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));

        Movie movie = movieRepository.findByIdAndStatus(request.getMovieId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", request.getMovieId()));

        Screen screen = screenRepository.findByIdAndStatus(request.getScreenId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", request.getScreenId()));

        validateShowTime(request.getStartTime(), request.getEndTime());

        if (show.getStatus() == ShowStatus.SCHEDULED) {
            List<Show> overlapping = showRepository.findOverlappingShows(
                    request.getScreenId(), ShowStatus.SCHEDULED, request.getStartTime(), request.getEndTime());
            if (overlapping.stream().anyMatch(s -> !s.getId().equals(id))) {
                throw new OverlappingShowException("Screen has overlapping shows at the specified time");
            }
        }

        show.setMovie(movie);
        show.setScreen(screen);
        show.setStartTime(request.getStartTime());
        show.setEndTime(request.getEndTime());

        Show updated = showRepository.save(show);
        return showMapper.toResponse(updated);
    }

    @Override
    public void updateShowStatus(Long id, ShowStatusChangeRequest request) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        show.setStatus(request.getStatus());
        showRepository.save(show);
    }

    @Override
    public void cancelShow(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new IllegalStateException("Only scheduled shows can be cancelled");
        }
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    private void validateShowTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.equals(startTime)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
    }
}
