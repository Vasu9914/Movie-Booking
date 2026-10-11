package com.vasu.movie_service.mapper;

import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.ScreenSeatCreateRequest;
import com.vasu.movie_service.dto.request.ScreenUpdateRequest;
import com.vasu.movie_service.dto.request.TheatreCreateRequest;
import com.vasu.movie_service.dto.request.TheatreUpdateRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.dto.response.TheatreResponse;
import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.entity.ScreenSeat;
import com.vasu.movie_service.entity.Theatre;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface TheatreMapper {

    Theatre toEntity(TheatreCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(TheatreUpdateRequest request, @MappingTarget Theatre theatre);

    TheatreResponse toResponse(Theatre theatre);
}
