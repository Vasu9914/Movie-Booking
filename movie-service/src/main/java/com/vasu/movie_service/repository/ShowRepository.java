package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.Show;
import com.vasu.movie_service.enums.ShowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Page<Show> findByStatus(ShowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Page<Show> findByMovieId(Long movieId, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Page<Show> findByMovieIdAndStatus(Long movieId, ShowStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"movie", "screen"})
    Optional<Show> findByIdAndStatus(Long id, ShowStatus status);

    List<Show> findByScreenIdAndStatus(Long screenId, ShowStatus status);

    @Query("SELECT s FROM Show s WHERE s.screen.id = :screenId AND s.status = :status AND s.startTime < :endTime AND s.endTime > :startTime")
    List<Show> findOverlappingShows(@Param("screenId") Long screenId, @Param("status") ShowStatus status,
                                    @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
