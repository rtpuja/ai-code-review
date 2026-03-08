package com.reviewer.model;

/** Metadata about a GitHub Pull Request. */
public record PullRequestData(
        int prNumber,
        String title,
        String description,
        String author,
        String headSha
) {}