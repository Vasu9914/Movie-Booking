package com.vasu.movie_service.service.impl;

import com.vasu.movie_service.dto.request.MovieCreateRequest;
import com.vasu.movie_service.dto.request.MovieUpdateRequest;
import com.vasu.movie_service.dto.response.MovieResponse;
import com.vasu.movie_service.entity.Movie;
import com.vasu.movie_service.enums.EntityStatus;
import com.vasu.movie_service.exception.DuplicateResourceException;
import com.vasu.movie_service.exception.ResourceNotFoundException;
import com.vasu.movie_service.mapper.MovieMapper;
import com.vasu.movie_service.repository.MovieRepository;
import com.vasu.movie_service.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    public MovieResponse createMovie(MovieCreateRequest request) {
        if (movieRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Movie with title '" + request.getTitle() + "' already exists");
        }
        Movie movie = movieMapper.toEntity(request);
        Movie saved = movieRepository.save(movie);
        return movieMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toResponse(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<MovieResponse> getAllMovies(Pageable pageable) {
        Slice<Movie> movies = movieRepository.findByStatus(EntityStatus.ACTIVE, pageable);
        return movies.map(movieMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<MovieResponse> searchMovies(String keyword, Pageable pageable) {
        Slice<Movie> movies = movieRepository.searchByKeyword(keyword, EntityStatus.ACTIVE, pageable);
        return movies.map(movieMapper::toResponse);
    }

    @Override
    public MovieResponse updateMovie(Long id, MovieUpdateRequest request) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movieMapper.updateEntity(request, movie);
        Movie updated = movieRepository.save(movie);
        return movieMapper.toResponse(updated);
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(EntityStatus.INACTIVE);
        movieRepository.save(movie);
    }
}
