package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.Movie;
import com.vasu.movie_service.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    Page<Movie> findByStatus(EntityStatus status, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE m.status = :status AND (LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.language) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Movie> searchByKeyword(@Param("keyword") String keyword, @Param("status") EntityStatus status, Pageable pageable);

    Optional<Movie> findByIdAndStatus(Long id, EntityStatus status);

    boolean existsByTitle(String title);
}
