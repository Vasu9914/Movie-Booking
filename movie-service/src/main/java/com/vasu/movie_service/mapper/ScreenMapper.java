package com.vasu.movie_service.mapper;

import com.vasu.movie_service.dto.request.ScreenCreateRequest;
import com.vasu.movie_service.dto.request.ScreenSeatCreateRequest;
import com.vasu.movie_service.dto.request.ScreenUpdateRequest;
import com.vasu.movie_service.dto.response.ScreenResponse;
import com.vasu.movie_service.dto.response.ScreenSeatResponse;
import com.vasu.movie_service.entity.Screen;
import com.vasu.movie_service.entity.ScreenSeat;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ScreenMapper {

    Screen toEntity(ScreenCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(ScreenUpdateRequest request, @MappingTarget Screen screen);

    ScreenResponse toResponse(Screen screen);

    ScreenSeat toEntity(ScreenSeatCreateRequest request);

    ScreenSeatResponse toResponse(ScreenSeat seat);

    List<ScreenSeatResponse> toResponseList(List<ScreenSeat> seats);
}
