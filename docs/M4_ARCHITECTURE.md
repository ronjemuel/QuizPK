# M4 architecture — Online Classic Match

M4.2 is approved after real two-device acceptance. M4.3 extends the same Node/SQLite
authority and native Java/XML consumer with Classic gameplay. See the
[Classic architecture and rules](M4_3_CLASSIC.md),
[safe deployment/fallback plan](M4_3_DEPLOYMENT_PLAN.md) and
[two-device acceptance plan](M4_3_DEVICE_TEST_PLAN.md).
The existing HTTPS transport uses one-second active-match snapshots and a server
timer; no new dependency, second service or client scoring engine is introduced.
Answer keys remain in separate server-private rows until the authorized reveal.
Android solo storage/authentication and working M1–M4.2 behavior are preserved.
Physical M4.3 acceptance and any live deployment require later review.

Earlier architecture records follow; their stage-specific exclusions are historical.

---

# Original Online foundation and M4.2 lobby consumer

M4.1 is user-approved. M4.2 implements only the native Online lobby consumer:
strict transport-neutral Java codec, bounded HTTPS adapter, activity-scoped serial
coordinator and Java/XML forms/rosters using existing dark/light tokens and encrypted
M3 login. Polling is foreground-only; pending write retries are explicit and
server membership recovers process death. Solo storage, SQLite v3, existing M3
features, backend production source and the canonical JSON protocol are unchanged.
See [protocol consumer behavior](M4_PROTOCOL.md), [deployment plan](M4_2_DEPLOYMENT_PLAN.md)
and [physical lobby checklist](M4_2_DEVICE_TEST_PLAN.md). No live host was modified.

The M4.1 architecture decisions below are retained as the historical foundation.
Its “only M4.1 authorized / unchanged Android” statements describe that stage;
the subsequent explicit user approval authorizes M4.2 only. M4.3+ remains deferred.

Authority: QuizPK_M4_ONLINE_AND_LOCAL_PROMPT.md supersedes the online-only brief.
Only M4.1 is authorized. M1–M3 are user-approved; previous pending-device entries
describe the earlier implementation session. Assets 08, 09, 12–17 were inspected;
no Android UI or resource change is needed for this backend stage.

## Decisions and compatibility

Extend the existing Node.js 24 process and durable SQLite database. Reuse its
individual provisioned bearer identities, loopback binding and HTTPS proxy check.
Add an isolated, versioned multiplayer migration; never drop/alter the M3 users,
jobs or throttle tables. Keep the Gemini loader/provider, generation route and
all generation budgets unchanged. Room operations never call Gemini.

Use bounded HTTPS REST commands and full short-poll snapshots, initially every
5 seconds with a separate heartbeat every 15 seconds. This uses Android's existing
HTTPS capabilities and no new packages. Cloudflare Quick Tunnels explicitly lack
SSE support, have no uptime guarantee and change hostname; short polling avoids
that constraint. WebSockets may be added later behind the same command/snapshot
codec, with authenticated commands and revision reconciliation. No socket server
or Java client is added in M4.1. Sources checked 2026-10-12:

- [Node 24.18 SQLite API](https://nodejs.org/download/release/v24.18.0/docs/api/sqlite.html)
- [Cloudflare Quick Tunnel constraints](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/)

Production remains one process on 127.0.0.1:8787 behind Caddy or a local cloudflared
edge. Forwarded HTTPS is trusted only on the loopback-bound service; public direct
access to 8787 is forbidden. No forwarded IP is an identity. Room ingress limits
use the socket peer, intentionally shared behind the proxy, separately from M3's
generation throttle. No TLS/cleartext Android policy or existing configuration is
changed. No live server is restarted and no deployed/private DB is migrated here.

Source changes: backend/src/store.mjs (additive migration hook),
backend/src/server.mjs (room dispatch), backend/src/multiplayer/{migrations,
protocol,rooms,http}.mjs, new room/protocol tests, shared protocol fixtures and
test-only reference rules. Docs/tools provide restore, setup and preservation
evidence. Existing Android source, SQLite v3, APK/version/signing and token format
remain unchanged. All data tests use temporary databases.

## Durable lobby model

Multiplayer schema version 1 has separate mp_schema, mp_rooms, mp_members,
mp_requests and mp_limits tables. Rooms have opaque UUIDs, protected private
six-character invite codes, immutable owner identity/mode, settings, LOBBY/CLOSED/
EXPIRED state, revision and a fixed two-hour TTL. Cryptographic alphabet excludes
ambiguous characters; uniqueness is checked transactionally with bounded retries.
Codes exist only in the private DB and authenticated member snapshots/creation
receipts, never health/errors/logs/public listing. A code is an invitation, not
authentication or moderator authority. There are no spectator/list-room endpoints.

Membership has a unique room/user pair, opaque participant UUID, server-assigned
role, readiness, joined/left timestamps and persisted presence. One identity may
occupy one active room. Classic creator is a contestant (eight total); Moderator
creator is the separate moderator (eight contestant seats plus the moderator).
Seats remain reserved while disconnected. Explicit contestant leave frees a seat;
owner leave closes the room without transferring ownership. Rejoin retains the
participant ID. A revoked token cannot read/rejoin or reuse a cached receipt.

BEGIN IMMEDIATE encloses cleanup, idempotency lookup, membership/seat checks,
mutation, revision and receipt. On error, roll back. Rate counters commit separately
so failed guesses remain counted. Every request authenticates; every snapshot checks
membership anew. Allowlisted projections never SELECT * into public responses.
Cached command acknowledgements contain no roster, answer keys or user/token hashes;
clients fetch a fresh authorized snapshot after an acknowledgement.

Presence becomes offline after 45 seconds or API restart. Snapshot reads do not
pretend to be heartbeats. Host absence locks new admissions/settings, while existing
members may authenticate and heartbeat to recover. No arbitrary host takeover.
Presence expiry updates durable revisions when observed; no stale live timer exists
in this stage. Future round restart behavior must fail closed before resuming play.

Initial caps: 20 live rooms; 5 creates/user/day, 100 global/day; one active room per
identity; 5 code attempts/user/minute and 20/hour; 120 room requests/user/minute,
300 per room/minute and 3000 per socket/minute. Writes also cap at 20/user/minute,
600/user/hour and 10000 globally/hour. Commands are 4 KB maximum. Retain
terminal rooms/members and receipts for 24 hours, then purge; counters expire with
their windows. New commands have a five-minute acceptance window and at most 30s
future skew, so expired stored receipts cannot make an old replay execute again.
No question, answer, score or match-history table is implemented prematurely.

## Permission matrix

| Operation | Contestant | Classic creator | Moderator | Outsider |
| --- | --- | --- | --- | --- |
| Create own room | Yes, becomes owner/assigned role | Yes | Yes | Auth required |
| Join by valid code | Contestant only | Same identity rejoins | Same identity rejoins | Auth required |
| Snapshot/current room, heartbeat | Own membership | Own membership | Own membership | Denied |
| Ready | Own contestant state | Own contestant state | Cannot be ready as contestant | Denied |
| Edit lobby settings | Denied | Own LOBBY, fresh revision | Own LOBBY, fresh revision | Denied |
| Leave | Own seat | Closes own room | Closes own room | Denied |
| Start/answer/queue/reveal/score | Not implemented | Not implemented | Not implemented | Denied |

## Shared Online/Local authority boundary

The transport-neutral version 1 command and snapshot contract is specified in
M4_PROTOCOL.md and protocol/multiplayer-v1.json. The transport resolves a principal
first; the authority handles command(principal, envelope), snapshot(principal, id)
and clock/presence. Roles, participant IDs, room membership, revisions and results
are assigned by that authority, never a client flag/display name. Rendering uses
the same safe snapshot shape; it must not depend on cloud user IDs or token hashes.

ONLINE adapter: M3 bearer token resolves a server user. LOCAL adapter (M4.8+):
embedded Java foreground-service host issues ephemeral per-room credentials and
host-confirms role assignment. Cloud bearer/provider credentials must never enter
that adapter, LAN invitations or LAN traffic. No cloud login is needed locally.
Each authority exposes a distinct random instance/session epoch for reconnect;
clients discard snapshots from another authority and reconcile revisions.

Future Java host: native lightweight HTTP command/snapshot server with a bounded
executor, compliant visible foreground-service notification, private SQLite and
monotonic elapsed-time clock. Evaluate/pin a small server library in M4.8 rather
than adding an unused dependency now. NSD/mDNS is optional discovery; manual LAN
IP:port + invite is mandatory. Never assume a hotspot gateway address. Binding,
permissions, Android 14+ service types, network locks and local-only cleartext/TLS
configuration need device validation in M4.8–M4.10; global HTTPS stays intact.
No Termux, cloud, DNS, Gemini or browser is part of local runtime gameplay.

The host's existing validated M2 bank and deliberately chosen saved AI questions
may feed LOCAL authority; no automatic upload. ONLINE must provision/review its own
server-owned bank in M4.3 (preserve attribution and validate all 240 IDs/categories/
difficulties before import), not accept a contestant's answer key. Known published
offline trivia cannot be kept secret from prior knowledge/external lookup. Local
device owners can inspect/alter their host: trusted classmates, no prize security
claim. Local result storage gets a later additive Android migration and is never
silently cloud-synced. Host death/network change fails visibly; no host migration.

## Staged stop gates

M4.1: authenticated online lobby authority, common protocol/spec/fixtures and tests.
M4.2: native Online lobby UI only after review. M4.3–M4.6: Classic, moderator
authoring/controls and ranking/history, each authorized separately. M4.7: real Online
phone acceptance. M4.8–M4.9: embedded Java LAN authority and offline both-mode UI.
M4.10: Node/Java fixture equivalence plus disconnected-router/hotspot physical
acceptance. Common future rules are designed/tested as reference vectors only;
no live match or local server exists in M4.1. STOP for review before M4.2.
