# Technical Skills Demonstrated

## Java 17
- **Records** for immutable DTOs (ReviewResult, CodeIssue, PullRequestData)
- **Text blocks** (triple-quote) for multi-line prompt templates
- **Virtual threads** (Thread.startVirtualThread) for async webhook processing
- **Pattern matching** in switch expressions (severity icon mapping)
- **Formatted strings** with .formatted() method

## Spring Boot 3.2
- @ConfigurationProperties with record binding
- WebClient (reactive HTTP) for GitHub + Azure OpenAI
- @Qualifier for multiple WebClient beans
- Layered architecture: Controller > Service > External APIs
- YAML config with environment variable fallbacks

## AI / LLM Integration
- Direct Azure OpenAI REST integration (no SDK dependency)
- Structured JSON output prompt engineering
- Multi-category analysis (bugs, optimization, docs, security)
- JSON parsing with regex fallback for malformed output
- Temperature 0.1 for deterministic structured responses

## API & Security
- RESTful endpoints with proper HTTP methods
- GitHub webhook with HMAC-SHA256 signature verification
- Secrets via environment variables, never hardcoded
- Async processing with virtual threads (non-blocking webhook)

## Architecture Patterns
- Service layer with constructor dependency injection
- Immutable record-based models (zero boilerplate)
- Separation of concerns: controllers have no business logic
- Configuration-driven design via application.yml

## Skills Matrix

| Skill | Technologies | Level |
|-------|-------------|-------|
| Java 17 | Records, Virtual Threads, Text Blocks | Expert |
| Spring Boot 3 | WebClient, Config Properties, DI | Expert |
| REST API Design | Controllers, Webhooks | Expert |
| GitHub API | PR Diffs, Comments, Webhooks | Advanced |
| Azure OpenAI | Chat Completions, JSON Mode | Expert |
| Prompt Engineering | Structured Output, Rubrics | Expert |
| Security | HMAC, Secret Management | Advanced |

## Target Roles
- Senior Java Developer
- GenAI / AI Engineer
- Backend Engineer
- Staff Engineer