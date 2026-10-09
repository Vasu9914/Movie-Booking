package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.Theatre;
import com.vasu.movie_service.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TheatreRepository extends JpaRepository<Theatre, Long> {

    Page<Theatre> findByStatus(EntityStatus status, Pageable pageable);

    Page<Theatre> findByCity(String city, Pageable pageable);

    Page<Theatre> findByCityAndStatus(String city, EntityStatus status, Pageable pageable);

    Optional<Theatre> findByIdAndStatus(Long id, EntityStatus status);
}
