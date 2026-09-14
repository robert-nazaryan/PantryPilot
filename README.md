# PantryPilot

A pantry inventory and meal-planning app — track what's in your kitchen, manage recipes, generate shopping lists from them, get notified before food expires, and use an in-app AI assistant that can read and modify your data through natural-language commands (with a per-action confirmation step).

The domain is intentionally ordinary; the focus is service design, an event-driven architecture with Kafka, and an AI feature that is optional rather than the point of the app.

Live at [https://pantry-pilot.duckdns.org](https://pantry-pilot.duckdns.org).

**Stack:** Java 21 · Spring Boot 4.1.1 · PostgreSQL 16 · Apache Kafka (KRaft) · React 19 + TypeScript · Vite · Tailwind 4 · Docker Compose · Caddy · GitHub Actions

## Architecture

Three services in-repo, split by responsibility and failure domain. All cross-service communication is one-way through Kafka domain events — no service calls another over REST.

```
                    ┌──────────────┐
                    │   frontend   │
                    │ React + TS   │
                    └──────┬───────┘
                           │ REST (HTTPS via Caddy)
                           ▼
                    ┌──────────────────────┐
                    │      core-service    │
                    │                      │
                    │ • Auth (email + Google OAuth)
                    │ • Pantry / Recipes / Shopping lists
                    │ • AI chat + function-calling
                    │   behind AiProvider (Groq | Gemini)
                    │ • Kafka producer
                    └────┬─────────────────┘
                         │
                    Kafka topics
                    user.registered
                    pantry.item.expiring
                         │
                         ▼
                    ┌──────────────────────┐
                    │ notification-service │
                    │ Kafka consumer       │
                    │ → email via          │
                    │   Gmail SMTP         │
                    └──────────────────────┘
```

**core-service** — auth, pantry, recipes, shopping lists, AI chat. PostgreSQL for storage. The only service the frontend talks to. Publishes domain events after transaction commit.

**notification-service** — Kafka consumer only. Turns `user.registered` into a welcome email and `pantry.item.expiring` into a daily digest. Uses Gmail SMTP (`spring-boot-starter-mail`). Runs independently; if email is misconfigured the core app is unaffected.

**frontend** — Vite SPA, served by nginx in production. React Query for all server state, React Router v7 for routing, dark mode, floating AI-chat widget.

AI lives inside `core-service` behind an `AiProvider` interface with two implementations (`GroqProvider`, `GeminiProvider`) selected by `ai.provider` config. There is no separate AI service — same graceful-degradation behavior (works with or without it) without an extra network hop.

A `telegram-bot-service` was in the original plan but has not been built and is not on the current roadmap.

## Features

- **Auth** — email/password registration + login, Google OAuth2 sign-in. Short-lived JWT access tokens (JSON body) + httpOnly refresh cookie scoped to `/api/auth`.
- **Pantry** — items with name, quantity, unit, category, and optional expiry date. Pagination, expiry-sorted listing.
- **Recipes** — title, free-text instructions, cook time, tag array, nested ingredients (name/quantity/unit).
- **Shopping lists** — multiple lists per user, per-item checked state, generate-from-recipe.
- **Dashboard** — stat tiles, items expiring soon, active shopping lists at a glance.
- **AI chat assistant** — see next section.
- **Email notifications** — welcome email on register; daily expiring-items digest (default 07:00 UTC, 3-day lookahead window; one email per user with at least one item in the window).
- **Dark mode** — toggle on the navbar, class-based (`.dark` on `<html>`), persisted in `localStorage`.
- **Deployment** — see [Deployment](#deployment) below.

One account per user, one inventory per account. No household/multi-tenant model.

## AI chat assistant

A floating chat button (bottom-right on every authenticated page) opens a slide-in panel. The assistant can answer questions about the user's real data ("what's expiring soon", "can I make anything with what's in my pantry") and can propose actions the user then confirms.

**What it can do:**
- **Read** — pantry contents, saved recipes with ingredients, active shopping lists with items. Each turn, a snapshot of this data is included in the system prompt (bounded: 100 pantry items, 30 recipes, 10 lists) so the model reasons over the user's actual state.
- **Propose actions via function calling** — 15 tools currently: `create/update/delete/consume_pantry_item`, `bulk_delete_pantry_items`, `create_shopping_list`, `add/remove/check/uncheck_shopping_list_item`, `generate_shopping_list_from_recipe`, `create/delete_recipe`, `add/remove_recipe_ingredient`.
- **Bulk operations** — a single "empty the pantry" or "delete all X" produces one confirmation card, not N.
- **Multi-turn clarification** — the model can ask "did you mean the 1L or the 2L milk?" and the user's follow-up answer resumes the original request in the same session.

**Confirm-then-execute flow:** the model never mutates data directly. It emits a function call, the backend materializes it into a `ProposedAction` DTO with a resolved preview (e.g. which pantry-item ids will be deleted), the frontend renders an action card, and only after the user clicks Confirm does `POST /api/ai/chat/actions/{id}/confirm` run the underlying service call. Sessions and full chat history persist in Postgres.

**Provider switching:** `ai.provider=groq` (default) uses `openai/gpt-oss-120b` on Groq's OpenAI-compatible endpoint; `ai.provider=gemini` uses Google's Generative Language API. Adding a new provider is one class implementing `AiProvider` + `@ConditionalOnProperty(name = "ai.provider", havingValue = "…")`.

**Fail-soft:** if `ai.chat.enabled=false`, or if the selected provider's API key is missing, `/api/ai/chat` returns `503 ai_unavailable` and the rest of the app runs normally. The frontend catches this and surfaces "The assistant isn't available right now" inside the chat panel; every other feature is unaffected.

## Running locally

```bash
git clone <repo-url>
cd pantrypilot
# Create a .env in the repo root with: DB_USERNAME, DB_PASSWORD, JWT_SECRET,
#   GOOGLE_CLIENT_ID, GOOGLE_CLIENT_SECRET, MAIL_USERNAME, MAIL_PASSWORD,
#   GEMINI_API_KEY, GROQ_API_KEY, AI_PROVIDER (see docker-compose.yml + DEPLOYMENT.md).
docker compose up
```

For hybrid dev (Postgres + Kafka in Docker, services in IntelliJ or `./mvnw`), see [`CLAUDE.md`](./CLAUDE.md#running-locally--three-valid-modes). The `local` profile of each service defaults Kafka bootstrap to `localhost:29092`, which the compose file advertises for host processes.

Frontend dev server: `cd frontend && npm install && npm run dev` (port 5173, expects core-service on `http://localhost:8080`).

## Deployment

Live at [https://pantry-pilot.duckdns.org](https://pantry-pilot.duckdns.org). Hosted on Oracle Cloud Always Free (ARM64 Ubuntu). Every push to `master` triggers `.github/workflows/deploy.yml`:

1. Backend tests + PMD + SpotBugs + frontend typecheck + lint.
2. Multi-arch (`linux/arm64`) build of core-service, notification-service, frontend → push to GHCR with `<sha>` and `latest` tags.
3. SSH into the host, `scp` the current `docker-compose.prod.yml` + `Caddyfile`, write `~/pantrypilot/.env` from GitHub Secrets, `docker compose pull && up -d`.
4. Post-deploy health check against frontend root + `/api/auth/login`.

Caddy 2 terminates TLS (automatic Let's Encrypt) and reverse-proxies to the frontend (nginx) and core-service. Postgres and Kafka run on the same host with named volumes; notification-service is on the internal Docker network only.

Full pipeline notes, GitHub Secrets list, and one-time server setup are in [`DEPLOYMENT.md`](./DEPLOYMENT.md).
