package com.reviewer.model;

import java.util.List;

/** Complete code review result with categorized issues. */
public record ReviewResult(
        int prNumber,
        String prTitle,
        int overallScore,
        String summary,
        List<CodeIssue> bugs,
        List<CodeIssue> optimizations,
        List<CodeIssue> documentation,
        List<CodeIssue> security,
        int totalIssues,
        String reviewedAt
) {}