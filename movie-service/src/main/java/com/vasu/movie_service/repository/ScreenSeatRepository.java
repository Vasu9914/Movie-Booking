package com.vasu.movie_service.repository;

import com.vasu.movie_service.entity.ScreenSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScreenSeatRepository extends JpaRepository<ScreenSeat, Long> {

    List<ScreenSeat> findByScreenId(Long screenId);

    long countByScreenId(Long screenId);
}
