# AI Code Review Assistant

AI-powered GitHub PR review assistant built with **Java 17**, **Spring Boot 3.2**,
**GitHub API**, and **Azure OpenAI**.

## Features

| Feature | Description |
|---------|-------------|
| Bug Detection | Null pointers, race conditions, resource leaks, logic errors |
| Code Optimization | O(n2) algorithms, N+1 queries, unnecessary allocations |
| Documentation | Missing Javadoc, unclear naming, TODO without tickets |
| Security Scan | SQL injection, XSS, hardcoded secrets, input validation |
| REST API | POST endpoint to trigger reviews programmatically |
| GitHub Webhook | Auto-review when PRs are opened or updated |
| PR Comments | Posts structured review as a GitHub PR comment |

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+
- GitHub Personal Access Token (repo scope)
- Azure OpenAI resource

### 1. Build

```bash
cd ai-code-review
mvn clean package -DskipTests
```

### 2. Set environment variables

```bash
export GITHUB_TOKEN=ghp_your_token_here
export AZURE_OPENAI_API_KEY=your-azure-key
export AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
export AZURE_DEPLOYMENT_NAME=gpt-4o-mini
```

### 3. Run

```bash
mvn spring-boot:run
```

### 4. Trigger a review

```bash
curl -X POST http://localhost:8080/api/review \
  -H "Content-Type: application/json" \
  -d '{"owner":"octocat","repo":"hello-world","prNumber":1}'
```

### 5. Review AND post as GitHub comment

```bash
curl -X POST http://localhost:8080/api/review-and-comment \
  -H "Content-Type: application/json" \
  -d '{"owner":"your-org","repo":"your-repo","prNumber":42}'
```

## GitHub Webhook Setup

1. Repo Settings > Webhooks > Add webhook
2. Payload URL: https://your-server.com/api/webhook/github
3. Content type: application/json
4. Secret: set and export as GITHUB_WEBHOOK_SECRET
5. Events: Pull requests

## License

MIT