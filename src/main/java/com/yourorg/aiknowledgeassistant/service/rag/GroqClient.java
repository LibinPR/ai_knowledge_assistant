package com.yourorg.aiknowledgeassistant.service.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class GroqClient {

        private final RestTemplate restTemplate;
        private final String apiKey;
        private final String baseUrl;
        private final String model;

        public GroqClient(
                        @Value("${groq.api-key}") String apiKey,
                        @Value("${groq.base-url}") String baseUrl,
                        @Value("${groq.model}") String model) {
                this.apiKey = apiKey;
                this.baseUrl = baseUrl;
                this.model = model;
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(Duration.ofSeconds(10));
                requestFactory.setReadTimeout(Duration.ofSeconds(90));
                this.restTemplate = new RestTemplate(requestFactory);
        }

        public String chat(String systemPrompt, String userMessage) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                Map<String, Object> body = Map.of(
                                "model", model,
                                "messages", List.of(
                                                Map.of("role", "system", "content", systemPrompt),
                                                Map.of("role", "user", "content", userMessage)));

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                ResponseEntity<GroqResponse> response = restTemplate.postForEntity(
                                baseUrl + "/chat/completions",
                                request,
                                GroqResponse.class);

                if (response.getBody() == null || response.getBody().choices() == null
                                || response.getBody().choices().isEmpty()
                                || response.getBody().choices().get(0).message() == null) {
                        throw new IllegalStateException("AI provider returned an empty response");
                }
                return response.getBody().choices().get(0).message().content();
        }

        record GroqResponse(List<Choice> choices) {
        }

        record Choice(Message message) {
        }

        record Message(String content) {
        }
}
