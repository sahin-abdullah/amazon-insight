package com.amazoninsight.llmreview.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping("/task-status")
public class TaskStatusController {

    @Value("${flask.endpoint.url}")
    private String flaskEndpointUrl;

    private final RestTemplate restTemplate;

    public TaskStatusController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getTaskStatus(@RequestParam("task_id") String taskId) {
        String url = flaskEndpointUrl + "/" + taskId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}