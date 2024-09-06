package com.amazoninsight.llmreview.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDTO {
    private String errorCode;
    private String errorMessage;
    private String errorDetails;
}
