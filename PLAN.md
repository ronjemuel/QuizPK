# M4 staged implementation — current plan (2026-10-13)

`QuizPK_M4_ONLINE_AND_LOCAL_PROMPT.md` supersedes the older M4 brief and the earlier
multiplayer milestone numbering below. M1–M3 are approved according to the user's
replacement brief. Preserve their Android code, private credentials and saved data.

1. **M4.1 — implemented and user-approved:** extend the one-process Node/SQLite
   backend with authenticated lobby authority, additive tables, roles, private
   codes, capacity, TTL, presence and reconnect snapshots. Design a common protocol,
   deterministic future scoring/state vectors and embedded Java host architecture.
   Verify recovery, migration/provisioning, backend and all Android regressions.
2. **M4.2 — implemented, STOP for review:** native Online room/lobby UI,
   Classic/Moderator selection, capacity/readiness/errors/reconnect; signed update
   APK and two-device lobby test plan. Physical acceptance remains pending. The live
   Termux host/DB stays unchanged; migration/deployment is separately planned.
3. **M4.3:** authoritative Online Classic rounds, server-owned bank, deadlines,
   atomic answers, scoring/reveal and completed results.
4. **M4.4:** moderator-private question library, custom authoring, validated queue.
5. **M4.5:** manual prepare/start/close/reveal/next for every moderator round.
6. **M4.6:** first submission versus fastest correct, ranks and durable history.
7. **M4.7:** real Online acceptance on at least two phones; moderator + two
   contestants for the full moderator flow. This does not certify Local.
8. **M4.8:** embedded Java Android LAN host, ephemeral local sessions, service
   lifecycle, private history and the same protocol fixtures; no cloud/Termux runtime.
9. **M4.9:** Local discovery plus mandatory manual LAN IP:port fallback, no-internet
   Classic/Moderator UI and complete offline games.
10. **M4.10:** Node/Java equivalence and physical disconnected-router/data-off
    hotspot tests, preserved M1–M3, both themes and same-signature APK release.

M4.1 uses bounded HTTPS commands and 5-second snapshot polling, compatible with
the existing proxy and temporary Quick Tunnel. No new runtime dependency, Android
change in M4.1. M4.2 adds only the Online Android lobby client; there is no live
match or LAN runtime in either stage. See docs/M4_ARCHITECTURE.md and
docs/M4_PROTOCOL.md for permissions, timing, score, expiry and future LAN decisions.

The following M0–M3 record is historical. Its pending M3 acceptance statements
were superseded by user approval in the current brief; its old M4–M7 split is
superseded by the ten stages above.

---

# M3 staged implementation (2026-10-12)

The user supplied the complete M3 brief and reported M1/M2 physical acceptance.
Read the full brief, starter prompt, logs, test notes, M2 inventory and Asset 07.
Created a verified source checkpoint excluding credentials before changes.

1. Secure backend: Node.js 24, durable SQLite and individual provisioned user
   tokens. Gemini key is loaded only server-side from the exact custom filename.
   Fixed quotas, concurrency, structured outputs and sanitized errors are implemented.
2. Data: additive SQLite v3 migration, atomic idempotent imports, local UUIDs,
   AI collections and reports; preserve M2 bank/history/drafts.
3. Native UI: AI input and lifecycle states; saved list/rename/delete; reuse the M2
   gameplay/scoring/resume/results/review engine and replay from local snapshots.
4. Validation: mock backend/provider tests, real SQLite JVM migration/data/UI tests,
   clean assembly/lint/tests, signing/security audits; document actual results.
5. External acceptance: deploy to one persistent host behind HTTPS, privately set
   a rotated Gemini key and current model on a verified free-tier project, provision
   user tokens, and complete the physical-device checklist. **Pending/unverified**.

No multiplayer, ads, payments or M1 startup/theme rewrite. Existing later plans
for private rooms with up to eight contestants and a separate nonplaying moderator
remain unchanged below.

---

# QuizPK implementation plan

> M1/M2 are approved according to the user's M3 brief. M3 source is implemented;
> real HTTPS backend/provider and physical-phone acceptance remain pending.
> Available automated evidence is in TEST_NOTES. M4–M7 have not been started.

## M0 — inspection (2026-10-12)

The root project is an existing Java/XML Android app, package/application ID
`com.zeke.quizpk`. Its only screen is the starter greeting; no existing quiz,
backend, or persistence was found. The workspace has no Git repository. Preserve
the Gradle wrapper, signing configuration, application identity, and starter pack.
Do not read signing credentials or replace the existing keystore.

Toolchain observed: OpenJDK 21.0.12 (Termux), Gradle 9.6.1, AGP 9.3.1,
compile/target SDK 36, minimum SDK 24, Java source/target 17. The baseline command
`bash gradlew :app:assembleDebug --offline --console=plain` failed with
`SDK location not found`. No ADB device was attached. Partial SDK files under
`/tmp` are not evidence of a working Android build environment.

All 17 primary PNGs and both optional PNGs were opened and visually inspected.
See `docs/REFERENCE_AUDIT.md`. They are composite design boards, never runtime
screenshots/icons. Native vectors will interpret the crowned question shield,
duotone geometric icons, violet accent, 8dp grid, and 12–20dp cards. The brief's
light/dark palette and security requirements override illustrative content.

## Dependency choices

- Keep Java + XML, Android SDK, existing AppCompat 1.6.1 and Material 1.9.0.
- Use AndroidX Fragment already supplied by AppCompat for native screen navigation;
  existing view binding stays enabled. Add no Compose/React Native/runtime services.
- SharedPreferences stores the explicit dark/light choice; dark is the default.
- M2: SQLiteOpenHelper, avoiding a new Room/compiler dependency on the phone.
  Separate repositories/models from views; local quiz keys never enter multiplayer
  payloads. Seed questions must have recorded sources/attribution before shipping.
- M3: server-side AI proxy, strict schema, moderation, quotas/cost caps. No Android
  API keys; no generated sample presented as real AI output.
- M4 onward: choose a trusted backend with anonymous authentication and membership
  authorization only after configuration is available. Firebase is a candidate,
  not an installed or connected dependency. Never simulate a connected room.

## Milestones and gates

1. **M1:** native shell, themed resources, reusable vectors/components, Home,
   Settings and five destinations. Persist dark/light choice, restore navigation,
   handle Android back/insets, and show honest upcoming/setup-required states.
   Attempt debug assembly and lint; report toolchain failures exactly.
2. **M2:** attributed seed bank and database, configuration, no repeats, solo
   question/timer/feedback/results/mistake review, resumable progress/history,
   centralized scoring; test domain/database behavior and offline device flow.
   Current scope: score/history, with no invented XP, streak or achievement features.
3. **M3:** AI topic/difficulty/count UI, saved valid quizzes and versioned secure
   contract. Enable generation only against a configured, tested backend.
4. **M4:** private classic rooms, auth/rules, server-only code generation, expiry,
   atomic submissions and scoring. Test two devices before declaring multiplayer.
5. **M5:** separate moderator library/custom builder, validation, deduplicated
   ordered queue, waiting lobby and eight contestant seats.
6. **M6:** authorized READY → LIVE → CLOSED → REVEALED transitions, server timing,
   first submission versus fastest correct, round/final results and history.
7. **M7:** both themes, accessibility, small screens, larger fonts, reconnect/host
   abandonment/error cases, physical-device acceptance and packaging.

The initial M0/M1 delivery followed README_FIRST.md. The user subsequently
approved M1/M2 and authorized the supplied M3 brief. M3 retains local solo data
and adds secure generation/saved replay. Preserve its real-backend/phone gate
and all later milestone gates.

## Multiplayer invariants (all later milestones)

- Private code/invitation rooms only; no public/random matchmaking.
- Classic: at most eight contestants, including a host who chooses to play;
  server selects questions and advances automatically.
- Moderator: one nonplaying moderator **in addition to** up to eight contestants.
  Bank and custom questions share one ordered deduplicated queue.
- Starting a moderator match leaves round 1 READY; only explicit moderator start
  makes it LIVE. Closed answers stay unrevealed until explicit reveal.
- Only trusted server code authorizes actions, deadlines, submission receipt order,
  scoring, idempotency, capacity and role transfer. Client clocks/points are untrusted.
- Contestant subscriptions/API omit keys, other answers and scoring before reveal.
  Keep server-only answer keys separate from client-readable room documents.
- Host disconnection safely pauses; no silent role takeover. Skip/cancel needs
  confirmation and documented scoring; stale/late/duplicate submits are rejected.
- Fastest correct uses earliest correct server receipt, distinct from first submit.
  Receipt sequence breaks ties deterministically; network latency limits fairness.

## M1 completion evidence

See IMPLEMENTATION_LOG.md and docs/TEST_NOTES.md for actual commands, file list,
build results, limitations and the next step. A source implementation or successful
compiler check is never a substitute for installed-device interaction testing.
