package com.vasu.reviewservice.mapper;

import com.vasu.reviewservice.dto.response.ReviewResponse;
import com.vasu.reviewservice.entity.Review;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    ReviewResponse toReviewResponse(Review review);

    List<ReviewResponse> toReviewResponseList(List<Review> reviews);
}
