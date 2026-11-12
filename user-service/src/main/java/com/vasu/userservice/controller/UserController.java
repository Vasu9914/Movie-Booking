package com.vasu.userservice.controller;

import com.vasu.userservice.dto.request.ChangePasswordRequest;
import com.vasu.userservice.dto.request.UpdateProfileRequest;
import com.vasu.userservice.dto.response.ProfileResponse;
import com.vasu.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ProfileResponse getProfile(@RequestHeader("X-User-Id") Long userId) {
        return userService.getProfile(userId);
    }

    @PatchMapping("/me")
    public ProfileResponse updateProfile(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody UpdateProfileRequest updateRequest) {
        return userService.updateProfile(userId, updateRequest);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody ChangePasswordRequest changeRequest) {
        userService.changePassword(userId, changeRequest);
    }
}
