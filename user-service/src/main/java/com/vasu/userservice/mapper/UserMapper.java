package com.vasu.userservice.mapper;

import com.vasu.userservice.dto.request.RegisterRequest;
import com.vasu.userservice.dto.request.UpdateProfileRequest;
import com.vasu.userservice.dto.response.ProfileResponse;
import com.vasu.userservice.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

    ProfileResponse toProfileResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProfile(UpdateProfileRequest request, @MappingTarget User user);
}
