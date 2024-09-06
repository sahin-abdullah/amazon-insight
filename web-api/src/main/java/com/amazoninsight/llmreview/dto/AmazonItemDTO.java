package com.amazoninsight.llmreview.dto;

import lombok.Data;

import java.util.List;

@Data
public class AmazonItemDTO {
    private String asin;
    private String title;
    private Double averageRating;
    private Integer totalReviewCount;
    private String imageUrl;
    private List<ReviewDTO> reviews;
}