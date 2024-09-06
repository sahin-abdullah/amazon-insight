package com.amazoninsight.llmreview.model;

import lombok.*;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "error_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private LocalDate timestamp;  // No Temporal needed for LocalDate

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "location", length = 255)
    private String location;  // Class name or method name where the error occurred

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;  // Additional error details
}
