package com.vasu.reviewservice.controller;

import com.vasu.reviewservice.dto.common.PageResponse;
import com.vasu.reviewservice.dto.request.ReviewCreateRequest;
import com.vasu.reviewservice.dto.request.ReviewUpdateRequest;
import com.vasu.reviewservice.dto.response.ReviewResponse;
import com.vasu.reviewservice.dto.response.ReviewSummaryResponse;
import com.vasu.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{movieId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            Authentication authentication,
            @PathVariable Long movieId,
            @Valid @RequestBody ReviewCreateRequest request) {
        Long userId = extractUserId(authentication);
        return reviewService.createReview(userId, movieId, request);
    }

    @GetMapping("/{movieId}/reviews")
    public PageResponse<ReviewResponse> getReviews(
            @PathVariable Long movieId,
            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.getReviewsByMovieId(movieId, pageable);
    }

    @GetMapping("/{movieId}/reviews/summary")
    public ReviewSummaryResponse getReviewSummary(@PathVariable Long movieId) {
        return reviewService.getReviewSummary(movieId);
    }

    @GetMapping("/users/me/reviews")
    public PageResponse<ReviewResponse> getUserReviews(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = extractUserId(authentication);
        return reviewService.getUserReviews(userId, pageable);
    }

    @PutMapping("/{movieId}/reviews/{reviewId}")
    public ReviewResponse updateReview(
            Authentication authentication,
            @PathVariable Long movieId,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        Long userId = extractUserId(authentication);
        return reviewService.updateReview(userId, movieId, reviewId, request);
    }

    @DeleteMapping("/{movieId}/reviews/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(
            Authentication authentication,
            @PathVariable Long movieId,
            @PathVariable Long reviewId) {
        Long userId = extractUserId(authentication);
        reviewService.deleteReview(userId, movieId, reviewId);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new SecurityException("Authentication required");
        }
        return Long.parseLong(authentication.getName());
    }
}
