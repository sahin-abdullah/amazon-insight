package com.amazoninsight.llmreview.service;

import com.amazoninsight.llmreview.dto.ReviewDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class OllamaClient {

    @Value("${ollama.base.url}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    @Setter
    private String modelName;

    public OllamaClient() {
        this.restTemplate = new RestTemplate();
        this.modelName = "llama3:8b";
    }

    public void generateText(String prompt, HttpServletResponse servletResponse) throws IOException {
        String url = baseUrl + "/api/generate?stream=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("model", modelName);
        requestBody.put("prompt", prompt);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Get the InputStream directly for streaming
        try (InputStream responseStream = new ByteArrayInputStream(
                Objects.requireNonNull(restTemplate.postForEntity(url, requestEntity, byte[].class)
                        .getBody()))) {

            ObjectMapper objectMapper = new ObjectMapper();
            BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream));
            String line;

            // Configure Servlet Response for Streaming
            servletResponse.setContentType("text/html");
            servletResponse.setCharacterEncoding("UTF-8");
            PrintWriter writer = servletResponse.getWriter();

            while ((line = reader.readLine()) != null) {
                try {
                    JsonNode rootNode = objectMapper.readTree(line); // Parse each line

                    if (rootNode.has("response")) {
                        String responseText = rootNode.get("response").asText();
                        writer.write(responseText);
                        writer.flush();
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing JSON line: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Handle exceptions (logging, error response, etc.)
            throw new RuntimeException("Ollama API request failed: " + e.getMessage());
        }
    }

    public String promptBuilder(String question, List<ReviewDTO> reviews, String title, String description) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are Amazon Insight, a helpful and polite assistant, conversing with a user about the subjects contained in a set of reviews.\n");
        prompt.append("Use the information from the Title, Description, and REVIEWS section to provide accurate answers. If unsure or if the answer isn't found in the REVIEWS section, simply state that you don't know the answer.\n");
        prompt.append("Do not answer questions that are unrelated to the content of the reviews.\n");
        prompt.append("Use your own words to answer the questions. You do not have to refer to specific document but you can quote a sentence from the reviews.\n");
        prompt.append("Title: ").append("\n").append(title);
        prompt.append("Description: ").append("\n").append(description);
        prompt.append("REVIEWS:\n");

        // Embed reviews
        for (ReviewDTO review : reviews) {
            prompt.append("- ").append(review.getReviewText()).append("\n");
        }

        prompt.append("QUESTION:\n");
        prompt.append(question);

        return prompt.toString();
    }
}