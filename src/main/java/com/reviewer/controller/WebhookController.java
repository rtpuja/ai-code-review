package com.reviewer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewer.config.AppConfig;
import com.reviewer.service.CodeReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * GitHub Webhook controller — auto-reviews PRs when opened or updated.
 *
 * Setup in GitHub: Settings > Webhooks > Add webhook
 *   Payload URL: https://your-server.com/api/webhook/github
 *   Content type: application/json
 *   Events: Pull requests
 */
@RestController
@RequestMapping("/api/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final CodeReviewService codeReviewService;
    private final String webhookSecret;
    private final ObjectMapper objectMapper;

    public WebhookController(
            CodeReviewService codeReviewService,
            AppConfig.GitHubProperties ghProps,
            ObjectMapper objectMapper) {
        this.codeReviewService = codeReviewService;
        this.webhookSecret = ghProps.webhookSecret();
        this.objectMapper = objectMapper;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String event,
            @RequestBody String payload) {

        // 1. Validate webhook signature
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            if (!verifySignature(payload, signature)) {
                log.warn("Invalid webhook signature");
                return ResponseEntity.status(401).body("Invalid signature");
            }
        }

        // 2. Only process pull_request events
        if (!"pull_request".equals(event)) {
            return ResponseEntity.ok("Ignored: " + event);
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            String action = root.path("action").asText();

            // 3. Only review on opened or synchronize
            if (!"opened".equals(action) && !"synchronize".equals(action)) {
                return ResponseEntity.ok("Ignored action: " + action);
            }

            // 4. Extract PR details
            String owner = root.path("repository").path("owner").path("login").asText();
            String repo = root.path("repository").path("name").asText();
            int prNumber = root.path("number").asInt();

            log.info("Reviewing PR #{} on {}/{}", prNumber, owner, repo);

            // 5. Async review via virtual thread (non-blocking webhook response)
            Thread.startVirtualThread(() -> {
                try {
                    codeReviewService.reviewAndComment(owner, repo, prNumber);
                    log.info("Review posted for PR #{}",  prNumber);
                } catch (Exception e) {
                    log.error("Failed to review PR #{}: {}", prNumber, e.getMessage());
                }
            });

            return ResponseEntity.ok("Review triggered for PR #" + prNumber);
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** Verify GitHub webhook HMAC-SHA256 signature. */
    private boolean verifySignature(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(hash);
            return expected.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}