package com.amazoninsight.llmreview.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReviewDTO {
    private String amazonReviewID;
    private String reviewTitle;
    private String reviewText;
    private Double rating;
    private Integer helpfulCount;
    private LocalDate reviewDate;
}