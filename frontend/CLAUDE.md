# CLAUDE.md (frontend)

Guidance for Claude Code when working in `frontend/`. This file applies only within this directory; see the repo-root `CLAUDE.md` for backend/architecture context.

## Stack
React + TypeScript (strict mode, no `any`), Vite, Tailwind CSS, React Query for all server state, React Router for routing, `lucide-react` for icons. Functional components only — no class components.

## Commands
```bash
npm install
npm run dev          # local dev server, port 5173
npm run build
npm run lint          # ESLint
npm run typecheck     # tsc --noEmit
npm run test          # unit tests
```

## Structure
```
frontend/src/
  components/     # shared, reusable UI — check here before creating anything new
  pages/          # route-level components
  hooks/          # custom hooks, incl. React Query hooks (useXxxQuery / useXxxMutation)
  api/            # typed API client functions, one file per backend resource
  types/          # shared TypeScript types/interfaces
  context/        # React context providers (e.g. AuthContext)
```

## Auth contract — locked in, do not deviate

core-service issues short-lived access tokens in the JSON response body and long-lived refresh tokens as an httpOnly cookie (`Path=/api/auth`, `SameSite=Strict`). This split is deliberate: the access token is readable by JS (and short-lived, so the exposure window is small), the refresh token never is.

- **Access token**: held only in memory (React context/state). Never `localStorage`, never `sessionStorage` — those are readable by any script on the page and would expose the token to XSS.
- **Refresh token**: the frontend never reads or stores it directly. Every request to `/api/auth/*` must include `credentials: 'include'` so the browser sends/receives the cookie automatically.
- **Session restore on load**: call `POST /api/auth/refresh` with `credentials: 'include'` when the app mounts. If the httpOnly cookie is still valid, this silently restores the session without ever exposing a token to storage.
- **Proactive refresh**: schedule a refresh before the access token's `expiresIn` elapses (e.g. at ~80% of the TTL) rather than waiting for a request to fail.
- **Reactive refresh**: on a `401` from any authenticated endpoint, attempt one silent refresh-and-retry before redirecting to `/login`.
- All of this lives in one reusable API client (fetch wrapper or axios instance with interceptors) that every feature's `api/` module uses — don't reimplement token attachment or refresh handling per-feature.

## Environment / config
- API base URL comes from a Vite environment variable (e.g. `VITE_API_BASE_URL`), never hardcoded — local dev points at `http://localhost:8080`, but this must be easy to change for later deployment.

## Non-negotiables
- No `any`, no `// @ts-ignore` without a one-line justification in the PR description (not inline in code).
- Server state (anything from core-service's REST API) goes through React Query — never manual `useEffect` + `fetch`/`axios`.
- Local/UI-only state (form inputs, toggles, modal open/closed) uses `useState`/`useReducer` — don't put it in React Query or a global store.
- Every component must be built mobile-first and responsive in the same pass — see `design-system` skill before writing any UI.
- Reuse a component from `components/` before creating a new one with similar purpose.

## Before marking a UI task complete
Apply the `design-system` skill checklist. Run `npm run lint && npm run typecheck` and confirm both pass with zero warnings, not just zero errors.