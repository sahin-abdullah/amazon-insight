package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.model.ErrorLog;
import com.amazoninsight.llmreview.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class ErrorLogService {

    private static final Logger logger = LoggerFactory.getLogger(ErrorLogService.class);

    @Autowired
    private ErrorLogRepository errorLogRepository;

    public void logError(String errorMessage, String location) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setTimestamp(LocalDate.now());
        errorLog.setErrorMessage(errorMessage);
        errorLog.setLocation(location);
        errorLogRepository.save(errorLog);
    }

    public void logError(String errorMessage, String location, Exception e) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setTimestamp(LocalDate.now());
        errorLog.setErrorMessage(errorMessage);
        errorLog.setLocation(location);
        errorLog.setStackTrace(getStackTraceAsString(e));
        errorLogRepository.save(errorLog);
        logger.error("Error at {}: {}", location, errorMessage, e);
    }

    private String getStackTraceAsString(Exception e) {
        return Arrays.stream(e.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.joining("\n"));
    }
}
