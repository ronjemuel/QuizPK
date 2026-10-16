# QuizPK backend — M4.3 Classic Match review

M4.2 lobbies are user-approved on real phones. This source extends the existing
Node 24/SQLite service with Classic-only gameplay: owner-authorized start,
server-selected validated questions, synchronized deadlines, atomic answers,
server scoring, safe reveal, automatic progression and durable final results.
No new dependencies or provider requests are needed. All existing M3 API/auth/
quota behavior and Moderator lobbies remain.

Review the [Classic contract](../docs/M4_3_CLASSIC.md),
[schema/deployment/fallback plan](../docs/M4_3_DEPLOYMENT_PLAN.md) and
[acceptance plan](../docs/M4_3_DEVICE_TEST_PLAN.md). Deploy the complete `src/`,
including `multiplayer/classic-bank.json`, only after separate approval. Startup
applies additive multiplayer schema 2 on the configured private DB. Unmodified
M4.2 source rejects schema 2; fallback keeps this source/DB and sets
`QUIZPK_CLASSIC_ENABLED=false`. Never restore an older DB to downgrade.

Run `npm test` for isolated mock/SQLite/security regressions. No live host, DB,
provider key, token file, service or tunnel is modified by those tests. Real
M4.3 phone/public-HTTPS acceptance remains pending. Historical M4.1 setup follows.

---

# Original M4.1 foundation

A dependency-free Node.js 24 service for a small invited class. The existing M3
Android app calls `POST /api/quizzes/generate` over HTTPS. M4.1 adds authenticated
private lobby operations under `/api/rooms`; their Android UI is deferred to M4.2.
Gemini credentials never leave the server.
The existing workspace key remains at `../GEMINI_API_KEY/GEMINI_API_KEY.env`; the
loader explicitly reads that exact filename. It also supports the backend-only
`./GEMINI_API_KEY.env`. Hosting environment variables take precedence. No file is
served publicly, and the backend is not part of the Android Gradle modules.

## Current status

The replacement M4 brief records user-approved M1–M3 physical checks, real Gemini
generation and offline replay. This stage did not repeat live provider/phone tests
or change the running host. Backend tests pass **54/54**, including the original
20 M3 tests; Android regressions pass **56/56**. No paid billing was enabled.

M4.1 implements only room/membership schema, roles, capacity, code expiry, readiness,
settings, presence, authorized snapshots and bounded/idempotent commands. No live
round, queue, answer or LAN service exists. Review [M4 setup](../docs/M4_SETUP.md),
[common protocol](../docs/M4_PROTOCOL.md), [architecture](../docs/M4_ARCHITECTURE.md)
and [tests](../docs/TEST_NOTES.md). Stop for review before M4.2. The deployment and
generation contract below remain valid; earlier unverified statements describe
the original M3 implementation session, not the user's subsequent acceptance.

## Model and free tier

Selected recommendation: `gemini-3.5-flash-lite`, checked against Google's official
documentation on **2026-10-12**:

- [Model code and structured output support](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite)
- [Standard free-tier pricing](https://ai.google.dev/gemini-api/docs/pricing#gemini-3.5-flash-lite)
- [Structured outputs](https://ai.google.dev/gemini-api/docs/structured-output)
- [REST generation configuration](https://ai.google.dev/api/generate-content#v1beta.GenerationConfig)

The Gemini 2.5 model page now limits new-project access, so 2.5 was not selected.
The REST implementation uses `responseMimeType: application/json` with
`responseJsonSchema`, then independently validates the result. Thinking is LOW for
Gemini 3; changing to an earlier supported 2.x model switches to a zero thinking
budget. The model is configured on the backend, never in the APK. Test fixtures
identify themselves as mocks and prove neither model availability nor fact accuracy.

Check your actual API project in Google AI Studio: the project must remain on the
**free tier**, with no billing upgrade. Google consumer Plus/Pro plans do not supply
API credits/quota. Free-tier capacity varies; the service fails on provider limits
without falling back to paid models or automatic retries. Free-tier inputs may be
used by Google to improve products; do not submit private class/student information.
`GEMINI_FREE_TIER_CONFIRMED=true` is an operator confirmation, not a billing check:
the API does not offer a per-request "free only" switch. Do not set it on a paid
project without separate authorization. The service defaults to generation disabled.

## Local development

From `backend/`, with Node.js 24 available (no npm install needed):

```sh
npm test
npm run check-config
```

The second command prints only booleans. The existing credential file is already in
valid `GEMINI_API_KEY=<secret>` dotenv form. Keep it out of `app/`, public/static
folders, commits, logs and distributable archives. `.env.example` deliberately has
blank key and model values. Bare-key files must be privately edited to dotenv form;
never paste credentials into chat or shell command arguments.

After checking the project is free-tier, set non-secret config for the process:

```sh
GEMINI_MODEL=gemini-3.5-flash-lite GEMINI_FREE_TIER_CONFIRMED=true npm start
```

This binds **127.0.0.1:8787**. It is a development service, not a classmate deployment.
Android intentionally refuses cleartext, including localhost HTTP. Backend HTTP
mock tests need no Android cleartext exception. To make one bounded live-provider
check after free-tier confirmation (five Easy Java questions; no output content):

```sh
GEMINI_MODEL=gemini-3.5-flash-lite GEMINI_FREE_TIER_CONFIRMED=true npm run smoke
```

That smoke script performs exactly one provider request and prints only a result
flag, count, or sanitized error code. It is an operator-only diagnostic that bypasses
HTTP quotas; do not expose it as an endpoint or use it in an automated retry loop.
It has **not been run against Gemini in this implementation session**.

## Deploy behind HTTPS on a Linux server

This architecture fits Termux development and small private classes without adding
Firebase SDKs or a distributed datastore. It needs **one persistent Node process and
persistent SQLite storage**. Use an existing Linux VPS or host with equivalent
persistent storage and TLS. **Do not deploy this SQLite version to Vercel Functions,
ephemeral serverless instances, or multiple replicas**: their storage/concurrency
semantics would invalidate quotas and idempotency. Scaling needs a shared durable
database and shared rate limiter. No hosting account or server was supplied here.

1. On your chosen server install Node.js 24, Caddy, and a dedicated `quizpk` system
   user. Point a real domain's DNS at that server and permit inbound ports 80/443.
   Follow the official [Caddy installation guide](https://caddyserver.com/docs/install).
   Obtain permission before enabling any paid hosting plan.
2. Copy **only** `backend/src/`, `backend/package.json`, and these example deployment
   files to `/opt/quizpk/backend`. Never copy local env files, tokens, Android files,
   reference assets, or the complete workspace. Create `/var/lib/quizpk` owned by
   `quizpk`, mode 700, for the durable database.
3. Privately create `/etc/quizpk/backend.env` (directory mode 700; file owned by
   `quizpk`, mode 600) in the server's secret settings or a private editor. It needs:

   ```dotenv
   GEMINI_API_KEY=
   GEMINI_MODEL=gemini-3.5-flash-lite
   GEMINI_FREE_TIER_CONFIRMED=true
   ```

   Supply the replacement key privately in the blank value. This file is read by
   systemd and is not publicly served. The local Termux file is not automatically
   present on your host. **Rotate the previously shared Gemini key before public
   release**, and update all deployed secret settings with its replacement.
4. Review `quizpk.service.example`, adapt the absolute Node path if needed, install
   it as `/etc/systemd/system/quizpk.service`, then:

   ```sh
   sudo systemctl daemon-reload
   sudo systemctl enable --now quizpk
   ```

5. Adapt `Caddyfile.example` to your real domain and install it in your Caddy
   configuration. The `.invalid` domain is an explicitly unusable example, never
   an Android default. Validate/reload Caddy using its installed service. Caddy
   terminates HTTPS and overwrites `X-Forwarded-Proto`. No web root or access-body
   logging is configured. Keep port 8787 on loopback; production startup refuses
   a non-loopback HOST. Health is `GET /health`, with only `{"status":"ok"}`.
   See [Caddy reverse proxy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy).
6. Create `/opt/quizpk/backend/private` owned by `quizpk`, mode 700 (the CLI needs
   write access to this directory; the source directory can remain read-only).
   Provision one distinct user token per classmate on the server as `quizpk`, using
   the **same** `QUIZPK_DB_PATH=/var/lib/quizpk/quizpk.sqlite` as the service:

   ```sh
   cd /opt/quizpk/backend
   QUIZPK_DB_PATH=/var/lib/quizpk/quizpk.sqlite node src/create-user.mjs classmate01
   ```

   The CLI creates a random 256-bit token in `backend/private/classmate01.token`;
   it prints no token. Only a SHA-256 hash is stored in SQLite. Transfer each file's
   contents privately to its owner, then remove the transfer file when no longer
   needed. Do not put tokens in APKs, public configuration or issue trackers.
   For revocation use `node src/revoke-user.mjs classmate01` with the same DB path.
   Use a new user ID for replacement access; old access remains revoked.
7. In Android's **AI Quiz → Connect to QuizPK**, enter the real HTTPS origin
   (no path/query) and that user's personal token. It is encrypted with Android
   Keystore AES-GCM; login/pending preferences are excluded from cloud backup and
   device transfer. The app contains no provider credential or production URL.
   Uninstall/restore may require reconnecting. Disconnect removes stored login.
8. Check health over HTTPS, verify unauthorized generation is rejected, then
   perform a five-question generation through Android. Complete the device
   checklist in `../docs/TEST_NOTES.md`, including airplane-mode replay and M2
   history. Until this succeeds, real end-to-end generation remains unverified.

Keep the database and its WAL together when backing up; use SQLite's backup API or
stop the service first. Do not delete/reset the backend DB to clear an error: doing
so would lose users, quotas and idempotency records. Store backups privately because
cached quizzes can contain user topics. No cache eviction is automatic; at the
initial global limit there are at most 20 new records/day. Before introducing data
retention, preserve permanent idempotency tombstones or reject expired request IDs.

## Contract, access and limits

Request headers: `Content-Type: application/json`, `Authorization: Bearer <personal
access token>`, `Idempotency-Key: <UUID>` (36 lowercase hex/dash characters).
Request JSON has exactly `topic` (3–120 trimmed characters, a letter or digit),
`difficulty` (Easy/Normal/Hard/Expert), `questionCount` (5/10/15/20), `timerSeconds`
(10/15/20/30), and optional `additionalInstructions` (up to 500 characters).
Unknown fields, client scores/roles/quota controls, coercions, and overlong bodies
are rejected. Body cap is 8 KB. Input instructions are untrusted prompt data.

Response schema version **1**:

```json
{
  "schemaVersion": 1,
  "generationId": "the-request-uuid",
  "quiz": {
    "topic": "Java Programming",
    "difficulty": "Hard",
    "questionCount": 5,
    "timerSeconds": 20,
    "createdAt": 1791763200000,
    "origin": "AI_GENERATED",
    "model": "configured-model-id",
    "questions": []
  }
}
```

The empty array above illustrates envelope fields only: a successful response
requires **exactly the requested question count**. Each entry has `text`, four
`options`, **zero-based** `correctIndex` (0–3), `explanation`, `topic`, `difficulty`.
Android generates persistent UUIDs locally and validates the same boundaries.
Unicode-normalized duplicate questions/options and explanation copies are rejected.
Topic metadata is tied to the validated request; prompt instructions require
on-topic content. These checks cannot prove factual accuracy or full topicality.
AI answers require review and must not be authoritative grades without verification.

Initial hard limits: 5 generations/user/day, 2/user/minute, 20 globally/day,
1 pending/user and 2 global concurrent generations. Each failed or uncertain
provider attempt consumes a reservation. IP/socket throttle is 30 requests/minute;
behind the single proxy this is intentionally a conservative global ingress cap,
not a forged forwarded-IP identity. Fixed quotas cannot be overridden by clients.
Provider timeout is 55 seconds; tokens are bounded to `min(12000, count*500+500)`;
response body cap 180 KB. No tools, grounding, paid fallback, or automatic repair/
provider retries. Token budgets and quota limits also bound accidental paid use,
but do not replace operator free-tier confirmation.

SQLite transactions reserve quota/idempotency before the provider call. Retries
return the cached response without another call. Different input with the same key
gets 409. An in-flight duplicate gets 425; a stale uncertain job gets 409 and is
never automatically repeated. Provider/server terminal errors are cached. Android
uses the same request ID after a transport timeout/loss, cancellation or process
interruption; after an explicit terminal response the next manual generation uses
a new ID. There is no automatic retry loop. Cancellation stops client waiting;
an already accepted server job can finish and consume its quota.

Sanitized errors: `{"schemaVersion":1,"error":{"code":"..."}}`. Common status
codes: 400 invalid request, 401 auth, 409 conflict/interrupted, 422 rejected topic,
425 in progress, 429 quota/provider cap, 502 malformed questions, 503 setup/outage,
504 generation timeout. Error bodies contain no upstream payloads or stack traces.
No credentials, requests, model responses or personal scores are logged. Production
reverse proxy/process must retain this policy. This server never accepts game scores
or accepts client-declared multiplayer privileges. Room permissions are assigned
by the separate lobby authority; its limits never charge Gemini budgets. Follow
M4_SETUP before upgrading the existing private DB; do not replace it.
