# Native architecture and future integration boundaries

The existing `com.zeke.quizpk` application remains native Java/XML. MainActivity
owns Home, Explore, AI, Rooms, Profile and the Settings back-stack. The P0
`updateChrome()` drawable fix is retained. ThemePreferences uses M1's existing
SharedPreferences key and applies the saved explicit light/dark theme to both
activities. No reference PNG or test JNI library is packaged. M3 adds INTERNET
permission for explicit HTTPS generation; M2 and saved AI replay remain local.

## M2 offline flow

Explore queries actual counts from QuizDatabase and opens the unexported
OfflineQuizActivity with the selected category. That activity owns configuration,
gameplay, results and review Fragments; timed gameplay has its own toolbar and no
bottom tabs. MainActivity handles the results' Back to Home intent. Home and Profile
expose the saved draft, and Profile queries completed history on resume.

`QuestionRepository` exposes count/selection independently of UI and is the future
moderator library boundary. Native SQLiteOpenHelper fits the installed Java/Android
build environment without a new Room/compiler dependency. Schema v2 contains:

- `questions`: stable ID, category, difficulty, unique prompt, four options, checked
  answer index, optional explanation and source URL; indexed by category/difficulty.
- `sessions`: completion timestamp, category/difficulty, points, accuracy, length,
  foreground duration and a versioned JSON snapshot with per-question answer details.
- `active_session`: one replaceable draft, with its own session ID and JSON snapshot.

Android initializes the bank transactionally. v1→v2 adds drafts without dropping
questions or history; v1 is an internal migration fixture, never a shipped M1 DB.
Finishing inserts a session once by UUID and deletes only its matching draft in one
transaction. Opening an older result cannot erase a newer unfinished quiz. Saved
question/answer snapshots make old review independent of later bank edits.

Selection filters category/difficulty before shuffling and taking a count; IDs are
unique and QuizSession rejects duplicate IDs. Randomization off uses stable ID
order. Unavailable lengths cannot start; configuration offers the largest supported
count or the same difficulty across all categories. The seed has 12 × 4 × 5 rows.
Difficulty is editorial, not measured or independently calibrated. The schema can
accommodate more questions without UI count changes.

`QuizSession` is the centralized answer gate. Selecting does not submit. A pending
question can become CORRECT, INCORRECT, SKIPPED or TIMEOUT once; Next requires a
resolved question. `QuizScoring` awards 100 per correct answer and zero otherwise.
Accuracy is rounded correct/total × 100. Skipped and timed-out questions are
unanswered, including a selected answer that was never submitted. Incorrect means
an explicitly submitted wrong answer. There is no speed bonus or duplicated score.

## Interrupted-quiz and timer policy

A new question uses the chosen 10/15/20/30-second limit. SystemClock.elapsedRealtime
measures foreground elapsed time, avoiding wall-clock changes. Submission/skip/timeout
stops answer timing; visible feedback reading still counts toward total quiz duration.
Backgrounding, leaving and Activity recreation pause timing and save progress. The
player explicitly resumes after a lifecycle pause; Home/Profile Resume is also an
explicit action. The chosen order, selections, outcomes and remaining time survive
recreation and reopening. Completed results persist before results navigation.

There is one unfinished quiz. Starting another offers Resume, Discard and Start,
or Cancel. Save and Exit keeps the draft; Discard deletes only the draft. Drafts
never contribute to completed-history statistics. State is checkpointed on user
selection, resolution, advancement and lifecycle pause/save. An abrupt OS kill that
skips lifecycle callbacks may restore the last checkpoint (including some elapsed
time since that checkpoint); no claim of force-stop timing precision is made.

Storage failures stop gameplay and show a closeable error rather than silently
resetting user data or reporting a successful completed save. This is a small local
bank: current SQLite calls and history decoding are synchronous. Very large history
and device first-import performance are not load-tested; pagination/background
queries remain a future optimization.

## UI and answer visibility

M1's violet semantic colors, native typography, card styles and original duotone
vectors are reused in both themes. Scrollable layouts and wrapping answer buttons
support narrow screens; new touch targets are at least 48dp. Selection, correct and
incorrect states have text markers as well as colors. The local answer key exists
in SQLite for offline scoring, but gameplay binds no correct marker or explanation
to visible/accessibility text before submit, skip or timeout. Results review deliberately
shows correct answers and sources. This local model is not a multiplayer wire format.

## Backend and later multiplayer boundaries

M3 implements AI generation with a configured HTTPS endpoint, individual access,
provider safety controls, strict schema/consistency validation, idempotency, quotas
and server-held credentials. Only validated responses are imported for offline
use. See the M3 section below; real host/provider/device acceptance remains pending.

Multiplayer needs authenticated private membership, secure collision-checked
six-character codes, expiry, server capacity limits, and rate-limited actions.
Classic has up to eight contestants including a playing host; moderator mode has
one nonplaying moderator in addition to eight contestants. Starting a moderator
match produces a READY round and does not open answers.

Contestant public payloads contain only the permitted phase, question ID/text,
choices and authoritative server times. Keys/explanations revealing the answer,
player-specific submissions and unrevealed scores must not be readable by players
before reveal. Trusted server code authorizes transitions, resolves races and
records atomic/idempotent submissions, server receipt order and elapsed time.
The earliest submission can be wrong; fastest correct is a separate server
calculation, with receipt sequence as a deterministic tie-break. Network latency
limits fairness. Host abandonment must pause safely or use explicit authenticated
transfer; the client must not assume moderation.

M3's generation contract and access tests are documented in `backend/README.md`.
Multiplayer schemas, membership rules and adversarial room tests belong to M4–M6.
The legacy `config/backend.properties.example` does not configure the M3 service;
Android uses AI Quiz's connection dialog and the server uses private environment
configuration.

Future ads/premium should attach through a separate domain/service boundary;
The app contains no ad SDK, shop, coins or payment behavior. Timed rounds must not be
interrupted by ads.

## M3 — AI generation and reuse

A separate Node.js 24 backend owns Gemini access, individual user authentication,
fixed quotas and cached idempotent responses. It runs one process with durable
SQLite behind a local HTTPS reverse proxy; ephemeral/multiple instances require a
shared store before deployment. See `../backend/README.md` for exact config, model
verification and deployment. No production endpoint is invented or embedded in Android.

The Android client has a strict HTTPS transport, bounded bodies, sanitized error
mapping, and a retained Activity-scoped job. Each request is checkpointed privately
before sending; network loss/cancellation/process interruption can retry the same
ID. Generation is never automatic after reopening. Personal backend login is encrypted
with Android Keystore and excluded from backup/transfer. It is not a Gemini credential.
The Gemini credential stays outside the Android project sources and Gradle inputs.

SQLite v3 adds `questions.origin` defaulting to `OFFLINE_BANK`, `ai_collections`,
`ai_questions` (owning collection + position + immutable question snapshot), and
local `question_reports`. Upgrade 1→3 retains the v2 draft migration; upgrade 2→3
only adds schema. Neither path drops or reseeds M2 bank/history. The separate AI
question table avoids M2's global prompt-uniqueness constraint across saved sets.
Collections have `AI_GENERATED` origin, model, topic, settings/date, and a unique
endpoint + request ID import key. Imports are transactional and all question IDs
are local UUIDs. No provider IDs or credentials are saved.

`QuizConfig.collectionId` is optional in JSON. Old M2 snapshots deserialize with
null and keep their original validation. AI configurations carry the collection
ID and title; allowed count/difficulty/timer remain exactly the M2 options. Saved
collections construct the same `QuizSession`, `GameplayFragment`, `QuizScoring`,
results and review; no second engine exists. Review's incorrect-question reports
are local only and clearly labelled as such. Reports do not certify/correct answers.

Delete cascades only to the saved collection's questions. Completed and active
sessions retain independent snapshots, so history/review/resume stay valid. AI
Play Again from a historical result can replay its snapshot even after deletion.
There is no network code in saved-play or result-replay paths. Collection rename
changes future play titles; existing session titles remain historical snapshots.

AI question correctness and full topical relevance cannot be proven by schema checks.
The backend requests stable educational facts, checks normalization/duplicates,
valid answer range, exact counts and substantive explanations; Android validates
again before import. No repair loop spends further quota. Review and reports are
available; authoritative grading needs independent fact verification. Multiplayer,
eight-contestant private rooms and a separate nonplaying moderator remain future
work with the original server-authority requirements intact.
