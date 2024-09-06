package com.amazoninsight.llmreview.mapper;

import com.amazoninsight.llmreview.dto.ReviewDTO;
import com.amazoninsight.llmreview.model.Review;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ReviewMapper {
    ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

    ReviewDTO reviewToReviewDTO(Review review);
    Review reviewDTOToReview(ReviewDTO reviewDTO);
}