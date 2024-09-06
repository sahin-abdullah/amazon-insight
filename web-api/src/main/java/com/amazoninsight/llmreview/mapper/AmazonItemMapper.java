package com.amazoninsight.llmreview.mapper;

import com.amazoninsight.llmreview.dto.AmazonItemDTO;
import com.amazoninsight.llmreview.model.AmazonItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AmazonItemMapper {

    AmazonItemMapper INSTANCE = Mappers.getMapper(AmazonItemMapper.class);

    @Mapping(source = "asin", target = "asin")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "averageRating", target = "averageRating")
    @Mapping(source = "totalReviewCount", target = "totalReviewCount")
    @Mapping(source = "imageUrl", target = "imageUrl")
    @Mapping(source = "reviews", target = "reviews")
    AmazonItemDTO amazonItemToAmazonItemDTO(AmazonItem amazonItem);
}