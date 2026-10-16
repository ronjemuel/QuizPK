# Common multiplayer protocol v1 — M4.3 Classic extension

M4.2 lobbies and refresh behavior are user-approved on two real phones. M4.3 adds
`match.start` and `match.answer`, opt-in Classic snapshots, server deadlines,
immutable submissions, scoring/reveal, automatic progression and durable results.
Read the [implemented Classic contract](M4_3_CLASSIC.md) for schemas, secrecy,
clock/scoring/restart/retention rules and client behavior. The JSON command/scoring
fixtures now include production Classic semantics; Moderator transition vectors
remain design-only. LOCAL and Moderator gameplay are not implemented.

New clients advertise `X-QuizPK-Features: classic-v1`; this is capability negotiation,
never authentication/authority. The original lobby DTO remains available to old
clients. Match DTOs contain only the current permitted question/reveal and frozen
contestant standings. There is no event stream or private-key endpoint. The Java
codec validates the extension without becoming a scoring authority.

The original M4.1/M4.2 contract below is retained as historical design context;
the linked M4.3 contract supersedes its statements that all rounds are deferred.

---

# Original lobby contract

This contract separates command/state semantics from transport and authentication.
ONLINE lobby operations below are implemented. LOCAL hosting, real rounds, questions,
submissions, results and event streaming are designed/deferred. No endpoint starts
a match in M4.1 or M4.2. Shared fixtures are in protocol/multiplayer-v1.json; the
test-only scoring reference is not a production game engine. M4.2 tests the Java
lobby codec against the shared creation fixture and the real Node lobby authority;
it does not claim Java scoring/LAN conformance.

## Native M4.2 consumer

`RoomProtocol` contains strict commands, receipts and immutable snapshots, without
Android transport or cloud identity. `RoomBackendClient` supplies the individual
M3 credential only in the HTTPS Authorization header, verifies certificates using
platform defaults, refuses redirects and bounds command/response bodies at
4KB/32KB. No global cleartext or local-network exception was added. Its transport
interface leaves a seam for future local sessions; the production implementation
currently accepts HTTPS Online only. The canonical JSON fixture remains unchanged
and is included only in JVM test resources, not Android assets.

`LobbyViewModel` serializes requests, rejects stale revisions within the same
authority epoch, cancels foreground work on stop and drops callbacks from a prior
lifecycle/account epoch. Cold entry uses authenticated current membership rather
than a stored room code or client-claimed role. Changed credentials clear pending
commands and snapshots before loading the new identity. Revocation clears private
lobby display; network errors retain a visibly stale, disabled snapshot in memory.
Uncertain writes retain their exact envelope for explicit retry. They survive
configuration recreation in memory, while process death recovers authoritative
membership without persisting tokens or lobby commands. A receipt alone never
becomes UI state; a validated fresh snapshot must follow it.

Classic renders the host among the eight contestants. Moderator renders the
nonplaying owner separately and exposes no contestant-ready or match controls to
that role. Settings capture the revision when the editor opens and require fresh
review after a conflict. Invite sharing includes only code/mode/origin; credentials
are never shared. No future gameplay fields, endpoints or LOCAL authority are
accepted as implemented capabilities in this stage.

## Commands and authentication

Every command is JSON with exactly these fields:

    {
      "protocolVersion": 1,
      "requestId": "<fresh UUID v4>",
      "issuedAtMs": <authority time in integer milliseconds>,
      "type": "<command name>",
      "payload": { ... }
    }

room.settings additionally requires a positive integer expectedRevision. Unknown
fields (including actor, userId, owner, role, scores and timestamps used for grading)
are rejected. The 4 KB cap includes the entire body. Display names are NFKC-normalized,
trimmed, 1–40 UTF-16 units and contain no control/format characters; names confer
no authority. UUIDs are opaque, never an identity credential.

ONLINE: Authorization: Bearer <individual M3 token> over HTTPS. No token is sent
in JSON/query strings/invites, and no shared embedded app token exists. Every HTTP
request rechecks the enabled identity. Commands use the JSON requestId, independently
of M3's Idempotency-Key header/AI quota ledger. No CORS or browser account flow is
added. Local loopback HTTP is only an isolated backend-development/test boundary.

LOCAL (M4.8+): the same command codec receives an identity established by a local
ephemeral room session. The host's internal create operation proves local authority;
knowing its IP/code never proves moderation. LOCAL credentials expire at room end,
cannot be used online and never contain M3/provider credentials. Authentication
handoff, local TLS/cleartext confinement and Android service implementation must be
tested separately before approval. Never silently fall back from LOCAL to cloud.

| REST operation | Command/payload | Success |
| --- | --- | --- |
| POST /api/rooms | room.create: mode CLASSIC/MODERATOR, displayName, settings | Receipt with roomId, participantId, inviteCode |
| POST /api/rooms/join | room.join: code, displayName | Receipt with roomId, participantId |
| POST /api/rooms/:id/leave | room.leave: empty payload | Receipt; contestant leaves, owner closes room |
| POST /api/rooms/:id/heartbeat | room.heartbeat: empty payload | Receipt; explicit identity presence |
| POST /api/rooms/:id/ready | room.ready: ready boolean | Receipt; own contestant readiness only |
| POST /api/rooms/:id/settings | room.settings: settings plus envelope expectedRevision | Receipt; owner and fresh revision only |
| GET /api/rooms/:id | No command body | Authorized full snapshot |
| GET /api/rooms/current | No command body | Own current room snapshot, or room: null |

Settings have exactly category, difficulty, questionCount and timerSeconds. Category
is one of M2's twelve category names or All Categories; difficulty Easy/Normal/Hard/
Expert, count 5/10/15/20 and timer 10/15/20/30. These are lobby intentions: sufficient
server bank stock and selected question uniqueness must be enforced at match start
in M4.3, not assumed from the client's bank. Settings changes reset readiness.

A receipt is exactly protocolVersion, requestId and result. Only creation includes
the private invite code. Receipts are acknowledgements, not state snapshots: fetch
a fresh snapshot after each write. A retried receipt does not refresh presence.
Same identity/requestId/normalized content/path replays its receipt; conflicting
reuse returns IDEMPOTENCY_CONFLICT. Rejoin has a fresh command ID and retains the
existing participant UUID; no extra seat is allocated.

New commands must be within 5 minutes of authority time and at most 30s in the
future. Bootstrap clock offset through authenticated GET /api/rooms/current, which
also recovers a lost room ID. Never refresh issuedAt on the same pending command.
Receipts last 24h; after deletion, the unchanged old command is expired, not executed.
Lost/aged create responses can recover via current-room lookup and a fresh heartbeat.
Client timestamps are only replay-window inputs; they never decide gameplay timing.

## Snapshots, revisions and presence

A snapshot has protocolVersion, serverTimeMs, authority { network, instanceId },
room, self and capabilities. A no-room response omits self/capabilities.
ONLINE is implemented; LOCAL is a reserved network discriminator.

LOBBY room fields: id, mode, state, revision, expiresAtMs, inviteCode, settings,
maxContestants (8), minContestants (2), hostOnline and members. Each member has
participantId, role, displayName, ready and online. Self has participantId, role,
isOwner. No principal IDs, token hashes, questions/keys, submissions, score or
private queues appear. isOwner is server-derived display information, never a
permission input. capabilities currently says lobby true, matches false, localHost
false; a client must not invent enabled match controls.

Terminal CLOSED/EXPIRED snapshots are available only to previous members and omit
code/settings/roster data (members is empty). A user who explicitly left a still-live
lobby cannot read it. There is no room-list/search/spectator endpoint. Invalid and
expired invitation codes return the same ROOM_NOT_FOUND response.

Every persisted room/presence/settings change increases revision. Full snapshots
replace client state; they are not partial events. Poll no faster than 5s normally,
send a distinct heartbeat every 15s, and use backoff after failures/429. Reads alone
do not keep a participant present. At 45s without a heartbeat, presence is expired
when next observed; the persisted revision then increases. Offline seats remain
reserved until explicit leave/room TTL. A settings revision conflict requires a
fresh snapshot and user reconciliation, never silent overwrite.

instanceId is an API boot epoch. For the same configured HTTPS origin and room,
an authenticated reconnect snapshot may replace the old epoch, then stale packets
from that epoch are discarded. Do not merge state across Online/Local authorities.
Restart keeps membership, ownership, room revision, expiry and receipts, while
marking presence/ready false and changing epoch. The same online identity explicitly
heartbeats/rejoins; no caller can claim ownership with a name or room code. A future
LAN host restart may invalidate local sessions and require explicit new admission;
it must not restore old live clocks or silently migrate host authority.

Future ordered events use protocolVersion, authority epoch, roomId, revision,
type and an allowlisted role-specific payload. Missing/out-of-order revisions force
an authorized full snapshot, not replay of potentially private shared events. No
event journal/SSE/WS endpoint exists yet; revisions provide M4.1 reconciliation.

## Future identical game rules (design vectors only)

LOBBY -> READY -> ROUND_PREPARED -> ROUND_LIVE -> ROUND_CLOSED ->
ROUND_REVEALED -> ROUND_PREPARED or MATCH_FINISHED -> EXPIRED.
Classic match.start from the authenticated creator prepares the first round;
only server authority subsequently starts/closes/reveals/advances it. Moderator
match.start only enters READY. The moderator separately prepares and explicitly
starts Question 1 and every subsequent question. Contestants never control phases.
All starts require at least two contestants, never more than eight. Membership/
queue/settings freeze at launch; future state changes require expectedRevision
and atomic receipts. No current lobby route can reach READY or a round.

During LIVE publish only public question text, four options, opaque question/round
IDs and authoritative deadline. Keep private key/explanation/selected choices/
correctness/points separate until an authorized reveal. Moderator-private reads
must be explicitly authorized; Classic owner never receives unrevealed keys.
Each accepted contestant answer gets one immutable sequence and authority-received
elapsedMs; duplicates replay the same result, never award twice. A deadline arrival
at elapsedMs >= durationMs is late. Grade, order, timings and points never come
from a contestant claim. Use monotonic elapsed time for actual authority timing,
with epoch deadlines/clock offsets only for display.

Planned multiplayer score (does not change M2/M3 solo scoring): timely correct =
100 + floor(50 * (durationMs - elapsedMs) / durationMs), otherwise 0. Range 100–150
for timely correct; no negative elapsed input is accepted. Cancel/skip awards zero
to all, records the cancellation before reveal and never retroactively regrades.
First submission is minimum accepted sequence of any correctness; fastest correct
is minimum accepted sequence among correct answers. Same elapsed time uses sequence.
No correct answer means no fastest-correct badge. Final order: score descending,
correct count descending, fastest-correct wins descending, sum of correct elapsedMs
ascending, then participant ID ascending for stable display; equality before the
last display-only key is a tied sporting result. Different networks affect receipt
times: no millisecond fairness guarantee.

Moderator absence: keep an already-started deadline, then close and wait; block
moderator actions until authenticated recovery. No reveal or next automatic round.
Server/host restart with an in-flight round must close it as interrupted with no
new points, preserving immutable accepted records for audit; no stale timer resume.
Classic restart similarly needs a documented recovery gate before progression.
Implement/test these persistence rules with rounds in M4.3/M4.5, not lobby code.

Seven score, four order and sixteen transition fixtures define representative
equivalence inputs. The Node test-only reference currently checks them. M4.8 must
run the same JSON against the Java authority and add duplicate/late/restart/socket
vectors. No Online-vs-Java equivalence or real LAN result is claimed now.

## Errors and operational bounds

Error JSON: protocolVersion: 1, error: { code: <allowlisted server code> }. Never
render upstream bodies, request contents, SQLite errors or stack traces.
401 AUTH_REQUIRED; 403 FORBIDDEN; 404 ROOM_NOT_FOUND/NOT_FOUND; 409 ROOM_FULL,
ALREADY_IN_ROOM, HOST_UNAVAILABLE, HEARTBEAT_REQUIRED, REVISION_CONFLICT,
IDEMPOTENCY_CONFLICT or COMMAND_EXPIRED; 410 ROOM_UNAVAILABLE; 429 ROOM_RATE_LIMITED/
ROOM_LIMIT; 400 INVALID_COMMAND/INVALID_MODE/INVALID_SETTINGS/INVALID_REVISION/
PROTOCOL_UNSUPPORTED; 413 oversized; 415 wrong content type; 503 CODE_UNAVAILABLE/
SERVER_UNAVAILABLE. Production also rejects missing/invalid HTTPS with HTTPS_REQUIRED.
Retry-After is conservative advice, not proof a daily quota has reset.

Two-hour hard room TTL; 24h terminal membership/receipt retention; cleanup on startup
and room traffic. Max 20 active rooms, one active room per identity; creates capped
5/user/day and 100 globally/day. Code attempts 5/user/min and 20/hour, even malformed
attempts. Requests 120/user/min, 300/room/min and 3000/socket/min. Writes also cap at
20/user/min, 600/user/hour and 10000 globally/hour, bounding retained receipt growth.
Per-room counters apply to member reads/commands (including cached commands);
admissions additionally use guess/user/write limits. Unauthenticated traffic can
consume only a bounded ingress window; no token is an invitation code. No chat.
These quotas are independent of all existing Gemini budgets.
