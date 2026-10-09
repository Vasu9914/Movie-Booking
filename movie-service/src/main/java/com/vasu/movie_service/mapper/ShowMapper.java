package com.vasu.movie_service.mapper;

import com.vasu.movie_service.dto.request.ShowCreateRequest;
import com.vasu.movie_service.dto.response.ShowResponse;
import com.vasu.movie_service.entity.Show;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MovieMapper.class})
public interface ShowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "movie", ignore = true)
    @Mapping(target = "screen", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Show toEntity(ShowCreateRequest request);

    ShowResponse toResponse(Show show);
}
