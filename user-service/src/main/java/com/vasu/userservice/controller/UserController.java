package com.vasu.userservice.controller;

import com.vasu.userservice.dto.request.ChangePasswordRequest;
import com.vasu.userservice.dto.request.UpdateProfileRequest;
import com.vasu.userservice.dto.response.ProfileResponse;
import com.vasu.userservice.service.UserService;
import com.vasu.userservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @GetMapping("/me")
    public ProfileResponse getProfile(HttpServletRequest request) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        return userService.getProfile(userId);
    }

    @PatchMapping("/me")
    public ProfileResponse updateProfile(HttpServletRequest request, @Valid @RequestBody UpdateProfileRequest updateRequest) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        return userService.updateProfile(userId, updateRequest);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest changeRequest) {
        String token = extractToken(request);
        Long userId = jwtUtil.getUserIdFromToken(token);
        userService.changePassword(userId, changeRequest);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Missing or invalid Authorization header");
    }
}
