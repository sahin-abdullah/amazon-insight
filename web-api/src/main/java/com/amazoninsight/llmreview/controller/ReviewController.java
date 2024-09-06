package com.amazoninsight.llmreview.controller;

import com.amazoninsight.llmreview.dto.UrlRequest;
import com.amazoninsight.llmreview.service.ScraperService;
import com.amazoninsight.llmreview.service.UserTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/amazon")
public class ReviewController {

    private final ScraperService scraperService;
    private final UserTaskService userTaskService;
    private final RestTemplate restTemplate;

    @Value("${flask.endpoint.url}")
    private String flaskEndpointUrl;

    private static final Logger logger = LoggerFactory.getLogger(ReviewController.class);

    @Autowired
    public ReviewController(ScraperService scraperService, RestTemplate restTemplate, UserTaskService userTaskService) {
        this.scraperService = scraperService;
        this.restTemplate = restTemplate;
        this.userTaskService = userTaskService;
    }

    private static final Pattern ASIN_PATTERN = Pattern.compile("/(dp|product)/([A-Z0-9]{10})");

    public static String extractAsin(String url) {
        Matcher matcher = ASIN_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return null;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public String extractReviews(@ModelAttribute UrlRequest urlRequest, RedirectAttributes redirectAttributes, Principal principal) {
        if (urlRequest.getUrl() == null || urlRequest.getUrl().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "URL is required.");
            logger.warn("URL is required but not provided.");
            return "redirect:/";
        }

        try {
            String taskId = scraperService.getItemReviews(urlRequest);
            String username = principal.getName();
            String asin = extractAsin(urlRequest.getUrl());

            // Save the user-task association using UserTaskService
            userTaskService.saveUserTask(username, taskId, asin);
            logger.info("User {} started a new task with ID {} for ASIN {}", username, taskId, asin);

            return "redirect:/amazon/progress?task_id=" + taskId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error extracting reviews. Please check the URL and try again.");
            logger.error("Error extracting reviews for URL {}: {}", urlRequest.getUrl(), e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/task-status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getTaskStatus(@RequestParam("task_id") String taskId) {
        logger.info("Received task_id: {}", taskId);
        String url = flaskEndpointUrl + "/task-status/" + taskId;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        logger.info("Task status for task_id {}: {}", taskId, response.getStatusCode());
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @GetMapping("/progress")
    @PreAuthorize("isAuthenticated()")
    public String showProgress(@RequestParam("task_id") String taskId, Model model) {
        model.addAttribute("task_id", taskId);
        logger.info("Showing progress for task_id {}", taskId);
        return "fetch-reviews";
    }
}