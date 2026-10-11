package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.Show;
import com.vasu.movie_service.enums.ShowStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByStatus(ShowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByMovieId(Long movieId, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByMovieIdAndStatus(Long movieId, ShowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Optional<Show> findByIdAndStatus(Long id, ShowStatus status);

    List<Show> findByScreenIdAndStatus(Long screenId, ShowStatus status);

    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId AND s.status = :status AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Show> findOverlappingShows(@Param("screenId") Long screenId, @Param("status") ShowStatus status,
                                    @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByMovieIdAndStatusAndStartTimeAfter(Long movieId, ShowStatus status,
                                                        LocalDateTime startTime, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByScreenTheatreCityAndStatus(String city, ShowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Slice<Show> findByScreenTheatreCityAndStatusAndStartTimeAfter(String city, ShowStatus status,
                                                                   LocalDateTime startTime, Pageable pageable);
}
