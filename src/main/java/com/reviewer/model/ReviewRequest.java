package com.reviewer.model;

/** Request payload for triggering a code review. */
public record ReviewRequest(String owner, String repo, int prNumber) {}