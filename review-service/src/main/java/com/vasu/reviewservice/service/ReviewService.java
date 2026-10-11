package com.vasu.reviewservice.service;

import com.vasu.reviewservice.dto.common.PageResponse;
import com.vasu.reviewservice.dto.request.ReviewCreateRequest;
import com.vasu.reviewservice.dto.request.ReviewUpdateRequest;
import com.vasu.reviewservice.dto.response.ReviewResponse;
import com.vasu.reviewservice.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewResponse createReview(Long userId, Long movieId, ReviewCreateRequest request);

    PageResponse<ReviewResponse> getReviewsByMovieId(Long movieId, Pageable pageable);

    ReviewSummaryResponse getReviewSummary(Long movieId);

    PageResponse<ReviewResponse> getUserReviews(Long userId, Pageable pageable);

    ReviewResponse updateReview(Long userId, Long movieId, Long reviewId, ReviewUpdateRequest request);

    void deleteReview(Long userId, Long movieId, Long reviewId);
}
