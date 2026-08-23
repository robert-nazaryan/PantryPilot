# PantryPilot

A pantry inventory and meal-planning app — track what's in your kitchen, manage recipes, generate shopping lists, and get notified before food expires.

The domain is intentionally ordinary; the focus is service design, an event-driven architecture with Kafka, and an AI feature that's optional rather than the point of the app.

**Stack:** Java 21 · Spring Boot 4.1.1 · PostgreSQL · Kafka · React + TypeScript · Docker Compose

## Architecture

Three services, split by responsibility and failure domain:

```
                    ┌──────────────┐
                    │   Frontend   │
                    │ React + TS   │
                    └──────┬───────┘
                           │ REST
                           ▼
                    ┌──────────────┐
                    │ core-service │
                    │              │
                    │ • Pantry     │
                    │ • Recipes    │
                    │ • Shopping   │
                    │   lists      │
                    │ • Auth       │
                    │ • AI ranking │ (behind AiProvider + feature flag)
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
    │ Kafka → email/push │      │ REST client of        │
    │                    │      │ core-service +        │
    │                    │      │ Kafka consumer        │
    └────────────────────┘      └──────────────────────┘
```

**core-service** — pantry, recipes, meal planning, shopping lists, and AI ranking/categorization behind an `AiProvider` interface + feature flag. PostgreSQL for storage. The only service the frontend talks to.

**notification-service** — Kafka consumer, turns domain events into email/push notifications. Runs independently so an email provider outage doesn't affect the core app.

**telegram-bot-service** — optional, feature-flagged. REST client of core-service, plus a Kafka consumer for proactive expiry reminders.

AI lives inside `core-service` rather than as a fourth service — same graceful-degradation behavior (works with or without it) without an extra network hop or deploy target.

## Features

- Pantry tracking: add/consume items, quantity, category, expiry date
- Recipes: create/edit, ingredients, steps, cook time, tags
- Shopping mode: mobile checklist, tick items off as you buy
- Shopping list generation from recipes + current inventory
- Expiry alerts (email/push/Telegram)
- Recipe search/filtering (Postgres full-text)
- Barcode scanning via Open Food Facts API
- AI: "what can I cook with what I have," auto-categorization — both optional, feature-flagged, degrade to normal app behavior when off

One account per user, one inventory per account. No separate household entity — a family shares a login if they want to.

## Running locally

```bash
git clone <repo-url>
cd pantrypilot
cp .env.example .env   # fill in local values
docker-compose up
```

## Deployment

Oracle Cloud Always Free (ARM), Docker Compose, Caddy for automatic HTTPS, GitHub Actions for build/push/deploy.