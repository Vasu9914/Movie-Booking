package com.vasu.reviewservice.service.impl;

import com.vasu.reviewservice.event.ReviewEligibilityPublisher;
import com.vasu.reviewservice.dto.common.PageResponse;
import com.vasu.reviewservice.dto.request.ReviewCreateRequest;
import com.vasu.reviewservice.dto.request.ReviewUpdateRequest;
import com.vasu.reviewservice.dto.response.ReviewResponse;
import com.vasu.reviewservice.dto.response.ReviewSummaryResponse;
import com.vasu.reviewservice.entity.Review;
import com.vasu.reviewservice.exception.AccessDeniedException;
import com.vasu.reviewservice.exception.BookingVerificationException;
import com.vasu.reviewservice.exception.DuplicateReviewException;
import com.vasu.reviewservice.exception.ResourceNotFoundException;
import com.vasu.reviewservice.mapper.ReviewMapper;
import com.vasu.reviewservice.repository.ReviewRepository;
import com.vasu.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMapper reviewMapper;
    private final ReviewEligibilityPublisher eligibilityPublisher;

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, Long movieId, ReviewCreateRequest request) {
        // Check if user already reviewed this movie
        if (reviewRepository.existsByUserIdAndMovieId(userId, movieId)) {
            throw new DuplicateReviewException(
                    "You have already reviewed this movie. Use update to modify your review."
            );
        }

        // Verify booking eligibility via RabbitMQ
        boolean eligible = eligibilityPublisher.checkEligibility(userId, movieId);
        if (!eligible) {
            throw new BookingVerificationException(
                    "You can only review movies for which you have a valid confirmed booking."
            );
        }

        Review review = Review.builder()
                .userId(userId)
                .movieId(movieId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        Review saved = reviewRepository.save(review);
        return reviewMapper.toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsByMovieId(Long movieId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByMovieId(movieId, pageable);
        return PageResponse.<ReviewResponse>builder()
                .content(reviewMapper.toReviewResponseList(page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Long movieId) {
        BigDecimal averageRating = reviewRepository.findAverageRatingByMovieId(movieId)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        long totalReviews = reviewRepository.countByMovieId(movieId);

        return ReviewSummaryResponse.builder()
                .movieId(movieId)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        Page<Review> page = reviewRepository.findByUserId(userId, pageable);
        return PageResponse.<ReviewResponse>builder()
                .content(reviewMapper.toReviewResponseList(page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long movieId, Long reviewId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only update your own reviews.");
        }

        // Verify movieId matches
        if (!review.getMovieId().equals(movieId)) {
            throw new ResourceNotFoundException("Review", reviewId);
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review updated = reviewRepository.save(review);
        return reviewMapper.toReviewResponse(updated);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long movieId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", reviewId));

        // Verify ownership
        if (!review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own reviews.");
        }

        // Verify movieId matches
        if (!review.getMovieId().equals(movieId)) {
            throw new ResourceNotFoundException("Review", reviewId);
        }

        reviewRepository.delete(review);
    }
}
