package com.vasu.reviewservice;

import com.vasu.reviewservice.client.BookingClient;
import com.vasu.reviewservice.dto.common.PageResponse;
import com.vasu.reviewservice.dto.request.ReviewCreateRequest;
import com.vasu.reviewservice.dto.request.ReviewUpdateRequest;
import com.vasu.reviewservice.dto.response.ReviewResponse;
import com.vasu.reviewservice.dto.response.ReviewSummaryResponse;
import com.vasu.reviewservice.entity.Review;
import com.vasu.reviewservice.exception.DuplicateReviewException;
import com.vasu.reviewservice.exception.BookingVerificationException;
import com.vasu.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingClient bookingClient;

    @Mock
    private com.vasu.reviewservice.mapper.ReviewMapper reviewMapper;

    private com.vasu.reviewservice.service.impl.ReviewServiceImpl reviewService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_MOVIE_ID = 100L;
    private static final Long TEST_REVIEW_ID = 1L;

    @BeforeEach
    void setUp() {
        reviewService = new com.vasu.reviewservice.service.impl.ReviewServiceImpl(
                reviewRepository,
                reviewMapper,
                bookingClient,
                null // MovieClient not used directly in service
        );
    }

    @Test
    void createReview_success() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(5)
                .comment("Excellent movie!")
                .build();

        when(reviewRepository.existsByUserIdAndMovieId(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(false);
        when(bookingClient.hasValidBookingForMovie(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(true);

        Review savedReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(TEST_USER_ID)
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Excellent movie!")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        ReviewResponse expectedResponse = ReviewResponse.builder()
                .id(TEST_REVIEW_ID)
                .userId(TEST_USER_ID)
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Excellent movie!")
                .build();
        when(reviewMapper.toReviewResponse(savedReview)).thenReturn(expectedResponse);

        ReviewResponse response = reviewService.createReview(TEST_USER_ID, TEST_MOVIE_ID, request);

        assertNotNull(response);
        assertEquals(5, response.getRating());
        assertEquals("Excellent movie!", response.getComment());
        assertEquals(TEST_MOVIE_ID, response.getMovieId());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_duplicateReview_throwsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(4)
                .comment("Good movie")
                .build();

        when(reviewRepository.existsByUserIdAndMovieId(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(true);

        assertThrows(DuplicateReviewException.class, () ->
                reviewService.createReview(TEST_USER_ID, TEST_MOVIE_ID, request)
        );
        verify(bookingClient, never()).hasValidBookingForMovie(anyLong(), anyLong());
    }

    @Test
    void createReview_noValidBooking_throwsException() {
        ReviewCreateRequest request = ReviewCreateRequest.builder()
                .rating(4)
                .comment("Good movie")
                .build();

        when(reviewRepository.existsByUserIdAndMovieId(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(false);
        when(bookingClient.hasValidBookingForMovie(TEST_USER_ID, TEST_MOVIE_ID)).thenReturn(false);

        assertThrows(BookingVerificationException.class, () ->
                reviewService.createReview(TEST_USER_ID, TEST_MOVIE_ID, request)
        );
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void getReviewsByMovieId_returnsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        List<Review> reviews = List.of(
                Review.builder().id(1L).userId(1L).movieId(TEST_MOVIE_ID).rating(5).comment("Great").build(),
                Review.builder().id(2L).userId(2L).movieId(TEST_MOVIE_ID).rating(4).comment("Good").build()
        );
        Page<Review> page = new PageImpl<>(reviews, pageable, 2);

        when(reviewRepository.findByMovieId(TEST_MOVIE_ID, pageable)).thenReturn(page);
        when(reviewMapper.toReviewResponseList(reviews)).thenReturn(List.of());

        PageResponse<ReviewResponse> response = reviewService.getReviewsByMovieId(TEST_MOVIE_ID, pageable);

        assertNotNull(response);
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());
    }

    @Test
    void getReviewSummary_returnsAverageAndCount() {
        when(reviewRepository.findAverageRatingByMovieId(TEST_MOVIE_ID)).thenReturn(Optional.of(new BigDecimal("4.50")));
        when(reviewRepository.countByMovieId(TEST_MOVIE_ID)).thenReturn(10L);

        ReviewSummaryResponse response = reviewService.getReviewSummary(TEST_MOVIE_ID);

        assertNotNull(response);
        assertEquals(TEST_MOVIE_ID, response.getMovieId());
        assertEquals(new BigDecimal("4.50"), response.getAverageRating());
        assertEquals(10L, response.getTotalReviews());
    }

    @Test
    void getReviewSummary_noReviews_returnsZero() {
        when(reviewRepository.findAverageRatingByMovieId(TEST_MOVIE_ID)).thenReturn(Optional.empty());
        when(reviewRepository.countByMovieId(TEST_MOVIE_ID)).thenReturn(0L);

        ReviewSummaryResponse response = reviewService.getReviewSummary(TEST_MOVIE_ID);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getAverageRating());
        assertEquals(0L, response.getTotalReviews());
    }

    @Test
    void updateReview_success() {
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating(4)
                .comment("Updated comment")
                .build();

        Review existingReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(TEST_USER_ID)
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Old comment")
                .build();

        Review updatedReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(TEST_USER_ID)
                .movieId(TEST_MOVIE_ID)
                .rating(4)
                .comment("Updated comment")
                .build();

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(updatedReview);

        ReviewResponse expectedResponse = ReviewResponse.builder()
                .id(TEST_REVIEW_ID)
                .rating(4)
                .comment("Updated comment")
                .build();
        when(reviewMapper.toReviewResponse(updatedReview)).thenReturn(expectedResponse);

        ReviewResponse response = reviewService.updateReview(TEST_USER_ID, TEST_MOVIE_ID, TEST_REVIEW_ID, request);

        assertNotNull(response);
        assertEquals(4, response.getRating());
        assertEquals("Updated comment", response.getComment());
    }

    @Test
    void updateReview_notOwner_throwsException() {
        ReviewUpdateRequest request = ReviewUpdateRequest.builder()
                .rating(4)
                .comment("Updated")
                .build();

        Review existingReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(99L) // Different user
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Old")
                .build();

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(existingReview));

        assertThrows(com.vasu.reviewservice.exception.AccessDeniedException.class, () ->
                reviewService.updateReview(TEST_USER_ID, TEST_MOVIE_ID, TEST_REVIEW_ID, request)
        );
    }

    @Test
    void deleteReview_success() {
        Review existingReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(TEST_USER_ID)
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Good")
                .build();

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(existingReview));

        reviewService.deleteReview(TEST_USER_ID, TEST_MOVIE_ID, TEST_REVIEW_ID);

        verify(reviewRepository).delete(existingReview);
    }

    @Test
    void deleteReview_notOwner_throwsException() {
        Review existingReview = Review.builder()
                .id(TEST_REVIEW_ID)
                .userId(99L) // Different user
                .movieId(TEST_MOVIE_ID)
                .rating(5)
                .comment("Good")
                .build();

        when(reviewRepository.findById(TEST_REVIEW_ID)).thenReturn(Optional.of(existingReview));

        assertThrows(com.vasu.reviewservice.exception.AccessDeniedException.class, () ->
                reviewService.deleteReview(TEST_USER_ID, TEST_MOVIE_ID, TEST_REVIEW_ID)
        );
    }
}
