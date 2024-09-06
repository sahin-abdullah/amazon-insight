package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.dto.UrlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Objects;


@Service
public class ScraperService {

    private final String FLASK_ENDPOINT_URL;
    private final RestTemplate restTemplate;
    private static final Logger logger = LoggerFactory.getLogger(ScraperService.class);

    @Autowired
    public ScraperService(@Value("${flask.endpoint.url}") String flaskEndpointUrl, RestTemplate restTemplate) {
        FLASK_ENDPOINT_URL = flaskEndpointUrl;
        this.restTemplate = restTemplate;
    }

    public String getItemReviews(UrlRequest urlRequest) {
        logger.info("Starting to get item reviews for URL: {}", urlRequest.getUrl());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        HttpEntity<UrlRequest> requestEntity = new HttpEntity<>(urlRequest, headers);
        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    FLASK_ENDPOINT_URL + "/scraper/",
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            String taskId = Objects.requireNonNull(response.getBody()).get("task_id");
            logger.info("Successfully retrieved task ID: {}", taskId);
            return taskId;
        } catch (Exception e) {
            logger.error("Error while getting item reviews: {}", e.getMessage());
            throw e;
        }
    }
}