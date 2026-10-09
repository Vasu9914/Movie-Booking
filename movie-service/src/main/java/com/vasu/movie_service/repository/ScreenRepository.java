package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Long> {

    @EntityGraph(attributePaths = {"theatre"})
    Page<Screen> findByStatus(EntityStatus status, Pageable pageable);

    Page<Screen> findByTheatreId(Long theatreId, Pageable pageable);

    @EntityGraph(attributePaths = {"theatre"})
    Page<Screen> findByTheatreIdAndStatus(Long theatreId, EntityStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"theatre"})
    Optional<Screen> findByIdAndStatus(Long id, EntityStatus status);

    List<Screen> findByTheatreIdAndStatus(Long theatreId, EntityStatus status);
}
