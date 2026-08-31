# Deployment

Production runs on a single ARM64 Ubuntu host at `84.235.240.84`, reachable at `https://pantry-pilot.duckdns.org`. Every push to `master` triggers automatic deploy via `.github/workflows/deploy.yml`.

## Branch convention (CI vs CD split)

- `develop` is the day-to-day work branch. Push to `develop` runs `.github/workflows/ci.yml` — backend tests + PMD + SpotBugs + frontend typecheck + lint. No build-and-push, no deploy, no contact with production.
- `master` is production. Push to `master` runs `.github/workflows/deploy.yml` — same verification, then multi-arch image build → GHCR push → SSH deploy → post-deploy health check.
- PRs targeting `master` also run CI, so a `develop → master` PR is verified before merge.
- Manual re-deploys are still possible via the `workflow_dispatch` trigger on `deploy.yml` (Actions tab → Deploy → Run workflow).

## Promoting `develop` to production

When `develop` is verified and ready to deploy:

```bash
git switch master
git pull --ff-only origin master
git merge --ff-only develop        # fast-forward only; if merge conflict, resolve on develop first
git push origin master             # triggers deploy.yml automatically
git switch develop                 # go back to day-to-day branch
```

Prefer `--ff-only` on the merge so `master`'s history is a clean linear projection of `develop`. If it refuses to fast-forward, that means `master` has diverged (e.g. a hotfix landed there directly) — investigate before forcing anything.

## Stack
- **postgres**: 16-alpine, named volume `postgres_data`.
- **kafka**: bitnami/kafka:3.7 in KRaft mode, named volume `kafka_data`. Internal-only (no port exposed).
- **core-service**: Spring Boot 4 image, built by CI, pulled from GHCR. Kafka producer.
- **notification-service**: Spring Boot 4 image, built by CI, pulled from GHCR. Kafka consumer, sends email via Gmail SMTP. Internal-only (no port exposed).
- **frontend**: Vite build served by nginx:alpine (SPA fallback), image built by CI, pulled from GHCR.
- **caddy**: 2-alpine reverse proxy, automatic Let's Encrypt HTTPS for `pantry-pilot.duckdns.org`.

## Build & release model
CI builds `linux/arm64` images with Docker Buildx (QEMU) on the x86_64 GitHub runner and pushes them to `ghcr.io/<owner>/pantrypilot-core-service` and `ghcr.io/<owner>/pantrypilot-frontend`. Each image is tagged with both the 7-char commit SHA and `latest`. The server only ever *pulls* — no source builds on the host. Roll back with `IMAGE_TAG=<sha> docker compose -f docker-compose.prod.yml up -d`.

## What happens on push to master
1. **test** job: `./mvnw clean install` (PMD, SpotBugs, tests) + `npm run typecheck && npm run lint`. Fails the workflow if any step fails — no deploy.
2. **build-and-push** job: multi-arch (arm64) build of both images, push to GHCR with `<sha>` and `latest` tags, GHA cache scoped per image.
3. **deploy** job:
   - SSH into the server using `SSH_PRIVATE_KEY`.
   - `scp` the current `docker-compose.prod.yml` and `Caddyfile` to `~/pantrypilot/` (source of truth is the repo).
   - Write `~/pantrypilot/.env` from GitHub Secrets — overwrites on every deploy, `chmod 600`. **This is the only production `.env` — it is not tracked in git anywhere.**
   - `docker login ghcr.io` on the server using the workflow's `GITHUB_TOKEN` (read-only pull scope is enough for the pull step).
   - `docker compose -f docker-compose.prod.yml pull && up -d --remove-orphans`.
   - Post-deploy health check: `curl` the frontend root (expect 200) and `POST /api/auth/login` with `{}` (expect 400 — proves the endpoint is wired and reachable). Retries for up to 90s. Fails the workflow visibly if the new containers don't come up.

Flyway migrations run automatically inside `core-service` at startup (`spring.flyway.enabled=true` in `application.yaml`) — no separate migration step is needed.

## GitHub Secrets

Every secret below must be configured in **Repo Settings → Secrets and variables → Actions** before the first deploy.

| Secret | How to obtain | Notes |
|---|---|---|
| `SSH_PRIVATE_KEY` | The private half of the SSH key whose public half is in `~ubuntu/.ssh/authorized_keys` on the server. Paste the *full* PEM (`-----BEGIN OPENSSH PRIVATE KEY-----` through `-----END OPENSSH PRIVATE KEY-----`). | Create dedicated deploy key: `ssh-keygen -t ed25519 -f deploy_key -N ""`; copy `deploy_key.pub` to the server's `authorized_keys`; paste `deploy_key` here. |
| `SSH_HOST` | `84.235.240.84` | The server's public IP. |
| `SSH_USER` | `ubuntu` | |
| `JWT_SECRET` | `openssl rand -base64 48` | **Do not reuse the local dev value.** Generate a fresh one. HS256 requires ≥256 bits; 48 base64 bytes = 288 raw bits. |
| `DB_USERNAME` | Choose a name (e.g. `pantrypilot`). | Used both as Postgres user and by core-service to connect. |
| `DB_PASSWORD` | `openssl rand -base64 24` | Strong random. |
| `GOOGLE_CLIENT_ID` | Google Cloud Console → APIs & Services → Credentials → your OAuth 2.0 Client ID. | Same OAuth client as local dev — no new client needed. |
| `GOOGLE_CLIENT_SECRET` | Same Google Cloud entry. | |
| `MAIL_USERNAME` | Full Gmail address of the sending account (e.g. `pantrypilot.notify@gmail.com`). Also used as the `From:` address on outgoing mail. | notification-service uses this to authenticate with Gmail SMTP. If unset (empty), notification-service still runs — it logs a warning and skips sending. Real send failures are NOT swallowed; they trigger the DLT flow per the kafka-patterns skill. |
| `MAIL_PASSWORD` | Gmail **App Password** (16 chars, no spaces) generated at https://myaccount.google.com/apppasswords — the sending account must have 2-Step Verification enabled first. Do NOT use the account's login password. | Rotate by regenerating the App Password in Google Account settings and updating this secret. |

`GITHUB_TOKEN` is provided by Actions automatically — no manual step needed. The `packages: write` permission is granted per-job in the workflow so the token can push to GHCR.

## Manual step in Google Cloud Console (one-time)

The Google OAuth client used for local dev has an existing Authorized redirect URI (`http://localhost:8080/login/oauth2/code/google`). You must add the production URI so the same client works for both:

1. Go to **Google Cloud Console** → the project hosting this OAuth client.
2. **APIs & Services → Credentials**.
3. Click the OAuth 2.0 Client ID row.
4. Under **Authorized redirect URIs**, click **+ ADD URI**.
5. Paste exactly: `https://pantry-pilot.duckdns.org/login/oauth2/code/google`
6. Click **Save**.

Without this, Google will reject the OAuth callback with `redirect_uri_mismatch`. Nothing in the CI or the app can automate this — it's a Google-side config change.

## One-time server setup (already done — for reference / disaster recovery)

```bash
# On the server as ubuntu:

# 1. Docker + Compose plugin (Ubuntu 22.04+)
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu   # then log out/in

# 2. Firewall — allow 80/443 (and 22 for SSH)
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable

# 3. Deploy directory (compose files land here on every deploy)
mkdir -p ~/pantrypilot

# 4. DuckDNS
#    Register pantry-pilot.duckdns.org → 84.235.240.84 at duckdns.org.
#    Cron a refresh (DuckDNS handles cert issuance via Caddy; DNS record just needs to stay pointed correctly):
#      */5 * * * * curl -sS "https://www.duckdns.org/update?domains=pantry-pilot&token=<TOKEN>&ip=" >/dev/null

# 5. SSH deploy key
#    Locally: ssh-keygen -t ed25519 -f deploy_key -N ""
#    Copy deploy_key.pub → server's ~ubuntu/.ssh/authorized_keys
#    Paste deploy_key contents → GitHub repo secret SSH_PRIVATE_KEY.
```

The first deploy after this creates `~/pantrypilot/.env`, pulls images, and Caddy provisions the TLS certificate on first request to `https://pantry-pilot.duckdns.org`.

## Local dev is untouched

Local dev still uses `docker-compose.yml` + the repo-root `.env` (gitignored). None of the deployment changes affect the local workflow — start locally exactly as before (`docker compose up`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=local`, `npm run dev`).
