package com.vasu.userservice.service;

import com.vasu.userservice.dto.request.ChangePasswordRequest;
import com.vasu.userservice.dto.request.LoginRequest;
import com.vasu.userservice.dto.request.RegisterRequest;
import com.vasu.userservice.dto.request.UpdateProfileRequest;
import com.vasu.userservice.dto.response.LoginResponse;
import com.vasu.userservice.dto.response.ProfileResponse;

public interface UserService {

    ProfileResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    ProfileResponse getProfile(Long userId);

    ProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, ChangePasswordRequest request);
}
