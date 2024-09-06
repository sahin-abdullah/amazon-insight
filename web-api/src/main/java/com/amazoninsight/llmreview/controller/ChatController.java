package com.amazoninsight.llmreview.controller;

import com.amazoninsight.llmreview.dto.AmazonItemDTO;
import com.amazoninsight.llmreview.dto.ReviewDTO;
import com.amazoninsight.llmreview.dto.ReviewIdResponse;
import com.amazoninsight.llmreview.mapper.AmazonItemMapper;
import com.amazoninsight.llmreview.mapper.ReviewMapper;
import com.amazoninsight.llmreview.model.AmazonItem;
import com.amazoninsight.llmreview.model.Review;
import com.amazoninsight.llmreview.repository.AmazonItemRepository;
import com.amazoninsight.llmreview.repository.ReviewRepository;
import com.amazoninsight.llmreview.repository.UserTaskBlockingRepository;
import com.amazoninsight.llmreview.service.AmazonItemService;
import com.amazoninsight.llmreview.service.OllamaClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.wimdeblauwe.htmx.spring.boot.mvc.HxRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/amazon")
public class ChatController {

    private final AmazonItemRepository amazonItemRepository;
    @Value("${flask.endpoint.url}")
    private String flaskEndpointUrl;

    private final UserTaskBlockingRepository userTaskBlockingRepository;
    private final ReviewRepository reviewRepository;
    private final AmazonItemService amazonItemService;
    private final AmazonItemMapper amazonItemMapper;
    private final OllamaClient ollamaClient;
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    public ChatController(UserTaskBlockingRepository userTaskBlockingRepository,
                          AmazonItemService amazonItemService,
                          AmazonItemMapper amazonItemMapper,
                          OllamaClient ollamaClient,
                          ReviewRepository reviewRepository, AmazonItemRepository amazonItemRepository) {
        this.userTaskBlockingRepository = userTaskBlockingRepository;
        this.amazonItemService = amazonItemService;
        this.amazonItemMapper = amazonItemMapper;
        this.ollamaClient = ollamaClient;
        this.reviewRepository = reviewRepository;
        this.amazonItemRepository = amazonItemRepository;
    }

    @GetMapping("/chat/{taskId}")
    @PreAuthorize("isAuthenticated()")
    public String chat(@PathVariable("taskId") String taskId, Principal principal, Model model) {
        String username = principal.getName();
        boolean isValidTask = userTaskBlockingRepository.findByUsernameAndTaskId(username, taskId).isPresent();

        if (!isValidTask) {
            return "redirect:/error";
        }

        String asin = userTaskBlockingRepository.findByTaskId(taskId).orElseThrow().getAsin();
        AmazonItem amazonItem = amazonItemService.findByAsinWithReviews(asin);
        AmazonItemDTO amazonItemDTO = amazonItemMapper.amazonItemToAmazonItemDTO(amazonItem);

        List<ReviewDTO> reviews = amazonItemDTO.getReviews();
        Collections.shuffle(reviews);
        amazonItemDTO.setReviews(reviews.stream().limit(20).collect(Collectors.toList()));

        model.addAttribute("amazonItem", amazonItemDTO);
        return "chat";
    }

    @HxRequest
    @PostMapping("/chat/ask")
    public void generateChatResponse(@RequestParam("message") String message, @RequestParam("asin") String asin,
                                     HttpServletResponse response, Model model) throws IOException {
        logger.info("User Message: {}", message);
        AmazonItem amazonItem = amazonItemRepository.findByAsin(asin);
        Long id = amazonItem.getId();

        // Fetch relevant reviews from Flask endpoint
        List<ReviewDTO> relevantReviews = getRelevantReviewsFromFlask(message, id);

        // Fetch Amazon item title and its description


        // Generate response using the reviews and the user message
        assert relevantReviews != null;
        String prompt = ollamaClient.promptBuilder(message, relevantReviews, amazonItem.getTitle(), amazonItem.getItemFeatures());

        ollamaClient.generateText(prompt, response);
    }

    private List<ReviewDTO> getRelevantReviewsFromFlask(String message, Long id) {
        Map<String, String> requestPayload = new HashMap<>();
        requestPayload.put("question", message);
        requestPayload.put("id", id.toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestPayload, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    flaskEndpointUrl + "/relevant-reviews",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                ReviewIdResponse response = objectMapper.readValue(
                        responseEntity.getBody(),
                        ReviewIdResponse.class
                );

                List<Integer> reviewIds = response.getAmazonReviewIDs();

                List<Review> reviews = reviewRepository.findAllByIdIn(
                        reviewIds.stream().map(Long::valueOf).collect(Collectors.toList())
                );
                return reviews.stream()
                        .map(ReviewMapper.INSTANCE::reviewToReviewDTO)
                        .collect(Collectors.toList());
            } else {
                logger.error("Flask endpoint returned non-OK status: {}", responseEntity.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("Error communicating with Flask endpoint: {}", e.getMessage());
            return null;
        }
    }
}