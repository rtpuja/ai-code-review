package com.reviewer.controller;

import com.reviewer.model.ReviewRequest;
import com.reviewer.model.ReviewResult;
import com.reviewer.service.CodeReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for triggering code reviews manually.
 *
 * POST /api/review           — get review JSON
 * POST /api/review-and-comment — review + post as GitHub PR comment
 * GET  /api/health           — health check
 */
@RestController
@RequestMapping("/api")
public class ReviewController {

    private final CodeReviewService codeReviewService;

    public ReviewController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/review")
    public ResponseEntity<ReviewResult> reviewPR(@RequestBody ReviewRequest request) {
        ReviewResult result = codeReviewService.reviewPullRequest(
                request.owner(), request.repo(), request.prNumber());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/review-and-comment")
    public ResponseEntity<ReviewResult> reviewAndComment(@RequestBody ReviewRequest request) {
        ReviewResult result = codeReviewService.reviewAndComment(
                request.owner(), request.repo(), request.prNumber());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Code Review Assistant is running");
    }
}