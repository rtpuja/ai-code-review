package com.reviewer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.reviewer.model.CodeIssue;
import com.reviewer.model.PullRequestData;
import com.reviewer.model.ReviewResult;
import com.reviewer.prompt.PromptTemplates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Code Review Service — orchestrates GitHub data fetching,
 * AI analysis, and result formatting.
 */
@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);
    private static final int MAX_DIFF_CHARS = 12_000;

    private final GitHubService gitHub;
    private final AzureOpenAIService ai;

    public CodeReviewService(GitHubService gitHub, AzureOpenAIService ai) {
        this.gitHub = gitHub;
        this.ai = ai;
    }

    /** Review a PR: fetch diff, analyze with AI, return structured result. */
    public ReviewResult reviewPullRequest(String owner, String repo, int prNumber) {
        log.info("Starting review for {}/{} PR #{}", owner, repo, prNumber);

        PullRequestData prData = gitHub.fetchPullRequest(owner, repo, prNumber);
        String diff = gitHub.fetchPRDiff(owner, repo, prNumber);

        // Truncate large diffs
        if (diff.length() > MAX_DIFF_CHARS) {
            log.warn("Diff too large ({}), truncating", diff.length());
            diff = diff.substring(0, MAX_DIFF_CHARS)
                    + "\n\n[... TRUNCATED ...]";
        }

        String context = "PR #%d: %s\nAuthor: %s\nDescription: %s\n\n--- DIFF ---\n%s"
                .formatted(prData.prNumber(), prData.title(),
                        prData.author(), prData.description(), diff);

        JsonNode analysis = ai.chatAsJson(PromptTemplates.CODE_REVIEW_SYSTEM, context);
        return parseResult(analysis, prData);
    }

    /** Review and post result as GitHub PR comment. */
    public ReviewResult reviewAndComment(String owner, String repo, int prNumber) {
        ReviewResult result = reviewPullRequest(owner, repo, prNumber);
        gitHub.postComment(owner, repo, prNumber, formatMarkdown(result));
        return result;
    }

    private ReviewResult parseResult(JsonNode json, PullRequestData pr) {
        List<CodeIssue> bugs = parseIssues(json.path("bugs"), "bug");
        List<CodeIssue> opts = parseIssues(json.path("optimizations"), "optimization");
        List<CodeIssue> docs = parseIssues(json.path("documentation"), "documentation");
        List<CodeIssue> sec = parseIssues(json.path("security"), "security");

        int total = bugs.size() + opts.size() + docs.size() + sec.size();
        return new ReviewResult(
                pr.prNumber(), pr.title(),
                json.path("overall_score").asInt(50),
                json.path("summary").asText("Review completed."),
                bugs, opts, docs, sec, total, Instant.now().toString());
    }

    private List<CodeIssue> parseIssues(JsonNode node, String category) {
        List<CodeIssue> issues = new ArrayList<>();
        if (node == null || !node.isArray()) return issues;
        for (JsonNode item : node) {
            issues.add(new CodeIssue(
                    category,
                    item.path("severity").asText("medium"),
                    item.path("file").asText("unknown"),
                    item.path("line").asText(""),
                    item.path("title").asText("Issue found"),
                    item.path("description").asText(""),
                    item.path("suggestion").asText("")));
        }
        return issues;
    }

    private String formatMarkdown(ReviewResult r) {
        var sb = new StringBuilder();
        sb.append("## \uD83E\uDD16 AI Code Review\n\n");
        sb.append("**Overall Score:** %d/100\n\n".formatted(r.overallScore()));
        sb.append("**Summary:** %s\n\n".formatted(r.summary()));
        appendSection(sb, "\uD83D\uDC1B Bugs", r.bugs());
        appendSection(sb, "\u26A1 Optimizations", r.optimizations());
        appendSection(sb, "\uD83D\uDCDD Documentation", r.documentation());
        appendSection(sb, "\uD83D\uDD12 Security", r.security());
        sb.append("---\n*Reviewed by AI Code Review Assistant*");
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, List<CodeIssue> issues) {
        if (issues.isEmpty()) {
            sb.append("### %s\n\u2705 No issues\n\n".formatted(title));
            return;
        }
        sb.append("### %s (%d)\n\n".formatted(title, issues.size()));
        for (CodeIssue i : issues) {
            String icon = switch (i.severity()) {
                case "high" -> "\uD83D\uDD34";
                case "medium" -> "\uD83D\uDFE1";
                default -> "\uD83D\uDFE2";
            };
            sb.append("%s **[%s]** %s\n".formatted(icon, i.severity().toUpperCase(), i.title()));
            if (!"unknown".equals(i.file())) {
                sb.append("  - File: %s".formatted(i.file()));
                if (!i.line().isEmpty()) sb.append(" (line %s)".formatted(i.line()));
                sb.append("\n");
            }
            sb.append("  - %s\n".formatted(i.description()));
            if (!i.suggestion().isEmpty())
                sb.append("  - \uD83D\uDCA1 *%s*\n".formatted(i.suggestion()));
            sb.append("\n");
        }
    }
}