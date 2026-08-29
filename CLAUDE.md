# CLAUDE.md

Guidance for Claude Code in this repository.

This repo is **core-service** — pantry management, recipes, meal planning, shopping lists, and the optional AI ranking/categorization module. Two sibling services exist in separate repos: **notification-service** (Kafka consumer → email/push) and **telegram-bot-service** (REST client of this service + Kafka consumer). All three communicate exclusively through Kafka domain events (`pantry.item.added`, `pantry.item.expiring`, `recipe.imported`) — this repo never calls the others directly.

## Stack
Spring Boot 4.1.1, Java 21, Maven wrapper, Spring Data JPA, Spring Security, Lombok, PostgreSQL, Kafka. AI features live in this service behind an `AiProvider` interface (Gemini default), gated by an Unleash feature flag — there is no separate `ai-service`. No Elasticsearch, no Redis.

## Commands

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw test -Dtest=ClassName
./mvnw clean install          # full build + tests
./mvnw pmd:check && ./mvnw spotbugs:check
```
Windows: `mvnw.cmd`. Frontend commands live in `frontend/CLAUDE.md`.

## Package structure
Root: `org.example.pantrypilot` → `model / repository / service / controller / config / dto`.

## Auth contract
Access tokens (short-lived JWT) are returned in the JSON body of `/api/auth/register`, `/login`, and `/refresh` as `accessToken` + `expiresIn`. Refresh tokens are **never** in the JSON body — they are set as an httpOnly, SameSite=Strict cookie named `refresh_token` scoped to `Path=/api/auth`, with `Secure=true` in every profile except `local`. `/refresh` and `/logout` read the token from that cookie (`@CookieValue`); `/logout` clears it with `Max-Age=0`. Frontend calls to these endpoints must use `credentials: 'include'`. Never reintroduce refresh tokens in the response body.

## Non-negotiables
- No comments except Javadoc on public APIs — if code needs explaining, rename or extract instead.
- Constructor injection only (`@RequiredArgsConstructor`), never field `@Autowired`.
- Controllers hold no business logic — validate via DTO + `@Valid`, delegate to a service.
- Every new public service method gets a unit test, including an edge case.
- Any AI or Telegram-bot-facing feature must be removable via feature flag without breaking the core product.
- No unfinished features — a feature ships tested and working end-to-end or it doesn't ship.

## Before marking a task complete
Run `code-quality-gate` skill checks. For anything touching UI, also apply `design-system` skill. Don't claim "done" without having actually run the build/test commands above.

## When to load which skill
- Writing or editing a service/controller/repository → `code-quality-gate`
- Touching Kafka producers/consumers → `kafka-patterns`
- Creating or editing any React component/page → `design-system`
- Anything else architecture-related that isn't covered above → ask, don't guess