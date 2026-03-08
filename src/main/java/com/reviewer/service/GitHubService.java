package com.reviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.reviewer.model.PullRequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * GitHub API Service — fetches PR data and posts review comments.
 */
@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private final WebClient github;

    public GitHubService(@Qualifier("gitHubWebClient") WebClient github) {
        this.github = github;
    }

    /** Fetch PR metadata. */
    public PullRequestData fetchPullRequest(String owner, String repo, int prNumber) {
        log.info("Fetching PR #{} from {}/{}", prNumber, owner, repo);
        JsonNode pr = github.get()
                .uri("/repos/{o}/{r}/pulls/{n}", owner, repo, prNumber)
                .retrieve().bodyToMono(JsonNode.class).block();
        return new PullRequestData(
                prNumber,
                pr.path("title").asText(),
                pr.path("body").asText(""),
                pr.path("user").path("login").asText(),
                pr.path("head").path("sha").asText());
    }

    /** Fetch the full unified diff of a PR. */
    public String fetchPRDiff(String owner, String repo, int prNumber) {
        log.info("Fetching diff for PR #{}", prNumber);
        return github.get()
                .uri("/repos/{o}/{r}/pulls/{n}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.v3.diff")
                .retrieve().bodyToMono(String.class).block();
    }

    /** Post a comment on the PR. */
    public void postComment(String owner, String repo, int prNumber, String body) {
        log.info("Posting comment on PR #{}", prNumber);
        github.post()
                .uri("/repos/{o}/{r}/issues/{n}/comments", owner, repo, prNumber)
                .bodyValue(Map.of("body", body))
                .retrieve().bodyToMono(JsonNode.class).block();
    }
}