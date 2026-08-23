# PantryPilot

A self-hosted pantry & meal-planning system, built as a portfolio project to demonstrate production-grade backend engineering: service boundary design, event-driven architecture with Kafka, an AI feature that degrades gracefully instead of being the product's center, and a project taken all the way to a monitored, deployed state — not just "works on my machine."

The domain (pantry tracking / meal planning) is intentionally ordinary. The point of this project is not the idea — it's the design and engineering discipline behind it.

## Why this project exists

Two years of commercial Java/Spring Boot experience, but no single side project that shows real engineering maturity end-to-end. PantryPilot exists to prove:

- Service boundaries can be designed and justified, not just implied by folder structure
- Kafka is used where asynchronicity is actually warranted, not added for show
- AI is integrated as an optional, gracefully-degrading enhancement — a more mature pattern than the typical "AI wrapper" side project
- A project can be carried through to completion: tested, containerized, deployed, monitored
- Clean Code / SOLID are enforced as checkable rules (static analysis, CI, `CLAUDE.md`), not a slogan

## Features

**Core**
- JWT-based registration and login
- Pantry inventory management — add/consume items, quantity, unit, category, expiry date
- Recipe library — create/edit recipes with ingredients, steps, cook time, tags
- Shopping mode — mobile-friendly checklist, tick items off as you buy them
- Shopping list generation from planned recipes and current inventory

**Quality of life**
- Expiry alerts (push/email/Telegram) for items nearing their use-by date
- Recipe search and filtering by tag, cook time, and ingredients (Postgres full-text search)
- Barcode scanning via phone camera + Open Food Facts API for fast item entry

**AI-assisted (optional, feature-flagged)**
- "What can I cook with what I have" — ranks recipes by inventory match
- Automatic category suggestion for newly added items

Every AI feature is built to degrade gracefully: if the underlying provider is unavailable or the feature flag is off, the app behaves exactly as it does without AI.

**Telegram bot (optional, feature-flagged)**
- Add/consume pantry items via chat commands
- "What can I cook" queries answered directly in chat
- On-demand shopping list
- Proactive expiry reminders, driven by the same Kafka event stream used for email/push notifications

**Operations**
- Full stack runs with a single `docker-compose up`
- Automated tests (JUnit, Mockito) across all services
- CI/CD via GitHub Actions: build → push image → deploy
- Reverse proxy with automatic HTTPS
- Basic monitoring and alerting

**Account model:** one account per user, one inventory per account. A household shares a single login — there is no separate multi-tenant "household" entity.

## Architecture

Three deployable services, split by responsibility and failure domain rather than by feature. The AI capability lives inside `core-service` behind a provider interface and a feature flag, rather than as a fourth network hop — the same graceful-degradation guarantee is achieved without the operational cost of an extra deployed service, extra health checks, and an extra point of network failure.

```
                    ┌──────────────┐
                    │   Frontend   │
                    │ React + TS   │
                    └──────┬───────┘
                           │ REST
                           ▼
                    ┌──────────────┐         ┌──────────────────┐
                    │ core-service │────────▶│  AiProvider       │
                    │              │         │  (Gemini, behind  │
                    │ • Pantry     │         │   interface +     │
                    │ • Recipes    │         │   Unleash flag)   │
                    │ • Shopping   │         └──────────────────┘
                    │   lists      │
                    │ • Auth       │
                    └──────┬───────┘
                           │
                     Kafka topics
                pantry.item.added
              pantry.item.expiring
                 recipe.imported
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
    ┌──────────────────┐      ┌──────────────────────┐
    │ notification-     │      │ telegram-bot-service  │
    │ service           │      │                       │
    │                    │      │ • REST client of      │
    │ Consumes Kafka     │      │   core-service        │
    │ events → email /   │      │ • Kafka consumer for  │
    │ push notifications │      │   proactive reminders │
    └────────────────────┘      └──────────────────────┘
```

**core-service** — the product itself: pantry inventory, recipes, meal planning, shopping list generation, and the optional AI ranking/categorization module behind `AiProvider`. Root package `org.example.pantrypilot`, organized as `model / repository / service / controller / config / dto`. Backed by PostgreSQL. The only service the frontend talks to directly.

**notification-service** — a Kafka consumer that turns domain events (`pantry.item.expiring`, etc.) into email/push notifications. Isolated so that an email provider outage never affects the core product.

**telegram-bot-service** — an optional interface layer, feature-flagged. Acts as both a REST client of `core-service` and a Kafka consumer for proactive reminders, reusing the same event stream as `notification-service`.

Kafka topics carry domain events between services (`pantry.item.added`, `pantry.item.expiring`, `recipe.imported`, and similar), keeping all three services independently deployable.

### Design principles

- **Product first** — every AI and Telegram feature can be switched off via feature flag without breaking anything else
- **No unfinished features** — everything shipped works end-to-end, including tests, before moving on
- **Clean Code / SOLID enforced mechanically** — static analysis and Claude Code rules (`CLAUDE.md`, `.claude/skills/`), not just convention
- **Deliberate, non-templated visual design** — a defined palette, type scale, and spacing system, mobile-first, checked at 375px and 1440px

## Tech stack

**Backend**
Java 21, Spring Boot 4.1.1, Maven (wrapper included — no global install needed), Spring Data JPA, Spring Security, Lombok

**Messaging**
Apache Kafka

**Data storage**
PostgreSQL

**AI**
Spring AI / LangChain4j, with Google Gemini (free tier) as the default provider, abstracted behind an `AiProvider` interface so Groq/OpenRouter can be swapped in

**Feature flags**
Unleash

**Frontend**
React + TypeScript, Tailwind CSS, functional components, React Query for server state

**Bot**
Telegram Bot API (`telegrambots` Java library)

**Code quality**
Checkstyle, PMD, SpotBugs (backend); ESLint, strict TypeScript (frontend)

**Infrastructure**
Oracle Cloud Always Free (ARM VM, 4 OCPU / 24GB RAM, no time limit), Docker Compose, GitHub Actions CI/CD, Caddy as reverse proxy with automatic TLS

## Running locally

```bash
git clone <repo-url>
cd pantrypilot
docker-compose up
```

This brings up all three services, PostgreSQL, Kafka, and Unleash. The frontend is served separately during development via its own dev server; see `frontend/README.md`.

## Deployment

Self-hosted on an Oracle Cloud Always Free ARM instance. The full stack runs via Docker Compose behind Caddy, which handles automatic HTTPS. Deployment is automated through GitHub Actions: build → push image → deploy. Basic monitoring and alerting close the operational loop.