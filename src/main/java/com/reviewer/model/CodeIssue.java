package com.reviewer.model;

/** A single code issue found during review. */
public record CodeIssue(
        String category,    // bug, optimization, documentation, security
        String severity,    // high, medium, low
        String file,
        String line,
        String title,
        String description,
        String suggestion
) {}