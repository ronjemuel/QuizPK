# M4.3 fresh-session review — blocked (2026-10-13)

New checks: supplied public HTTPS `/health` returned **530**, with TLS verification
enabled, no redirect and no credentials sent. Plain loopback 8787 refused the
connection. Native production state is unverified: the executor exposes 12 numeric
process directories, no visible Node/cloudflared and no readable TCP table. This
does not prove the native service is globally stopped. No live SQL/authenticated
room request, restart, rotation, migration, deployment or APK change occurred.

Retained private backup/release/receipt hashes verify; the historical receipt has
87 passing checks. Current deployed source/config files match retained inputs,
source matches the approved M4.2 checkpoint, workspace backend matches frozen
M4.3, and checkpoint/APK hashes match. Current DB schema/counts/durable equality
were not checked. No fresh backup/context/rehearsal or test suite was run after
the endpoint/runtime failure. Historical 106 Android / 74 backend / 87 rehearsal /
33 operator passes below remain historical evidence.

The old rotation helper refuses multiplayer schemas and cannot be reused for the
latest phone01 exposure. New rotation remains unperformed and needs a reviewed,
synthetic-tested schema-aware helper. Review the
[revised blocked runbook](M4_3_DEPLOYMENT_RUNBOOK_REVISED.md) and
[sanitized observations](validation/m4.3-fresh-session-review/summary.json).
M4.3.1 automatic connection/guest onboarding is design only. STOP for review.
Documentation verification parsed all 10 Bash blocks without executing them,
resolved all relative runbook links and validated the evidence JSON. The retained
operations-manifest source anchor/files match, and retained Android XML totals
confirm 106 tests with zero failures/errors/skips; this is not a new test run.

---

# M4.3 deployment preparation (2026-10-13)

Fresh online schema-1 backup and standalone restore passed while the same M4.2
process kept running. The copied database contains two identities, four saved AI
jobs, six rooms and seven membership rows. Deployed source matches approved M4.2.
Both current tokens and all four HTTP cached responses pass on isolated copies;
phone01 retains the rotated identity and the exposed credential is rejected.

The exact frozen M4.3 source passes **74/74 backend tests**. Its real-data rehearsal
passes **87/87 checks**, including additive migration, full Classic progression,
restart interruption, separate fallback copy, a newer saved AI job and the actual
Termux production entrypoint reading the disabled-Classic environment flag.
Operator regressions pass **33/33** (16 new M4.3 + 17 existing); all 16 new checks
also pass from the frozen operator copy. No Android change requires another build.
The approved version-6 APK remains byte-identical.

The final live main/WAL, source/config and PID/start identity match preflight.
No live Store migration, stop, source replacement, token write, provider call or
APK installation occurred. Logs and raw database/configuration remain private.
Sanitized evidence: `validation/m4.3-deployment-prep/`.

Public HTTPS remains unresolved: the old recorded origin fails DNS, while control
DNS works. A cloudflared process remains running and plain loopback correctly
requires HTTPS proxy proof. Only credential-free health checks were sent to live;
no live room/API authentication bookkeeping was exercised. Confirm the URL saved
on the working phones and prepare a freshly bound context before deployment.
See [the exact runbook](M4_3_DEPLOYMENT_RUNBOOK.md). Stopped for review.

---

# M4.3 Online Classic validation (2026-10-13)

M4.2 physical two-device lobbies and refresh fix were approved by the user before
this work. M4.3 physical acceptance is pending; use [the device plan](M4_3_DEVICE_TEST_PLAN.md).
The final automated result is recorded in `validation/m4.3/final-summary.json`.

Backend: **74/74**, including 20 new Classic tests. Coverage includes host/roster
permissions, secret-free pre-reveal snapshots and receipts, real HTTPS/auth gates,
duplicate and conflicting submissions, exact deadline scoring, acceptance-order
fastest correct, ties, atomic grading failure/rollback, automatic progression,
durable restart/cancellation, authenticated result recovery, bounded retention,
8-player polling capacity, populated v1 migration and compatible fallback.
The final deadline regression uses one sampled acceptance timestamp for validation
and storage, avoiding a clock-crossing inconsistency.

Android final suite contains **106 tests**: the original 96 plus nine Classic
protocol/UI/lifecycle tests and one Java-to-real-Node/SQLite five-round game test.
The latter exercises both clients, duplicate taps, scoring, restart and dismissal.
Final result: **106/106 passed**, with zero failures, errors or skipped tests.
Assembly/lint passed: **0 errors / 52 warnings**, versus 48 before M4.3. Four new
warnings: two plural-string suggestions, one overdraw and one combined-layout
view-count warning. No lint suppression, dependency or toolchain change was added.
Narrow-screen/130% font and dark/light checks are automated layout checks;
physical rendering, network latency and two-phone gameplay remain acceptance gates.

Maintenance regressions: **17/17**, using isolated synthetic databases/configs.
Two existing backup tests initially contacted the real port probe and correctly
refused an occupied listener. Their synthetic socket is now mocked; production
guards are unchanged. No live service was stopped or altered. The earlier import
failure and Android failures are retained as diagnostic evidence. The first
Android run had the obsolete unsupported-Classic expectation; the expanded run
passed all gameplay assertions but its fixture stdin kept the child process alive.
The fixture now closes/pauses input and exits normally; the final run verifies it.

APK: `artifacts/QuizPK-M4.3-classic-debug.apk`, **7,435,777 bytes**, version code
**6 / 1.4-M4.3**, package `com.zeke.quizpk`, min SDK 24, target 36. SHA-256:
`f3cfd99bddbb1f297767d7e63308b59f2aa818cbd316306ace782053f202e748`.
Valid v2 signing matches the actual installed version 5 and retained M3 signer.
Permissions and existing assets are unchanged; no private values/hashes, test
classes or synthetic fixture credentials were found in public changes or APK.
The 240-question structural audit and byte-identical server-bank check passed.

Preservation audit checks all **36** private/live baseline files by bytes without
opening live SQLite. Prior M3/M4 checkpoints and APKs remain unchanged. Original
public files are retained; [source inventory](M4_3_CHANGED_FILES.md) records scope.
Android saved-data schema stays v3. Backend schema 2 is additive and tested only
on isolated fixtures. **No live deployment/migration, provider call, app install,
credential change or private-data staging rehearsal was performed.** Review the
[deployment and fallback gates](M4_3_DEPLOYMENT_PLAN.md) separately. Old M4.2 source
cannot run schema 2; fallback uses the same M4.3 release with Classic disabled.

---

# M4.2 refresh correction — physical acceptance pending (2026-10-13)

Confirmed causes: each 5-second background GET used the command busy flag to
disable the focused editor, and GONE/VISIBLE progress changed layout height.
Rendering also selected Create/Private Lobby from unconfirmed cached snapshots.
No direct server-name assignment to the editable display-name field exists.
Recreation tests additionally exposed first-layout cursor initialization and
focus capture after detachment. See [cause and correction](M4_2_REFRESH_FIX.md).

Polling now leaves editors enabled and progress geometry stable. Rendering uses
conditional property/roster updates; confirmed accepted snapshots alone reconcile
automatic navigation. Account/lifecycle/request guards, room revision checks and
retired authority epochs reject stale replies. Raw form text, selection ranges,
focused field, mode/settings and scroll survive view/lifecycle restoration; the
final one-time layout restore cannot replace newly typed text. Authentication,
protocol, backend, schemas and M1–M3 production sources remain unchanged.

Final verification: **Android 96/96**, including all original 81 tests and 15 new
refresh regressions; **backend 54/54**; question-bank structural audit passed.
Assembly and lint passed, **0 errors / 48 warnings**, with no new suppression,
baseline, dependency or toolchain change. New checks model 30 seconds of continuous
composition across six delayed polls, field/IME-inset changes, reconnect,
rotation, Settings/Back, fresh saved-state restoration, changed-account delayed
callbacks, old revisions/retired authorities and confirmed room transitions.

The original code fails both focused reproductions. Intermediate failures are
retained, including the full 94/96 run that exposed first-layout cursor resets;
no assertion was removed or weakened. One earlier targeted attempt failed at
test-worker startup; the interrupted run is also retained. Final XML, lint,
build log, signature, inventory and preservation records are under
`validation/m4.2-refresh-fix/`.

Signed debug artifact: [QuizPK-M4.2-refresh-fix-debug.apk](../artifacts/QuizPK-M4.2-refresh-fix-debug.apk),
**6,367,273 bytes**, package `com.zeke.quizpk`, version **5 / 1.3.1-M4.2**.
Its valid v2 signature matches the actual installed version 4 and the retained
M3/M4.2 signer. SHA-256: `8299fa4fb782892f409ced06f53e9cb01b1d29348b4bccd894cafc6e4c8c494b`. Install as an update only; never uninstall or
clear storage. SDK 24 minimum / 36 target, permissions, bundled 240-question bank
and private credential exclusion are unchanged.

Pre-edit recoverable source checkpoint: `QuizPK-M4.2-before-refresh-fix.tar.gz`,
218 verified files, SHA-256
`356db45de02e20eeb9ad06e413a0482a167c77af3a720667b0065a72115390c6`.
The preservation audit checks all 36 private/live files by bytes without opening
live SQLite; original M3/M4 checkpoints and APKs remain unchanged. No backend
deployment/start, provider request, credential rotation, live DB operation,
Android installation or installed saved-data access occurred in this patch.

Reproduce in this ARM Termux toolchain from the project root:

```sh
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  :app:assembleDebug :app:lintDebug :app:testDebugUnitTest \
  --offline --console=plain --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx1536m \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2 \
  -PquizpkTestSqliteDir=/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host
```

The 30-second automated check uses simulated time and an Android editor input
connection; it does not certify the real keyboard, OEM autofill, radio or physical
rendering. Follow the [physical acceptance plan](M4_2_REFRESH_DEVICE_TEST_PLAN.md)
and the existing two-device lobby checklist. **Stopped for physical testing and
review. M4.3/live matches and LAN runtime remain deferred.** Earlier records follow.

---

# M4.2 stopped-maintenance recovery and credential rotation (2026-10-13)

User reported Step 3 liveDurableBytesEqual=false with the other four checks true,
and explicitly authorized revocation/rotation of the exposed phone01 credential.
Read-only investigation proves the main hash stayed equal; SQLite's mode=ro open
created an absent-to-empty WAL and SHM. A synthetic fixture reproduces it. The
failed backup hashes/integrity/restore/full rows verify; it is usable for data
recovery but contains the now-compromised pre-rotation identity credential.

Only phone01's token hash and private token file were rotated. Both user IDs, the
other user's credential, all four jobs, throttle, M3 schema and user_version are
preserved. Token replacement is 256-bit random, private/0600, flushed before
commit and atomically installed; no token/hash was printed. Exposed-token
rejection is proven on the rotated DB and M4.1/M3-rollback staging.

Corrected stopped backup reads only a hash-verified private main+WAL copy, retaining
strict live byte equality and stopped/owner checks. It never ignores a nonempty
WAL or opens live SQLite just to back up a stopped host. Online mode is unchanged.
New post-rotation backup: `~/.local/share/quizpk-backups/20261013T033128Z-maintenance`.
All five checks pass. New staging passes **59/59**, backend **54/54**, operator
safety **17/17**. Receipts bind exact backup/release manifests; stopped guards
reject DB drift and --require-revocation requires the exposed-token evidence.

**Live backend remains stopped; no M4 deployment/live migration/start, DB-file
overwrite/restore, provider request, Android change or M4.3 work occurred.**
Only private staging servers ran on disposable DBs/ephemeral ports. Old failed
manifests, backups and missing-window-path state are retained; no check was
overridden. New prepared-window.path records a verified candidate, not approval.
See [findings, risks and safe next commands](M4_2_MAINTENANCE_RECOVERY.md).
Evidence: `validation/m4.2-maintenance-recovery/`. Records below are historical.

---

# M4.2 backend deployment preparation — stopped for review (2026-10-13)

Read the deployment plan, two-device plan, common protocol and this entire history.
Conditional approval was used only for private live backup and isolated staging.
**No live deployment, migration, restart, provider call, new user, tunnel change,
Android update or M4.3 implementation occurred.**

Private verified recovery point:
`/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T023118Z-preflight`.
It contains a read-only-source SQLite online snapshot, exact M3 source/config,
token files, captured environment/tunnel startup configuration and independent
restore; 0700 directories / 0600 files. No secrets, private hashes or DB rows are
published. Source/config/tokens remain byte-identical. Normal M3 activity added a
fourth live job during preparation; original jobs/identities remain intact and
the newer snapshot includes this activity. Earlier snapshots are retained.
Read-only SQLite may update transient -shm coordination, never restored over live.

Evidence under `validation/m4.2-deployment-preflight/`:

| Check | Result |
| --- | --- |
| Backend regressions | 54 passed, 0 failed/skipped |
| Actual-snapshot migration/lobbies/token/rollback/re-upgrade rehearsal | 55 passed; both original tokens, all four cached responses preserved |
| Operator safety tests | 8 passed; fully synthetic, exec mocked |
| Public HTTPS health | 200, default certificate verification; no bearer sent |
| Provider calls | 0; rejecting injected provider and blank real-entrypoint credentials |
| Live/Android/checkpoint preservation | Boolean-only audit; source-only review checkpoints retained |

Rehearsal also confirms source rollback retains a simulated newly saved response,
multiplayer rows and identities; deliberate migration collision rolls back DDL.
The real Termux Node v24.18.0 production entrypoint runs against another copy on
an ephemeral loopback port and shuts down cleanly. Initial two safety-test fixture
failures are retained; explicit private-directory permissions/resolved paths fix
the fixture without weakening guards.

Actual M3 uses manual npm start and a direct Cloudflare Quick Tunnel, with NODE_ENV
unset. Proposed launch enables production HTTPS-header enforcement; public-edge
room/auth smoke remains pending after reviewed deployment. Normal rollback keeps
the current DB; a full rewind requires a separate accepted loss-window decision.
See [audit/risks](M4_2_DEPLOYMENT_PLAN.md) and
[exact commands/rollback/expected results](M4_2_DEPLOYMENT_RUNBOOK.md).
Android's previous 81-test result below is retained evidence, not a new Android
run; source/APKs are unchanged. Physical two-phone acceptance remains pending.

---

# M4.2 native Online lobbies — stopped for review (2026-10-13)

M4.1 was approved; only M4.2 was implemented from the replacement Online + Local
brief. M1–M3 remain user-approved. This stage adds native Java/XML Online entry,
create/join and Classic/nonplaying Moderator lobbies, using the unchanged M4.1
backend and canonical protocol. Live matches and LAN runtime remain deferred.

## Verification

| Check | Result | Evidence under validation/m4.2/ |
| --- | --- | --- |
| Backend regressions | **54 passed**, 0 failed/skipped | backend-regressions.txt |
| Full Android regressions | **81 passed**, 0 failures/errors/skips | android-final-validation.txt, android-tests/, test-summary.json |
| Existing M1–M3 Android tests | **56 passed**, source unchanged | android-tests/ |
| New native lobby flow tests | **13 passed** | android-tests/TEST-com.zeke.quizpk.LobbyFlowTest.xml |
| New Java codec/HTTPS tests | **11 passed** | android-tests/TEST-com.zeke.quizpk.LobbyProtocolTest.xml |
| Two Java clients → real Node/SQLite lobby | **1 passed**, disposable DB | android-tests/TEST-com.zeke.quizpk.LobbyBackendIntegrationTest.xml |
| Android assembly/lint | **Successful**, 0 lint errors / **49 warnings** | android-final-validation.txt, final-lint-results-debug.*, lint-summary.json |
| Preserved offline bank | **240 structurally valid questions** | preserved-bank-audit.txt |
| Signed APK/update metadata | Valid v2, same signer as **actual installed M3** | apk-signature.txt, apk-summary.json, installed-app-baseline.json |
| Source/private-state/checkpoint audit | Boolean-only results; 22 private/live files compared | security-preservation.json, checkpoint.json, review-checkpoint.json |

Android suites are Startup 7, OfflineData 19, OfflineFlow 11, AiData 10, AiFlow 9,
LobbyProtocol 11, LobbyFlow 13 and LobbyBackendIntegration 1. Final Gradle duration:
**12m 11s**. An earlier full `--rerun-tasks` execution rebuilt all production
inputs; after test-only harness corrections, the final combined assembly/lint/test
command reran the entire 81-test suite. APK production inputs and signer did not
change during those test corrections. The frozen APK equals the build output.

Lobby tests cover distinct Moderator capacity, own-role readiness, owner settings
and captured revisions, name/code validation, missing/full/429/old-M3 errors,
revocation, stale disabled controls, duplicate taps, exact-body uncertain retries,
recreation and authenticated process recovery, foreground heartbeat/background stop,
changed credentials, invitation privacy, confirmations and 320dp / 1.6-font forms
in both existing themes. The integration test uses real M4.1 HTTP routes/SQLite with
two synthetic identities and the production Java codec, plus a test-only loopback
HTTP adapter. The APK has no such adapter, Node server, fake identities or fixtures.
These are not public-edge TLS, physical-phone or LAN certification.

## Retained failures and corrections

- First main compile: ambiguous clipboard import; corrected explicit text imports.
  The first targeted Gradle setup also caught a test-resource File/String mismatch.
  Both failed logs remain; no toolchain downgrade or warning suppression was added.
- Initial targeted tests: **24/25 passed**. Java `trim()` silently removed trailing
  NUL unlike Node's codec. Implemented the authority's ECMAScript boundary-space
  policy and added NUL/C0/Unicode whitespace assertions without relaxing validation.
- Initial expanded run: **80/81 passed**. The new heartbeat test jumped virtual
  time across a real worker callback, making its scheduled poll assertion race.
  It now advances one polling interval at a time and waits for worker completion.
- Second expanded run: **80/81 passed**, all 25 new lobby tests green. An unchanged
  M3 retry test observed 0 saved quizzes instead of 1. SQLite trace showed its worker
  importing into a lobby test's temporary directory. Local AndroidX 2.5.1 bytecode
  confirms the default AndroidViewModelFactory retains an Application in `sInstance`.
  New lobby tests now clear that singleton before/after each test and assert the
  default AI model uses the current test Application. Production M3 code and all
  original 56 tests remain unchanged. A fresh pre-cleanup run passed 81/81; the final
  run also passes 81/81 with the cleanup included. All prior logs/XML are retained.

The full forced compile reports seven test-only deprecations (legacy host SQLite
and the chooser's legacy Parcelable getter), plus the prior unchecked test note.
Final incremental compilation reports only the changed test's two deprecations.
Lint has **49 warnings**, versus M3's 39: nine retained old multiplayer placeholder
strings are now unused, plus a lobby plural candidate and bounded roster refresh
advisory; the formerly unused warning color is now used. Allowed question counts
are all plural (5/10/15/20), and roster refresh occurs only on changes for at most
eight contestants. Remaining warnings cover prior resources/icons/Gradle/build UI.
No lint suppressions, baseline hiding or original resource deletion was introduced.

## Delivery and preservation

APK: `artifacts/QuizPK-M4.2-online-lobby-debug.apk`, **6,294,852 bytes**,
`com.zeke.quizpk`, version **4 / 1.3-M4.2**, min SDK 24, target SDK 36.
SHA-256: `1cc16d7c56212d6eac82d59f29987224f044e1223d3c1b40bb1724245b1ff2c2`.
Certificate SHA-256:
`f348cf5d46fb928644a96ef40bcecbaae780d45f6e15cdcdca42d1da1ee0eaff`.
Read-only package inspection confirmed the installed version 3 / 1.2-M3 is exactly
the retained M3 APK, with that signer. The new APK was not installed during this
session. Use an update installation, never uninstall or clear storage.

Pre-edit M4.1 checkpoint: 192 files, SHA-256
`c76afd06599d4e9b0d551b2229ecc8925091aa22130e2c9fb66de6eedcf5f958`.
M3's 176-file source checkpoint, original archive and APK remain unchanged.
A separate completed-stage source checkpoint and manifests exclude all private
state, signing, generated output and validation. See [recovery/setup](M4_SETUP.md).

Production backend/protocol and prior tests are byte-preserved. Android manifest,
backup policy, brand/themes, bank, startup/navigation, SQLite v3, solo gameplay,
history/drafts, AI/saved quiz and encrypted credential implementations remain
unchanged. Only the multiplayer placeholder and app version/test resource setup
were replaced among previous Android inputs. The APK contains no actual keys,
tokens/token hashes, private DBs, test runtime or canonical test fixture.

**Live Termux source, credentials, tokens and durable DB/WAL were not changed.**
No server/tunnel restart, migration, provisioning, provider call or paid deployment
was performed. The live M3 host does not support room routes; installation alone
cannot enable lobbies there. Review the separate
[migration/deployment plan](M4_2_DEPLOYMENT_PLAN.md) before authorizing that work.
The audit compares file bytes without opening live SQLite or printing private hashes.
No installed app data was opened or modified; regressions use temporary test DBs.

## Reproduce and device review

From `backend/`: `npm test`. From the project root in this Termux toolchain:

```sh
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  :app:assembleDebug :app:lintDebug :app:testDebugUnitTest \
  --offline --console=plain --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs=-Xmx1536m \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2 \
  -PquizpkTestSqliteDir=/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host
python3 tools/validate_question_bank.py
python3 tools/m42_inventory.py
apksigner verify --verbose --print-certs artifacts/QuizPK-M4.2-online-lobby-debug.apk
```

The optional ARM host SQLite adapter is test-only; desktop hosts use Robolectric's
normal adapter. Use local SDK/toolchain paths appropriate to the machine. The
preservation tool accepts a private pre-edit baseline with
`--preservation-manifest /private/path/before.json`; that manifest is deliberately
not distributed. Source inventory and all boolean audit evidence are public.

No ADB device was attached; read-only package inspection does not certify an
installation or touch/rendering/network behavior. Follow the
[two-device lobby acceptance plan](M4_2_DEVICE_TEST_PLAN.md) after approved server
deployment, including existing saved AI replay and M2/history checks. Full Moderator
acceptance in later stages needs a moderator plus two contestants; two phones here
exercise one moderator plus one contestant only. **STOP for M4.2 review.**

The M4.1 record below is historical; its approval gate was subsequently approved.

---

# M4.1 — current verification and review gate (2026-10-12)

Only the Online lobby backend foundation and common protocol design are implemented.
The replacement Online + Local brief records the user's approved M1–M3 phone/live
AI/offline replay checks. This stage preserves that baseline; it does not repeat
those external checks or implement M4.2 UI, live matches or embedded LAN hosting.

## Automated results

| Check | Result | Evidence under validation/m4.1/ |
| --- | --- | --- |
| Final backend suite | **54 passed**, 0 failed/skipped | backend-final-tests.txt |
| Existing M3 provider/API/restart tests | **20 passed** within final suite | backend-final-tests.txt |
| New room HTTP/SQLite integration tests | **29 passed** | backend-final-tests.txt |
| Protocol codec and future-rule reference tests | **5 passed**, 27 design vectors | backend-final-tests.txt |
| Android M1–M3 regressions | **56 passed**, 0 failures/errors/skips | android-regression-tests.txt, android-tests/, test-summary.json |
| Restored source-only M3 checkpoint | **20 passed**, all 176 restored hashes match | checkpoint-restored-m3-source-tests.txt, checkpoint.json |
| Backend syntax | **19 modules checked** | backend-syntax.json |
| Offline bank audit | **240 valid entries** | preserved-bank-audit.txt |
| Retained M3 APK signature | Valid v2; same M2/M3 certificate | retained-m3-apk-signature.txt |
| Private-state/source/security preservation | All final checks passed | security-preservation.json |
| Existing actual Termux DB identity check | Two distinct enabled identities confirmed read-only; still M3 schema | existing-termux-db-readonly.json |

Android suites: Startup 7, OfflineData 19, OfflineFlow 11, AiData 10, AiFlow 9.
They reran all tasks with the existing Robolectric SDK-34 model and real ARM host
SQLite adapter. Gradle completed in **14m 1s**. The compiler reports the existing
five legacy-SQLite deprecations and unchecked-operation test note. No new clean
APK assembly or lint was needed for unchanged Android inputs; M3's historical
lint is **0 errors / 39 warnings**, not a fresh M4 lint claim. No new APK was made.

The final backend suite exercises real loopback HTTP clients and real temporary
SQLite, never the user's live DB. It covers role escalation/isolation, Moderator
plus eight contestants, concurrent last-seat joins, idempotency conflicts/replays,
host absence/recovery, stable IDs/reserved seats, readiness/settings revisions,
code collision/expiry/guess caps, presence and restart epochs, revocation, safe
projections, body/schema/TLS rejection, rate/retention boundaries and migration
preservation. Existing generation budgets/auth/idempotency still pass unchanged.
The original provisioning CLI and new read-only identity checker are tested with
a populated temporary M3-compatible DB. The checker was also run read-only against
the actual existing Termux DB and both existing private token files; no new live
identity was provisioned and no live migration was performed.

Future scoring/order/moderator transitions use shared JSON fixtures: seven score,
four order and sixteen transition cases. They validate a **test-only reference**;
they are not live Online rounds, Java host conformance or offline LAN results.
Minimum-two match start, real deadlines/answers/results and queue permissions
belong to later stages and are not claimed implemented from these design tests.

## Reproduce

```sh
cd backend
npm test
```

From the project root in the available Termux/PRoot toolchain:

```sh
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  :app:testDebugUnitTest --rerun-tasks --offline --console=plain --no-daemon \
  --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1536m \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2 \
  -PquizpkTestSqliteDir=/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host
python3 tools/validate_question_bank.py
python3 tools/m4_inventory.py
python3 tools/verify_m4_security.py
```

The SDK/test-JNI paths are local test infrastructure, not app requirements or APK
content. The audit also accepts `--preservation-manifest /private/path/before.json`
to compare the private pre-edit file hashes without printing them. This session's
private manifest is outside the workspace and is not distributed in reports or
checkpoints. Without it, the reusable audit still checks public-source preservation,
checkpoint/APK/secret scans and ignore rules, but does not claim the private-file
before/after comparison. `--external-preservation-manifest` additionally checks
the actual Termux DB/WAL, tokens and provider credential against a private baseline;
it excludes the transient SQLite `-shm` reader-coordination file from byte checks.

## Recovery, preservation and limitations

Primary source checkpoint: `../QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz`,
**176 files**, SHA-256 `8fe1a54a07768aaec255b67a1c54923b55127e2005e673d7939386d57f690d28`.
It derives exclusively from the pre-edit 275-file archive, omitting historical
validation reports. All hashes and restored M3 tests pass. The original archive
remains intact; see [M4 setup](M4_SETUP.md) for safe recovery into a separate folder.

All **153 protected Android/build/reference/M3 core files** match the checkpoint.
All **four preexisting credential/token/signing files** match the private baseline;
their values and hashes are not reported. Android SQLite remains v3, package
`com.zeke.quizpk`, version `1.2-M3` / code 3. No app-data deletion/uninstall/reset or
private DB overwrite occurred. The retained APK is 6,210,337 bytes, SHA-256
`7fe96067cabeac51ce337a022e91743729c06d5f6752100e1a43b98d1665ff76`, and its certificate
SHA-256 remains `f348cf5d46fb928644a96ef40bcecbaae780d45f6e15cdcdca42d1da1ee0eaff`.

The first expanded backend run was **49/50**, retained in
backend-first-full-tests.txt. Malformed invite-code errors were corrected to match
unknown/expired 404 errors; backend-second-full-tests.txt is **50/50**, before four
boundary tests were added. The first preservation audit rejected historical
validation reports in the original checkpoint; security-first-check.json is kept.
The source-only archive excludes those reports, and the final audit passed.

Workspace `backend/data/` has no private DB (workspace-private-db-status.json).
The actual host was found at `/data/data/com.termux/files/home/quizpk-server`.
Both distinct existing token files match enabled users in its actual DB; the live
schema is still M3. The **five durable private files** (DB, WAL, two tokens, provider
credential) remain byte-for-byte unchanged. SQLite's read-only WAL reader updated
transient `-shm` coordination; the initial strict byte-check result is retained in
existing-termux-db-first-check.json. No saved data, schema or identity was changed,
and the live coordination file was never restored/overwritten. No backend/tunnel was restarted,
no Gemini/provider request was made and no key/token/model/billing setting changed.

| Acceptance category | This stage |
| --- | --- |
| Automated backend/migration/protocol reference | Passed as scoped above |
| Automated M1–M3 Android regressions | Passed; JVM tests, not physical phones |
| M1–M3 physical/live AI acceptance | User-reported approved baseline in current brief |
| M4 single-device or multi-device UI | Not tested; no M4 client UI exists |
| Existing actual private DB identities | Passed read-only for both distinct tokens; M3 schema preserved |
| Public HTTPS room smoke / live DB migration | Not run; operator steps supplied |
| Online live Classic/Moderator matches | Deferred to M4.3–M4.7 |
| Java/LAN transport and Node/Java equivalence | Deferred to M4.8–M4.10 |
| Disconnected Wi-Fi/data-off hotspot games | Not implemented/tested yet |

Read the multi-phone plan in [M4_SETUP](M4_SETUP.md). **Stop for user review before
M4.2**, as required by [the current brief](../QuizPK_M4_ONLINE_AND_LOCAL_PROMPT.md).

---

# Historical M3 implementation-session verification (2026-10-12)

The user's later approval supersedes the pending acceptance statements in this
historical record. Commands and earlier results are retained for traceability.

M3 source is implemented. **Clean build, lint and automated tests passed;
physical-device and live Gemini acceptance remain pending.** The M3 brief records M1/M2 approval
on the user's TECNO Android 14 phone. Earlier M1/M2 pending-device statements below
are retained historical evidence, not the current approval status.

## What has been checked

- Backend mock tests: **20 passed**, no failures/skips. These exercise the actual
  loopback HTTP server and SQLite with injected provider responses. No live Gemini
  access or provider quota is claimed. Evidence: `validation/m3/backend-tests.txt`.
- Targeted AI UI rerun: **9 passed**, no failures/errors/skips; Gradle successful
  in 6m 24s. Evidence: `validation/m3/ui-targeted-rerun.txt` and
  `validation/m3/targeted-ai-ui-tests.xml`.
- Final clean Android suite: **56 passed**, no failures/errors/skips; assembly,
  lint and tests completed in 7m 48s. Lint: **0 errors, 39 warnings**.
- Existing credential location/format and boolean loading checked without outputting
  the value. Model remains unset and free-tier confirmation false. No live request,
  hosting deployment or paid billing was performed.
- Seed audit: all **240** offline questions remain valid. Preservation/security
  checks cover startup, branding, themes, references, unchanged gameplay/scoring,
  backup exclusions and effective ignore rules. Final packaged APK audit passed.

Automated Android tests use Robolectric 4.14.1's SDK-34 model, legacy graphics,
Conscrypt disabled and the existing real ARM host SQLite JNI adapter. That adapter
executes SQL and is test-only; it is not shipped. JVM layout/theme checks cannot
certify phone rendering, Android Keystore, TLS trust, radio/network conditions,
OEM lifecycle/process termination, keyboard behavior or real countdown timing.
No device/emulator is attached (`validation/m3/adb-devices.txt`).

## Reproduce the checks

From the project root in this Termux/PRoot environment:

```sh
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  clean :app:assembleDebug :app:lintDebug :app:testDebugUnitTest \
  --offline --console=plain --no-daemon --max-workers=2 \
  -Dorg.gradle.jvmargs=-Xmx1536m \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2 \
  -PquizpkTestSqliteDir=/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host
python3 tools/validate_question_bank.py
python3 tools/verify_m3_security.py
```

From `backend/` with Node.js 24:

```sh
npm test
npm run check-config
```

Normal desktop hosts may omit the host SQLite property. The test adapter's build
steps remain in `tools/prepare_arm_sqlite_tests.sh`. Backend tests use mock keys
and provider fixtures; neither command performs a live generation.

## Final M3 automated results

**BUILD SUCCESSFUL in 7m 48s; 55 actionable tasks, all 55 executed.** The final log
is `validation/m3/clean-build-lint-tests.txt`. All **56 Android tests passed**, with
0 failures, 0 errors, 0 skipped. The 113 recorded Android/build source input hashes
are unchanged after verification (`source-inputs-unchanged.json`). Final test XML
is in `validation/m3/final-tests/`; counts are in `test-summary.json`.

| Suite | Tests | Scope |
| --- | ---: | --- |
| StartupTest | 7 | M1 startup, navigation, themes, recreation and persisted appearance |
| OfflineDataTest | 19 | M2 bank/filtering, v1 upgrade, scoring, drafts, history and isolation |
| OfflineFlowTest | 11 | M2 gameplay/timing/results/review, exit/resume, navigation and compact layout |
| AiDataTest | 10 | Settings, malformed/partial/duplicate batches, idempotent UUID import, v2 upgrade preserving 240/history/draft, AI scoring, deletion snapshots, reports, mocked network/HTTP errors and HTTPS origins |
| AiFlowTest | 9 | M2 engine/report/replay, AI draft recreation, both themes, rename/delete, interrupted identity, 320dp/1.6 font layout, duplicate taps, explicit retry and cancellation |
| Backend node tests | 20 | Request/output schemas, authentication, HTTPS enforcement, quotas/throttle/concurrency, cached retries/conflicts, sanitized errors, bounded structured Gemini requests and on-disk restart/revocation |

Lint has **0 errors and 39 warnings**, with no added suppressions. Compared with
M2's 34 warnings: one `NestedWeights` in the saved-list layout, one
`PluralsCandidate` (only permitted counts 5/10/15/20 are displayed), and three
now-unused legacy AI placeholder resources. Other advisories concern existing
unused resources, SDK/dependency versions, overdraw, icons, layout hierarchy,
release shrinking, toolbar action and test dependency catalogs. All ID counts
and locations are retained in `lint-summary.json` and `final-lint-results-debug.*`.
The Java compiler also reports five test-only legacy-SQLite deprecations and an
unchecked-operation test note; experimental AAPT2/PRoot environment warnings remain.

The first clean attempt passed assembly/lint and 53/56 Android tests; three newly
added UI tests asserted before queued AlertDialog/view callbacks. Processing the
main looper before simulated actions fixed the test harness without removing the
report, rename or invalid-input assertions. The targeted 9-test rerun then passed.
That failed clean log, first XML and lint report are retained as
`clean-first-ui-failures.txt`, `first-tests/` and `first-lint.txt` under validation/m3.
An earlier resource compile attempt overlapped resource edits; its log is retained
as `stage2-build-tests.txt`. Final verification uses frozen source.

## APK and credential protection

Delivery: **`artifacts/QuizPK-M3-ai-debug.apk`**, **6,210,337 bytes**, version
`1.2-M3`, code 3. SHA-256:
`7fe96067cabeac51ce337a022e91743729c06d5f6752100e1a43b98d1665ff76`.
The bytes match the final clean build output. M1/M2 APKs are retained.

`apksigner verify --verbose --print-certs` passed: v2 scheme, one signer, same
certificate as M2 (certificate SHA-256
`f348cf5d46fb928644a96ef40bcecbaae780d45f6e15cdcdca42d1da1ee0eaff`).
AAPT2 confirms `com.zeke.quizpk`, launcher `com.zeke.quizpk.MainActivity`, minimum
API 24/target 36, INTERNET permission and `usesCleartextTraffic=false`. Packaged
backup/transfer rules exclude the login and pending request preferences. No test
JNI/loader/backend is bundled; `assets/questions.json` matches the unchanged bank.
Evidence: `apk-signature.txt`, `m2-apk-signature-comparison.txt`, `apk-badging.txt`,
`apk-manifest.txt`, packaged backup-rule dumps, `apk-metadata.json`, `apk-sha256.txt`
and `security-preservation.json` under `validation/m3/`.

Provider credentials stay in the existing server-only file and hosting secret
settings. The Android client receives only a provisioned individual access token.
The security tool scans source, reports, backup contents and decompressed APK
entries for the local credential (without displaying it), checks packaging and
effective Git ignore rules, and compares protected M1/M2 files with the checkpoint.
No Git repository/history exists here. The ignored secret was never added to a
repository in this session; earlier chat exposure still requires rotation before
public release. The pre-M3 archive excludes credentials and signing files.

## External setup and physical acceptance — NOT EXECUTED

The server is deployable code with real Linux/Caddy/systemd instructions, but no
host account, real domain/TLS endpoint or personal user token was supplied. The
Gemini model/free-tier configuration is intentionally unset. See
[M3_SETUP](M3_SETUP.md) and the [backend README](../backend/README.md) for exact
private setup. Recommended model `gemini-3.5-flash-lite` was checked against
[Google's model documentation](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite)
and [Standard pricing](https://ai.google.dev/gemini-api/docs/pricing#gemini-3.5-flash-lite)
on 2026-10-12. Actual access/quota has not been tested. Consumer Gemini plans do not
provide API credits; do not enable paid billing without permission.

Install the final APK **as an update without uninstalling M2**:

```sh
adb install -r artifacts/QuizPK-M3-ai-debug.apk
```

1. Confirm startup, navigation and AI Quiz in both light/dark modes; check small
   width, large fonts, keyboard access and TalkBack labels on the actual phone.
2. Submit empty/short topics and check helpful validation. Confirm the four
   difficulties, 5/10/15/20 question counts and 10/15/20/30 second timers.
3. Deploy/configure the HTTPS server, rotate the prior key, confirm free-tier
   project status and privately provision an individual token. Enter the real
   origin and that token in **AI Quiz → Connect to QuizPK**. Never enter the
   Gemini key there. Verify unauthorized calls fail.
4. Online, generate a five-question Java quiz. Try duplicate taps, backgrounding
   and rotation; verify a single saved collection. Check factual quality manually.
5. Play with correct/wrong/skip/timeout answers. Verify timer, M2 scoring, results,
   explanations, history and local **Report incorrect question** feedback.
6. Close/reopen, find Saved AI Quizzes, then enable Airplane Mode and Play Again.
   Complete and review fully offline. Rename, cancel deletion, then delete; verify
   historical results and an existing active snapshot still reopen.
7. Test airplane-mode generation, client timeout, invalid/revoked token, server
   outage and quota limits. Retry explicitly; recover a pending request without
   creating a duplicate. Cancellation stops waiting; an accepted server request
   can still finish and consume quota. No automatic paid retry is performed.
8. Recheck all M2 categories and existing history/draft after the update, including
   airplane mode and both themes. Reconnect after uninstall/restore if necessary;
   Android Keystore/backup behavior requires a real-device check.

AI schema/consistency checks do not prove factual correctness or full topicality.
Answers require review before authoritative grading. Reports are stored locally
and are not sent to a server. Saved replay never invokes the generation client.
M3 remains unaccepted until the real configured backend and phone pass this list.

---

# Historical M2 offline gameplay verification (2026-10-12)

The user approved all seven M1 checks on a physical TECNO Android 14 device before
authorizing M2. The old P0/M1 entries below are retained as historical evidence.
The MainActivity.updateChrome() fix is preserved.

M2 final clean verification passed; results and evidence are recorded below. No M2
physical-device or emulator test is claimed: current `adb devices` has an empty list,
and no emulator is available. Automated tests use Robolectric 4.14.1's SDK-34 model,
legacy graphics and Conscrypt disabled, with real host SQLite. They do not reproduce
the TECNO firmware, system splash, graphics rendering or Android process termination.

## Build and automated checks

Exact command in this environment:

```sh
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  clean :app:assembleDebug :app:lintDebug :app:testDebugUnitTest \
  --offline --console=plain --no-daemon \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2 \
  -PquizpkTestSqliteDir=/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host
python3 tools/validate_question_bank.py
```

Final Gradle log: `validation/m2/clean-build-lint-tests.txt`.
First assembly and failed native-library attempts are also retained there. The first
fully working host-adapted run passed 34 tests. Final results follow this section.
A subsequent 37-test clean attempt had 36 passes
and one newly added exit-test failure: it asserted before the queued AlertDialog
callback. After processing that callback, finish and storage checks passed; a targeted
rerun then failed only on an exact 13,000 vs 12,999 ms comparison after explicit
Resume. The corrected test verifies the saved checkpoint exactly before reopening
and allows legitimate elapsed foreground time afterward. Both attempts are retained
as `clean-first-dialog-test-failure.txt` and `dialog-test-after-idle.txt`.

The test host is Linux aarch64/Termux/PRoot. Both Robolectric's normal native SQLite
selection and its distributed legacy sqlite4java architecture loader initially failed.
The test-only adapter loads JNI compiled from GrapheneOS sqlite4java commit
`789dab5f1d816bfe7cc01963ed157a637cf00fe9` against host SQLite 3.53.4. It executes
real SQL; it does not mock database answers. `tools/prepare_arm_sqlite_tests.sh`
reproduces the JNI build with git, SWIG, Termux clang, SQLite headers and the host JDK.
The optional Gradle property is only needed on this host; normal supported desktop
hosts may omit it. The JNI library and adapter are not part of production resources.

The structural bank audit checks 240 unique IDs/prompts, four distinct choices,
answer indices, source metadata and all 48 category/difficulty counts. Facts were
assistant-reviewed with attributed references; there was no independent human review
or calibrated difficulty study. HTTP access failures are preserved as access failures.

## Final M2 results

**Clean build, lint and automated tests passed**: `BUILD SUCCESSFUL in 4m 46s`,
55 actionable tasks, all 55 executed. Source-input hashes were checked unchanged
after verification. 37 tests passed; 0 failures, 0 errors, 0 skipped.

| Suite | Tests | Coverage |
| --- | ---: | --- |
| OfflineDataTest | 19 | Seed/reopen, migration preserving data, counts/filtering, randomized unique selection, insufficient counts, centralized scoring, timeout/skip, duplicate gates, draft/history persistence, old-result/different-draft isolation |
| OfflineFlowTest | 11 | Category/configuration, shortages/alternatives, 5- and 20-question UI flows, feedback/review, explanations off/no early key markers, double taps, handler timeout, save/exit/resume, background/recreation, history reopening, Play Again/Home intents, light theme and compact 320dp/1.6 font configuration targets |
| StartupTest | 7 | M1 startup, five tabs, Settings/Back, repeated activity lifecycle, theme persistence and recreation |

Lint: **0 errors, 34 warnings**, no lint baseline or suppression added. Full location/
message details: `validation/m2/final-lint-results-debug.txt` and HTML/XML/SARIF.

| Warning ID | Count |
| --- | ---: |
| `OldTargetApi` | 1 |
| `GradleDependency` | 3 |
| `ObsoleteSdkInt` | 1 |
| `Overdraw` | 2 |
| `UnusedResources` | 17 |
| `NotShrinkingResources` | 1 |
| `UselessParent` | 1 |
| `MonochromeLauncherIcon` | 2 |
| `IconDipSize` | 1 |
| `IconDuplicatesConfig` | 1 |
| `IconLocation` | 1 |
| `AlwaysShowAction` | 1 |
| `UseTomlInstead` | 2 |

Other build warnings: three Java deprecation warnings for test-only
`SQLiteMode.Mode.LEGACY` (one per test class); AGP warns that the explicit AAPT2
override is experimental; Gradle warns that native daemon environment integration
is unavailable in this PRoot environment. These are recorded without suppression.
No Java compilation error or runtime-app dependency change occurred. Dependency
upgrade/SDK and release-shrinking advisories remain outside this M2 scope.

Question bank audit passed. Direct access checks cover 172 canonical source URLs:
124 returned HTTP 200; 48 were blocked, temporary server errors, resets or timeouts.
There are no remaining confirmed-404 URLs in that access report. Browser-source
review and access limits are described in `validation/m2/source-review-notes.md`.
Semantic text color pairs in both themes exceed 4.5:1 in a static sRGB calculation;
this does not certify all device states, rendering or accessibility behavior.
M1 theme code, colors/themes/styles, brand vectors and all 19 references match the
pre-M2 backup byte-for-byte (`validation/m2/m1-preservation.txt`).

## Delivered APK

- `artifacts/QuizPK-M2-offline-debug.apk` (6,155,138 bytes).
- Normal output: `app/build/outputs/apk/debug/app-debug.apk`.
- Package `com.zeke.quizpk`; version `1.1-M2`, code 2; minimum API 24, target API 36.
- SHA-256: `b928ad122e4b58d07c0dedc0b8574f03731695ea169ec338e70b0ed4912fd14c`.
- `apksigner verify --verbose --print-certs` passed, v2 scheme, one signer.
  Certificate matches the retained M1 P0 APK, enabling an update without uninstall.
- AAPT2 verifies QuizPK label and MainActivity launcher. No INTERNET permission.
- Bundled questions exactly match the audited 240-row source asset. No test adapter,
  sqlite4java JNI library or Robolectric code is in the APK.

Metadata/signature/hash: `validation/m2/apk-metadata.json`, `apk-signature.txt`,
`apk-badging.txt`, `apk-sha256.txt`. Final test XML and all lint formats are retained
beside the clean log. Modified/added files: `docs/M2_CHANGED_FILES.md`.

Implementation and available automated verification are complete. **M2 physical
acceptance remains pending**; the user-reported M1 physical pass is separate.

## Device acceptance checklist — M2 NOT EXECUTED

Install as an update with the same signing identity, preserving M1 preferences:

```sh
adb install -r artifacts/QuizPK-M2-offline-debug.apk
adb logcat -c
adb shell am force-stop com.zeke.quizpk
adb shell monkey -p com.zeke.quizpk 1
adb logcat -b crash -d
```

Then manually verify:

1. Cold start three times, warm background/reopen, force-stop/relaunch; visible Home,
   no App Error or FATAL EXCEPTION. Recheck tabs, Settings, both themes and persistence.
2. Enable airplane mode and disable Wi-Fi. Choose Science, Easy, five questions.
   Check actual count, timer options, selection without immediate scoring, Submit,
   wrong/correct feedback, explanations, Next and final totals/review.
3. Choose a category/difficulty with a requested 20 questions. Confirm limitation,
   Use Available and Switch to Random Mix. Play 20 unique questions across categories.
4. Let a selected-but-unsubmitted answer time out. Skip another. Both count unanswered.
   Repeatedly tap Submit/Next; confirm one score/resolution and no repeated question.
5. Exit and save during a question, relaunch and resume; test Discard, replacement
   confirmation, Home/Profile Resume, background pause and rotation before/after reveal.
   Repeat with large system font and a narrow screen; inspect scrolling and TalkBack.
6. Finish, use Play Again/Back to Home, open Profile history, force-stop and reopen
   the completed review. Verify date/category/difficulty/points/accuracy/count/duration.
7. Repeat the flow in light and dark themes and confirm bars/cutouts and contrast.

Not executed: physical touch/visual comparison, TalkBack, actual offline network
disconnection, OS process-death/force-stop recovery, OEM/system-splash behavior,
real-time timer drift, Android instrumentation, oldest API-24 runtime and large-history
performance. JVM recreation, SQLite reopen and preference tests cover their modelled
paths only. No M3/AI/multiplayer behavior was enabled or tested in M2.

---

# Historical P0/M1 evidence (superseded device-approval status)

# P0 startup fix — historical validation before user approval

The original M1 debug APK was reported to crash after the splash screen on a
TECNO KL7 / Android 14. The historical M1 build results below are not device
startup validation and did not override this report. M2 was on hold at that stage.

## Confirmed source defect and regression evidence

Robolectric 4.14.1 at SDK 34 reproduced the original activity's cold-start failure:
`Resources$NotFoundException: Resource ID #0x0` at original
`MainActivity.updateChrome(MainActivity.java:151)`, via
`Toolbar.setNavigationIcon(Toolbar.java:1046)`. The zero resource ID was being
used to clear the toolbar back icon. Opening Settings had the same invalid zero
lookup in `setLogo(int)`. Both now use the Drawable overload with `null` to clear.

See `docs/STARTUP_CRASH_FIX.md` and the captured `before-fix-test.xml` /
`before-fix-stacktrace.txt` under `docs/validation/p0-startup/`. This is an actual
JVM test exception, **not** a device FATAL EXCEPTION or logcat capture.

No ADB target/emulator is available. Native crash-buffer reading returned no
lines, which does not prove permission to read the QuizPK process or absence of
a crash. The exact TECNO crash cause remains unverified without its real log.

## Commands

The first offline regression attempt could not resolve an uncached Guava Android
variant. An online dependency resolution allowed the original one-test startup
regression to run and fail with the exception above (`BUILD FAILED in 2m 36s`).
No production dependency version was changed.

```sh
# Original activity: confirmed failing cold-start regression (retained evidence).
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
  :app:testDebugUnitTest --tests com.zeke.quizpk.StartupTest \
  --console=plain --no-daemon \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2

# Fixed activity: clean build, lint and complete startup suite.
ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew clean \
  :app:assembleDebug :app:lintDebug :app:testDebugUnitTest \
  --offline --console=plain --no-daemon \
  -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2
```

The production patch's first full run passed six startup/navigation/recreation
checks. Two intermediate theme-test failures were test-harness issues: assuming
a radio button's click return value proves selection, then closing an activity
before AppCompat's queued recreation ran. The test now checks actual selection,
night configuration and stored choice, and drains the main looper before closing.
The corrected isolated theme regression passed (`BUILD SUCCESSFUL in 2m 20s`).
Intermediate results are retained alongside the final evidence; neither failure
was hidden or treated as a passing test.

Final complete clean run: **BUILD SUCCESSFUL in 5m 27s**, 55 actionable tasks
(54 executed, 1 up-to-date). `clean`, `:app:assembleDebug`, `:app:lintDebug` and
`:app:testDebugUnitTest` completed successfully. Lint: **0 errors, 34 warnings**;
no new suppression was added. The two additional warnings recommend putting
the test-only dependency versions in the existing TOML catalogue. Warnings are retained in
`docs/validation/p0-startup/lint-results.txt` and the generated HTML/XML/SARIF.

**7 SDK-34 JVM tests passed, 0 failures/errors/skips**:

| Coverage | Result in Android 14 JVM model |
| --- | --- |
| Cold activity start, Home inflation, brand logo/no back icon | Passed |
| Home cards and all five destinations | Passed |
| Settings from Profile, repeated opening, Back and logo restoration | Passed |
| Header Settings action and Back to Home | Passed |
| Light/dark switching, queued recreation, retained Settings and stored theme on new activities | Passed |
| Three activity launches and warm stop/restart lifecycle | Passed |
| Activity recreation restores Settings and its previous Explore destination | Passed |

No device/emulator result is implied by this table. In particular, new-activity
preference restoration is not process-death/force-stop persistence verification.
The suite uses legacy graphics and disables Conscrypt; actual rendering, OEM
behavior and native/OS paths still require device checks.

Delivered debug APK: `artifacts/QuizPK-M1-P0-startup-fix-debug.apk` (6,061,576 bytes).
The normal Gradle output is also `app/build/outputs/apk/debug/app-debug.apk`.
SHA-256: `623035a3377290b3440e766f6547a9ab4914fe3b4732fb46e3ff6b8cbf49135f`.
`apksigner verify --verbose` passed with v2 signing / one signer; the final
clean-built APK matches the verified bytes. AAPT2 confirms package
`com.zeke.quizpk`, label QuizPK, launcher MainActivity, minimum API 24 and target
API 36. All 820 packaged resource entries match the original M1 APK byte-for-byte.

Retained final evidence: `docs/validation/p0-startup/clean-build-lint-tests.txt`,
`final-tests.xml`, `lint-results.txt`, `apk-signature.txt` and `apk-metadata.json`.
The captured before-fix JVM exception remains beside them. The delivered APK is
newly built and locally regression-tested; its physical startup is not certified.

## Physical Android 14 verification — NOT EXECUTED

No APK installation, actual system splash transition, device cold/warm/repeated
launch, force-stop/relaunch, real touch navigation, system theme recreation or
physical storage persistence was verified. JVM activity recreation/relaunch tests
cannot substitute for process termination, OEM behavior or OS splash rendering.
No screenshot/visual acceptance or instrumentation pass is claimed.

With an authorized Android 14 target, install the new APK, then capture the real
sequence the user requested:

```sh
adb install -r artifacts/QuizPK-M1-P0-startup-fix-debug.apk
adb logcat -c
adb shell am force-stop com.zeke.quizpk
adb shell monkey -p com.zeke.quizpk 1
adb logcat -b crash -d
```

Repeat cold/force-stop launches at least three times. Also return Home to the
background and reopen for a warm start. Verify all five tabs, Home cards, Settings
from header/Profile, Settings Back, light/dark switching and saved selection after
force-stop/relaunch. Save any FATAL EXCEPTION and full Caused by chain. Do not use
an empty crash buffer alone as proof of success: confirm foreground activity and
visible Home. M2 was on hold pending startup acceptance at that stage; the later
user approval recorded above lifted that gate.

---

# M0/M1 validation — 2026-10-12

## Environment and inspection

- No Git repository at the workspace root. No commits/branches were created.
- Read the implementation brief and starter README; opened all 19 PNG references.
- `java -version`: OpenJDK 21.0.12 (Termux).
- `bash gradlew --version`: Gradle 9.6.1 / Kotlin 2.3.21, Linux aarch64/PRoot.
- `adb devices`: no attached device. No emulator binary found on PATH.
- Preserved package ID, Gradle versions, dependency catalogue and signing setup.

## Actual build attempts

1. `bash gradlew :app:assembleDebug --offline --console=plain`
   failed: `SDK location not found` (`BUILD FAILED in 2m 37s`).
2. `ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew :app:assembleDebug --offline --console=plain --no-daemon`
   failed: uncached `com.android.tools.build:aapt2:9.3.1-15703166`
   (`BUILD FAILED in 1m 50s`).
3. Used the available ARM-compatible SDK AAPT2 explicitly:

   ```sh
   ANDROID_HOME=/tmp/manhwa-toolchain/sdk bash gradlew \
     :app:assembleDebug :app:lintDebug --offline --console=plain --no-daemon \
     -Pandroid.aapt2FromMavenOverride=/tmp/manhwa-toolchain/sdk/build-tools/36.0.0/aapt2
   ```

   Initial M1 assembly and lint completed: `BUILD SUCCESSFUL in 2m 45s`;
   45 actionable tasks, 34 executed, 11 up-to-date. After review corrections,
   the final rerun also succeeded, as recorded below.
   The AAPT2 override is experimental and Gradle warns that this PRoot environment
   lacks native daemon environment integration. These warnings were not suppressed.

Final rerun: **BUILD SUCCESSFUL in 2m 20s**. 45 actionable tasks: 22 executed,
23 up-to-date. Both `:app:assembleDebug` and `:app:lintDebug` completed.
Lint reported **0 errors and 32 warnings**; warnings include the retained SDK/
dependency versions, unused scaffold tokens and starter assets, icon configuration,
layout simplification/overdraw and an always-visible Settings action. No lint
warnings were hidden. These are not a full visual/accessibility audit.

Built artifact: `app/build/outputs/apk/debug/app-debug.apk` (7,046,059 bytes).
AAPT2 confirms application `com.zeke.quizpk`, label QuizPK, launcher MainActivity,
minimum SDK 24 and target SDK 36. The merged APK has no internet permission;
its only listed permission is AndroidX's non-exported dynamic receiver permission.
`apksigner verify --verbose` passed for the final APK: APK Signature Scheme v2,
one signer (appropriate for minimum API 24). This verifies signing, not installation.
SHA-256: `58be63d9c6992b072eb5f2ecd74ab0ca8f1056c32eb202d4f156dd56b7a6fd4a`.

Retained evidence: `docs/validation/m1-build.txt`, `m0-sdk-retry.txt` and
`m1-apk-metadata.json`. The complete lint report is generated at
`app/build/reports/lint-results-debug.html` (also XML/text/SARIF).
A final XML parse covered 53 resource/manifest files. There are 19 reference PNGs
and zero PNGs in runtime resources.

## Scope of verification

XML parsing and Android resource linking, view-binding generation, Java
compilation, dexing, APK packaging and lint check source/build correctness.
They do not exercise Android runtime layout inflation or user interactions.
No unit/instrumentation suite exists in M1; no test count or device pass is claimed.
No live AI/multiplayer service is configured or tested.

A numerical color review found baseline dark violet too low in contrast for small
text on dark cards; a separate lighter `accent_text` token corrects labels and
selected navigation text while retaining the specified primary violet for controls.
This is a token calculation, not a full accessibility/device audit.

## Device checklist — NOT EXECUTED

1. Install and cold-launch in airplane mode: Home should show the honest M1 shell.
2. Tap every bottom destination and Home card; verify the corresponding native
   screen, category selection text and disabled unavailable actions.
3. Open Settings from each destination; use toolbar and Android Back to return.
   Back from a non-Home tab should go Home; Back from Home should leave the app.
4. Switch dark → light → dark; confirm colors/system bars update, the Settings
   screen stays selected and the choice survives force-stop/relaunch.
5. Select a category; open Settings and return, then rotate/recreate and verify
   selection and navigation restoration. Try rapid repeated Settings taps.
6. Test 320dp-wide phone, landscape, 200% font scaling, gesture/three-button
   navigation, cutouts, TalkBack and keyboard visibility. Check wrapping and scroll
   access to all controls. Test at least API 24 and API 36.
7. Confirm room/AI controls cannot create fake output, and there are no sample room
   codes, random/public matchmaking, fake players or illustrative statistics.

M2–M7 product acceptance tests in the brief remain unimplemented and unexecuted.
