package com.reviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Azure OpenAI Service — sends prompts and parses structured JSON responses.
 */
@Service
public class AzureOpenAIService {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAIService.class);
    private final WebClient ai;
    private final ObjectMapper mapper;

    public AzureOpenAIService(
            @Qualifier("azureOpenAIWebClient") WebClient ai,
            ObjectMapper mapper) {
        this.ai = ai;
        this.mapper = mapper;
    }

    /** Send system + user message and return raw text. */
    public String chat(String systemPrompt, String userContent) {
        Map<String, Object> body = Map.of(
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userContent)),
                "temperature", 0.1,
                "max_tokens", 4000);

        JsonNode resp = ai.post().bodyValue(body)
                .retrieve().bodyToMono(JsonNode.class).block();

        return resp.path("choices").get(0)
                .path("message").path("content").asText();
    }

    /** Send prompt and parse response as JSON with fallback. */
    public JsonNode chatAsJson(String systemPrompt, String userContent) {
        String raw = chat(systemPrompt, userContent);
        // Strip markdown fences
        String cleaned = raw.replaceAll("(?s)```json?\\s*", "")
                .replaceAll("(?s)```\\s*$", "").trim();
        try {
            return mapper.readTree(cleaned);
        } catch (Exception e) {
            log.warn("JSON parse failed, extracting: {}", e.getMessage());
            int s = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (s >= 0 && end > s) {
                try { return mapper.readTree(cleaned.substring(s, end + 1)); }
                catch (Exception ex) { log.error("Extraction failed"); }
            }
            return mapper.createObjectNode();
        }
    }
}