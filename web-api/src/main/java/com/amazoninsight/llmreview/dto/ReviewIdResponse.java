package com.amazoninsight.llmreview.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReviewIdResponse {
    private List<Integer> amazonReviewIDs;
}