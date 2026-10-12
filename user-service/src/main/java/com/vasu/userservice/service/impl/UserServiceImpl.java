package com.vasu.userservice.service.impl;

import com.vasu.userservice.dto.request.ChangePasswordRequest;
import com.vasu.userservice.dto.request.LoginRequest;
import com.vasu.userservice.dto.request.RegisterRequest;
import com.vasu.userservice.dto.request.UpdateProfileRequest;
import com.vasu.userservice.dto.response.LoginResponse;
import com.vasu.userservice.dto.response.ProfileResponse;
import com.vasu.userservice.entity.User;
import com.vasu.userservice.enums.UserStatus;
import com.vasu.userservice.enums.UserRole;
import com.vasu.userservice.exception.DuplicateResourceException;
import com.vasu.userservice.exception.ResourceNotFoundException;
import com.vasu.userservice.mapper.UserMapper;
import com.vasu.userservice.repository.UserRepository;
import com.vasu.userservice.service.UserService;
import com.vasu.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public ProfileResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        User saved = userRepository.save(user);
        return userMapper.toProfileResponse(saved);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid email or password");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("Account is not active");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userMapper.toProfileResponse(user);
    }

    @Override
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        userMapper.updateProfile(request, user);
        User updated = userRepository.save(user);
        return userMapper.toProfileResponse(updated);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
