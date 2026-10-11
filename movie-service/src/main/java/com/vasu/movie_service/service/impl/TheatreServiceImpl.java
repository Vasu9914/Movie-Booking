package com.vasu.movie_service.service.impl;

import com.vasu.movie_service.dto.request.StatusChangeRequest;
import com.vasu.movie_service.dto.request.TheatreCreateRequest;
import com.vasu.movie_service.dto.request.TheatreUpdateRequest;
import com.vasu.movie_service.dto.response.TheatreResponse;
import com.vasu.movie_service.entity.Theatre;
import com.vasu.movie_service.enums.EntityStatus;
import com.vasu.movie_service.exception.ResourceNotFoundException;
import com.vasu.movie_service.mapper.TheatreMapper;
import com.vasu.movie_service.repository.TheatreRepository;
import com.vasu.movie_service.service.TheatreService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;
    private final TheatreMapper theatreMapper;

    @Override
    public TheatreResponse createTheatre(TheatreCreateRequest request) {
        Theatre theatre = theatreMapper.toEntity(request);
        theatre.setStatus(EntityStatus.ACTIVE);
        Theatre saved = theatreRepository.save(theatre);
        return theatreMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(Long id) {
        Theatre theatre = theatreRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", id));
        return theatreMapper.toResponse(theatre);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<TheatreResponse> getAllTheatres(Pageable pageable) {
        Slice<Theatre> theatres = theatreRepository.findByStatus(EntityStatus.ACTIVE, pageable);
        return theatres.map(theatreMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<TheatreResponse> getTheatresByCity(String city, Pageable pageable) {
        Slice<Theatre> theatres = theatreRepository.findByCityAndStatus(city, EntityStatus.ACTIVE, pageable);
        return theatres.map(theatreMapper::toResponse);
    }

    @Override
    public TheatreResponse updateTheatre(Long id, TheatreUpdateRequest request) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", id));
        theatreMapper.updateEntity(request, theatre);
        Theatre updated = theatreRepository.save(theatre);
        return theatreMapper.toResponse(updated);
    }

    @Override
    public void updateTheatreStatus(Long id, StatusChangeRequest request) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Theatre", id));
        theatre.setStatus(request.getStatus());
        theatreRepository.save(theatre);
    }
}
