# Architecture Deep-Dive

## System Flow

```
GitHub Webhook (PR opened)  --->  WebhookController
                                    |  HMAC validation
REST Client (curl/app)     --->  ReviewController
                                    |
                                    v
                              CodeReviewService
                              (orchestrator)
                                /         \
                               v           v
                        GitHubService   AzureOpenAIService
                        (WebClient)     (WebClient)
                            |               |
                            v               v
                       GitHub API     Azure OpenAI
                       (REST)        (GPT-4o-mini)
```

## Request Flow (Manual)

```
POST /api/review {owner, repo, prNumber}
  |
  v
CodeReviewService.reviewPullRequest()
  |-- GitHubService.fetchPullRequest()  --> GET /repos/:o/:r/pulls/:n
  |-- GitHubService.fetchPRDiff()       --> GET diff
  |-- Truncate if > 12,000 chars
  |-- AzureOpenAIService.chatAsJson()   --> POST Azure OpenAI
  |-- parseResult()                     --> JSON to ReviewResult
  v
200 OK --> ReviewResult JSON
```

## Key Design Decisions

### WebClient over RestTemplate
RestTemplate is deprecated in Spring Boot 3. WebClient is modern,
non-blocking, and using .block() gives synchronous behavior when needed.

### Records for Models
Java records provide immutability, equals/hashCode, toString with zero
boilerplate. Perfect for DTOs that interviewers love to see.

### Virtual Threads for Webhooks
GitHub expects fast webhook responses (< 10s). Reviews take 5-15s.
Virtual threads let us return 200 immediately and process in background.

### No LangChain4j Dependency
For a single chat completion, direct REST is simpler and shows you
understand what happens under the hood rather than hiding behind a framework.

### Diff Truncation at 12K chars
Large diffs produce worse reviews. 12K chars ~= 3K tokens, leaving
room for system prompt and response. Production would split by file.

## Error Handling

| Scenario | Handling |
|----------|---------|
| Invalid GitHub token | 401 from WebClient |
| PR not found | 404, logged |
| Diff too large | Truncated with marker |
| Malformed AI JSON | Regex fallback extraction |
| Bad webhook signature | 401 before processing |
| Non-PR event | Ignored with 200 OK |

## Cost (~$0.004 per review, 250 PRs for $1)

| PR Size | Tokens | Cost |
|---------|--------|------|
| Small (< 500 lines) | ~4K in + ~1K out | ~$0.002 |
| Medium (500-2000) | ~8K in + ~2K out | ~$0.004 |
| Large (truncated) | ~12K in + ~3K out | ~$0.006 |