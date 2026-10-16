# M4.3 Online Classic Match

M4.2 was approved after real two-phone lobby and refresh-fix acceptance. M4.3 adds
Classic gameplay only. Moderator rooms retain their separate nonplaying owner and
working lobby; authoring, moderator gameplay, LAN runtime, monetization and a
dedicated match-history browser remain later stages. Physical M4.3 acceptance is
pending. No live service/database or installed Android app was changed here.

## Authority and transport

Extend the existing Node 24 process, individual-token HTTPS boundary and durable
SQLite. There are no new dependencies. Native Android uses the existing HTTPS
adapter and activity-scoped coordinator. Lobby polling stays at five seconds;
active Classic polling uses one second, with separate 15-second heartbeats.
Authoritative state also advances from a 250ms server timer without any client.
Snapshot/command reconciliation remains transport-neutral for future LAN work.
One persistent process is required; do not run multiple API replicas on this DB.

New clients send `X-QuizPK-Features: classic-v1`. This selects the Classic snapshot
extension; it grants no permission. Old clients receive their original lobby
shape/capabilities. An old client encountering an already started match gets a
sanitized `UPDATE_REQUIRED`. The updated APK can use an older M4.1/M4.2 backend:
its ordinary lobbies work, with Start absent until the new backend is deployed.
Moderator snapshots never advertise Classic support. No event, library, answer-key
or spectator endpoint is introduced.

## Commands and safe public state

The existing v1 envelope, 4KB command cap, individual-token authentication,
idempotent receipts and five-minute replay window remain. Every request rechecks
the enabled identity. New operations:

| Operation | Type and payload | Permission |
| --- | --- | --- |
| POST `/api/rooms/:id/match/start` | `match.start`, empty payload, `expectedRevision` required | Classic room owner, 2–8 online ready contestants, sufficient stock |
| POST `/api/rooms/:id/match/answer` | `match.answer`, exactly `roundId` and integer `option` 0–3 | Frozen contestant identity, current LIVE round, before deadline, no previous answer |
| GET `/api/rooms/:id` or `/current` | Authorized snapshot with opt-in header | Own room/match membership only |
| POST `/api/rooms/:id/leave` after finish | Existing `room.leave` | Dismiss own result; does not erase history |

During a match the roster/settings freeze; new players cannot join. Existing
identities reconnect to the same seat through authenticated snapshots/heartbeats.
Ready/settings/leave during gameplay cannot mutate the frozen match. Back can
return Home with the seat reserved; the Classic server continues even when the
host disconnects. Result dismissal/navigation waits for a confirmed server reply.

The room state projects `ROUND_PREPARED`, `ROUND_LIVE`, `ROUND_REVEALED` or
`MATCH_FINISHED` from the separate match table. The underlying room row keeps its
v1 schema. Snapshot `room.match` contains phase/count, start/finish metadata, one
current round and standings. Full question queues and answer keys never enter it.
The Java parser rejects unknown fields, private keys in public questions, reveal
data outside REVEALED, unsupported roles, duplicate/foreign participant IDs and
invalid choices/timing/score ranges. No client grades or calculates earned points.

PREPARED holds the question privately for three seconds. LIVE includes public
question text/four shuffled options, shared absolute start/deadline, submitted
participant IDs and the requesting player's own accepted option. It omits others'
choices, correctness, explanations and pending points. Standings contain only
previously revealed scores. Accepted-answer receipts contain only room/participant
IDs, independently of whether the choice is correct. Errors never include input,
SQL details, tokens, keys or upstream bodies.

At the deadline, close/grade/reveal happen in one transaction; CLOSED is an
internal transition, with no half-graded public snapshot. REVEALED adds correct
index/explanation, each contestant's accepted choice/time/order/outcome/points,
first submission and fastest correct. Five seconds later the next PREPARED round
begins, or the match finishes. All players receive the full timer; answering early
does not end a round. A client cannot start, reveal or advance individual rounds.

## Questions, clocks, scoring and ties

`backend/src/multiplayer/classic-bank.json` is a byte-identical, attributed copy of
the approved 240-question M2 bank. It is validated on startup. The server filters
by category/difficulty, checks stock, cryptographically shuffles unique questions
and option order, then stores immutable snapshots/private keys at launch. A single
category/difficulty currently has five questions; use All Categories for 10/15/20.
No Gemini request or contestant-supplied answer key is used. Published M2 trivia
may already be known to players; API secrecy does not prevent prior knowledge or
external lookup and is not a prize-tournament anti-cheat guarantee.

An epoch display clock is anchored to the server's monotonic clock for gameplay.
Server receipt time determines elapsed time; client `issuedAtMs` is only a replay
check. Ordinary server wall-clock adjustments do not re-time a live round.
Arrival at `elapsedMs >= durationMs` is late. No network grace adds extra answer
time. Android displays a smooth countdown from the latest authoritative clock,
stops offering answers when disconnected/stale for 2.5 seconds, and never shows
invented points. Network latency and polling affect what a phone sees; no
millisecond-perfect fairness is claimed.

Timely correct: `100 + floor(50 * (durationMs - elapsedMs) / durationMs)`.
Wrong/unanswered/cancelled: zero. First submission is the lowest accepted sequence
of any correctness; fastest correct is the lowest sequence among correct answers.
Equal elapsed milliseconds use acceptance sequence. There is no incorrect-answer
bonus; zero correct answers means no fastest-correct winner.

Final order: points descending, correct count descending, fastest-correct wins
descending, sum of correct elapsed milliseconds ascending. Participant UUID sorts
otherwise equal rows for display only. Equal sporting totals share a competition
rank and winner status. Accuracy uses the requested question count; interrupted
matches clearly report interruption and preserve only revealed rounds' totals.
Final results show rank, score, correct count, accuracy, fastest wins and duration.

## Durability, restart and bounds

Schema ledger `mp_schema` advances from 1 to 2 in one additive transaction, adding
`mp_matches`, `mp_match_players`, `mp_rounds`, `mp_answer_keys`, `mp_answers` and
indexes. M3 `users/jobs/throttle`, prior room tables and `PRAGMA user_version`
remain unchanged. Private keys are stored separately from public question JSON.
Answers have unique `(round_id, participant_id)` and `(round_id, sequence)`.
Grades/totals/phase/revision commit together. A failed scoring transaction rolls
all of them back; retry cannot award twice. Same-envelope replay returns the same
receipt, while a fresh conflicting answer cannot replace the accepted choice.

API startup finishes every in-flight Classic match as `INTERRUPTED`, cancels its
unrevealed/queued rounds and preserves accepted records without grading those
rounds. Previously revealed grades/scores remain. The new boot epoch invalidates
old callbacks. Clients recover authenticated final results and must explicitly
create/join a new room to play again. A process resuming after a prepared question's
entire deadline also interrupts rather than releasing a stale question. Room TTL
expiry finishes a match safely with `ROOM_EXPIRED`.

Completed/interrupted results and answer records remain in server SQLite for 30
days, accessible by the existing authenticated membership snapshot; an undismissed
latest result recovers through `/current` after app restart. Done dismisses only
that player's current-result pointer. Creating a new room dismisses older result
pointers. A dedicated list/history UI and optional local caching are deferred.
Android solo SQLite remains v3; no saved quiz/history migration is added.

Existing user/write/guess/global ingress caps remain. Match room reads/commands
have a 1200/min cap so eight one-second pollers fit; ordinary lobby cap remains
300/min and per-user requests remain 120/min. Commands remain 4KB, responses 32KB,
rooms hard-expire at two hours, receipts at 24h. Retention plus existing creation
caps bounds game data; there is no chat or unbounded event journal. Termux and
temporary tunnels are development hosting, with Android background/uptime risks.

`QUIZPK_CLASSIC_ENABLED=false` disables new starts and advertises ordinary lobby
capabilities. On restart, in-flight games still finish safely and authorized new
clients can recover final results. This is the tested fallback on the current DB.
Unmodified M4.2 code rejects schema 2; never downgrade source blindly, drop tables,
edit its ledger or rewind the DB. See the [deployment/rollback plan](M4_3_DEPLOYMENT_PLAN.md).
