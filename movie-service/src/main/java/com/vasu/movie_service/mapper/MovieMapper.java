package com.vasu.movie_service.mapper;

import com.vasu.movie_service.dto.request.MovieCreateRequest;
import com.vasu.movie_service.dto.request.MovieUpdateRequest;
import com.vasu.movie_service.dto.response.MovieResponse;
import com.vasu.movie_service.entity.Movie;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface MovieMapper {

    Movie toEntity(MovieCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(MovieUpdateRequest request, @MappingTarget Movie movie);

    MovieResponse toResponse(Movie movie);
}
