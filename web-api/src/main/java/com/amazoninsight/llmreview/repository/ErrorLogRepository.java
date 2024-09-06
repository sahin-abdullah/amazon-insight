package com.amazoninsight.llmreview.repository;


import com.amazoninsight.llmreview.model.ErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ErrorLogRepository extends JpaRepository<ErrorLog, Long> {
}