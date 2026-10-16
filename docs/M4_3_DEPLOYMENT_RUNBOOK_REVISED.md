# M4.3 revised deployment runbook — BLOCKED, review only

Prepared 2026-10-13 from the complete
[V2 handoff](../QuizPK_M4_3_FRESH_SESSION_HANDOFF_V2.md). The filename requested
without `_V2` was absent. This document supplements, and does not overwrite, the
[historical runbook](M4_3_DEPLOYMENT_RUNBOOK.md). **No command below authorizes a
live action. No live deployment, restart, tunnel change, rotation, migration or
APK installation was performed in this session.**

## 1. Observations and closed gates

The credential-free check on 2026-10-13, before 14:39:57 UTC, returned **HTTP 530**
from `https://viking-prophet-parallel-researchers.trycloudflare.com/health` with
certificate verification enabled and redirects refused. The response was not
the expected JSON health response. Plain `127.0.0.1:8787/health` refused the
connection (errno 111); this did **not** establish a working HTTPS_REQUIRED gate.

The executor ran Termux Python and Node v24.18.0, but exposed only 12 numeric
`/proc` directories, no visible Node/cloudflared processes, and no readable
`/proc/net/tcp`. Consequently, neither the actual native production PID/tunnel
nor the number/address/owner of production listeners could be established. Do not
interpret this as proof that every native Termux process is stopped. Do not use
this executor's incomplete process view to authorize a stopped backup or launch.

| Evidence checked now | Outcome and limit |
| --- | --- |
| Native live `src/` files | Exactly match the approved M4.2 checkpoint; running version cannot be established |
| Deployed capability/auth/TLS source | Lobby `matches:false`, no Classic start/answer routes, schema-2 rejection and production HTTPS-header gate present; no live authenticated API check |
| Workspace backend source | Exactly matches the historical frozen M4.3 `src/` |
| Historical private backup | File hashes and receipt bindings valid; recorded schema 1, two identities, four jobs, six rooms; not a current snapshot |
| Historical staging receipt | All 87 checks true and bound to its retained inputs; not rerun now |
| Approved M4.2 checkpoint | Expected archive hash matches |
| M4.3 APK | Expected artifact hash matches; version 6 / 1.4-M4.3 installation is handoff evidence, not a new package-manager check |
| Historical private source/config copies | Current file hashes match their retained baseline; values/hashes not printed |
| Fresh preflight/context/rehearsal | **Not created/run:** endpoint/runtime checks failed |

Historical Android 106/106, backend 74/74, real-data rehearsal 87/87 and combined
operator 33/33 results remain historical. No test suite was rerun. No live SQLite
connection was opened, no production credentials were sent, and no provider was
called. The current live schema, identity/job/room counts and durable DB/WAL
equality were not established. Sanitized session evidence is in
[fresh-session review](validation/m4.3-fresh-session-review/summary.json).

**STOP operational work here.** Before resuming preparation, establish native
process visibility and a user-confirmed current origin with verified TLS, no
redirect, HTTP 200 and exactly `{"status":"ok"}`. Recovery requiring a backend or
tunnel restart needs separate approval. Never guess a hostname or regenerate the
tunnel to satisfy this runbook.

## 2. Future preparation: exact inputs and a fresh private context

Everything from this section onward is an **unexecuted future procedure**, gated
by section 1. Paths must come from a newly verified session, not remembered PIDs,
old `launch.pid`, old path receipts or historical deployment approval. Run in
native Termux as the existing service user, with shell tracing disabled:

```bash
set -eu
set -C
umask 077
export PYTHONDONTWRITEBYTECODE=1
QP_LIVE=/data/data/com.termux/files/home/quizpk-server
QP_DB="$QP_LIVE/data/quizpk.sqlite"
QP_NODE=/data/data/com.termux/files/usr/bin/node
QP_WORK=/mnt/sdcard/CodeOnTheGoProjects/QuizPK
QP_ROOT=/data/data/com.termux/files/home/.local/share/quizpk-backups
```

The following variable names describe **new private artifacts**, not existing
approved values: `QP_PRE` (online preflight), `QP_PREP` (release/context bundle),
`QP_OPS` (`QP_PREP/operations`), `QP_RELEASE` (`QP_PREP/release`), `QP_WINDOW`
(final stopped maintenance snapshot), and `QP_ORIGIN` (confirmed HTTPS origin).
Do not proceed with any unset variable or placeholder path.

1. Identify exactly one native backend by executable, absolute cwd, PID and
   `/proc` start identity. Privately compare its environment; report only
   nonsecret booleans. Require the actual absolute DB path above, loopback HOST,
   port 8787, production TLS enforcement, one matching listener and one observed
   SQLite owner. Inspect the existing cloudflared process without printing its
   arguments/environment. A failed or inaccessible observation closes the gate.
2. Inspect and hash the schema-aware M4.3 helpers before use. The old
   `m42_backend_backup.py` is incompatible with populated multiplayer databases.
   The historical frozen operations manifest has the public source hash
   `7e642b6f222e1ab7c87fe1708760ea8766654c3437d280409a466399e2dd150f`;
   if using that bootstrap copy, verify this anchor and every member first.
   A changed helper requires a new reviewed manifest and isolated safety tests.
3. Use `m43_backend_backup.py --label preflight` only with a verified running
   process. It uses SQLite's read-only online snapshot with committed WAL data,
   full schema/table equality, integrity, foreign keys and a separately restored
   standalone main file. It captures configuration privately and rechecks process
   identity. Read-only online SQLite may update transient SHM coordination; never
   copy SHM back. Online durable-byte equality is diagnostic during normal writes.
4. Make a new exclusive mode-0700 bundle under `QP_ROOT`, with mode-0600 files.
   Freeze only backend `package.json`, all regular `src/` files **including
   `src/multiplayer/classic-bank.json`**, backend test modules/fixtures, the common
   protocol JSON and reviewed operator helpers/tests. Refuse symlinks, extra files
   and existing destinations. There are currently 26 source/package/test files in
   the retained backend release; recompute the exact set rather than trusting a
   count. Confirm package/dependencies equal the live package.
5. Write NEW release, protocol, operations and context manifests. The private
   context must include the verified origin, exact Node executable/hash and exact
   revoked/current token-file paths/hashes. Never print context/runtime/backup
   manifests: they include private environment or credential-related hashes.
   Record trust anchors privately. Check every frozen file and the exact file set.
   Do not change the old context or edit any old hash, check or receipt in place.
6. From the exact release, run `node --test test/*.test.mjs`; from the exact
   operations copy run the combined M4.3/M4.2 safety suites on synthetic data.
   Use protected temporary directories, independent DB paths and ephemeral
   loopback ports. No inherited real provider configuration may reach a test
   entrypoint. Keep raw output private and publish only counts/booleans.

The reviewed `m43_backend_smoke.py --health-only` makes no authenticated room
request. Its current implementation explicitly disables environment proxies; only
use it in a verified native direct-network Termux environment. Where the executor
requires an injected network proxy, preserve that proxy and CA trust and prepare
a reviewed equivalent health check rather than bypassing network policy. Full
smoke is deferred to the authorized post-start window: authenticated room GETs
perform normal rate/presence/cleanup writes and are not read-only preparation.

### Isolated real-data rehearsal gates

After freezing the bundle, use the schema-aware rehearsal with a different DB
path beneath its exclusive private staging directory:

```bash
"$QP_NODE" "$QP_OPS/m43_backend_rehearsal.mjs" \
  "$QP_PRE" "$QP_PREP" "$QP_REVOKED_FILE" "$QP_CURRENT_RECOVERY_FILE" \
  > "$QP_PRE/rehearsal-output.json" 2> "$QP_PRE/rehearsal-private.log"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_PRE" "$QP_PREP"
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP" --health-only
```

Both token variables contain **paths only**, never credential values. Before the
new rotation, the historical revoked credential can establish only its earlier
revocation; the currently screenshotted credential remains compromised and active.
Do not label that historical rejection as rejection of the latest exposure. A
pre-rotation rehearsal is preparatory evidence only and cannot clear the rotation
gate. After rotation, regenerate the context and rehearse with the newly revoked
credential and fresh replacement, as in section 3.

Require all original users/jobs/throttle/rooms/members/receipts/limits and schema
definitions unchanged immediately after migration, except exactly one appended
version-2 ledger row; enabled identities, `user_version`, integrity and foreign
keys must match. Assert both clients share question/options/deadline; host-only
start; readiness; one locked answer; exact retry/conflicting-answer rejection;
wrong-first versus fastest-correct; 100–150 correct points; timeout; answer secrecy
in snapshots/receipts before reveal; automatic progression/final results;
reconnection and restart interruption; separate Moderator lobby. Replay old M3
HTTP cache entries using a rejecting provider and require zero provider calls.

On a separate schema-2 copy, run M4.3 with `QUIZPK_CLASSIC_ENABLED=false` and the
actual production entrypoint: old/new saved jobs, enabled identities, accepted
answers, revealed totals, existing lobbies and recoverable results persist; new
starts fail. Run the **unmodified verified deployed M4.2 source** on another
schema-2 copy and require `UNSUPPORTED_ROOM_SCHEMA`. This demonstration was
historically recorded and must be rerun against the fresh inputs. Never lower the
ledger, drop tables or rewind a DB to make old code start. A failing read-only or
staging check stops work; retain the failed directory and evidence.

## 3. Proposed new phone01 rotation — separate approval, NOT performed

The latest screenshot exposed the replacement phone01 token. The previous
rotation does not address this new exposure. Keep the owner ID and token unchanged
and preserve phone01's existing ID, enabled state and ownership of all saved data.

**Implementation blocker:** `tools/m42_rotate_phone_token.py` explicitly refuses
any `mp_%` schema (`EXPECTED_M3`). Its old tests use M3-only fixtures. Do not run
that helper on this database, delete its check, provision a substitute user, or
claim its previous tests validate a multiplayer rotation. No schema-aware
rotation helper was implemented or tested in this blocked session. Before
authorizing rotation, prepare and review one separately with populated schema-1
and schema-2 synthetic fixtures, stopped-owner/listener guards, stale-backup
rejection and file-failure-after-commit recovery. There is intentionally no
executable live rotation command in this runbook until that helper exists and
passes those tests.

The proposed order for the future reviewed helper is:

1. Verify the current endpoint/runtime and a new online preflight/context. Arrange
   idle players/no pending provider work. Under explicit shutdown/rotation
   approval, stop only the freshly guarded PID (section 4), keep the tunnel, and
   take a **new stopped main+WAL backup** with all five checks true. Keep this
   pre-rotation recovery point private and never restore its credentials to serve.
2. Require the same absolute DB, no owner/listener, exact current source/config
   and stopped durable bytes. Privately retain the latest exposed token in the
   stopped backup's `host/private/phone01.token`; never move it to shared storage.
3. Generate a new 256-bit random token in an exclusive private recovery file;
   flush file and directory before DB commit. Use `mode=rw` on the existing DB,
   `synchronous=FULL` and `BEGIN IMMEDIATE`. Compare all original table rows,
   schemas, ledger and `user_version` to the verified snapshot, with no migration
   import. Require exactly one enabled phone01 matching the existing token file.
4. Atomically update only phone01's credential hash, guarded by ID, enabled state
   and old hash. Before commit require every other users field/row and every
   jobs/throttle/multiplayer row unchanged, integrity/foreign keys valid, new token
   resolving to the same phone01 ID, and the just-exposed token resolving to no
   enabled identity. Report only booleans. Roll back if any assertion fails.
5. After committing, atomically replace only `private/phone01.token` from an
   exclusive 0600 temporary file, fsync the directory and verify private equality.
   DB and filesystem cannot commit as one transaction: if file installation
   fails after DB commit, keep the backend stopped and finish installation from
   the durable new recovery file. **Never reactivate the exposed token.**
6. Take a NEW post-rotation stopped schema-aware backup, including the new private
   file. Build a NEW context referencing the latest revoked token's pre-rotation
   backup path and fresh replacement recovery path. Reuse source bytes only after
   rechecking their hashes; regenerate all context-bound preflight/rehearsal,
   verification and launch receipts. Pre-rotation source-only hashes may still
   match, but previous credential/config inventories, backup bindings and
   context/rehearsal approvals are stale. Retain them unchanged as evidence.
7. On isolated copies prove the latest exposed token gets HTTP 401, the new token
   authenticates as the same phone01 ID, the owner credential remains valid, all
   original rows are preserved except the intentional hash change, cached M3
   responses replay with zero provider calls, and Classic/disabled fallback pass.
   Use the fresh stopped backup as `QP_WINDOW`. After separately approved startup
   repeat TLS/auth checks. Privately update only phone01's saved Android connection
   on the verified origin and test reconnection to the same identity/room. No
   reinstall, data clearing, screenshots of tokens or owner connection edits.

Rotation is not automatically authorized by deployment approval. If approved as
a separate window, restoring M4.2 service after rotation also needs a fresh
post-rotation schema-1 backup, context and bound rehearsal before guarded launch;
capture a new online runtime/preflight when preparing the later deployment.

## 4. Future guarded shutdown and final stopped snapshot

Only after endpoint/runtime verification, successful preparation and separate
approval for this exact maintenance window. These commands describe a healthy
running baseline; they are **inapplicable to the unresolved current executor**.

```bash
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_PRE" "$QP_PREP"
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP" --health-only
QP_PID="$(python3 "$QP_OPS/m43_backend_ops.py" pid "$QP_PRE" "$QP_PREP")"
kill -TERM "$QP_PID"
python3 "$QP_OPS/m43_backend_ops.py" wait-stopped "$QP_PRE" "$QP_PREP"
```

Require the same PID/start identity immediately before signalling; allow graceful
exit, no forced provider interruption, no supervisor restart, free port and no
observable SQLite owner. The wait helper waits up to 55 seconds and never signals
anything. Inaccessible `/proc`/socket information is a blocker, not proof of no
owners. Never use SIGKILL or signal npm/cloudflared/unrelated Node processes.

For the newly selected private output directory, refuse existing output files
including broken symlinks. Capture a new stopped snapshot with:

```bash
python3 "$QP_OPS/m43_backend_backup.py" --label maintenance \
  --runtime-from "$QP_PRE" > "$QP_PREP/stopped-backup-result.json"
QP_WINDOW="$(python3 - "$QP_PREP/stopped-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] is True for k in ['snapshotIntegrity','snapshotDataEqual',
 'standaloneRestoreEqual','configCopiesVerified','liveDurableBytesEqual'])
print(r['backupDirectory'])
PY
)"
```

Use an exclusive redirection (`set -C` in the operator shell, or equivalent
reviewed exclusive-file creation) so prior outputs are not overwritten. The
stopped path hashes/copies main **and any WAL** before opening only the private
copy; it refuses a journal, owner/listener or byte drift. Do not open stopped live
SQLite with `mode=ro`, use immutable mode to ignore WAL, delete sidecars or copy a
WAL over live. All five checks, especially `liveDurableBytesEqual`, are mandatory.

If rotation is approved, perform only the reviewed section-3 procedure now, then
replace `QP_WINDOW`/`QP_PREP` and all derived operations/release path variables
with the NEW post-rotation snapshot/context, not edits to existing artifacts.
With the latest revoked/replacement paths:

```bash
"$QP_NODE" "$QP_OPS/m43_backend_rehearsal.mjs" \
  "$QP_WINDOW" "$QP_PREP" "$QP_REVOKED_FILE" "$QP_CURRENT_RECOVERY_FILE" \
  > "$QP_WINDOW/rehearsal-output.json" 2> "$QP_WINDOW/rehearsal-private.log"
python3 "$QP_OPS/m43_backend_ops.py" verify \
  "$QP_WINDOW" "$QP_PREP" --stopped-durable
```

Require complete section-2 coverage, zero provider calls, exact source/package/
protocol/context/operator hashes and current rotated credentials. Normal online
activity since an earlier snapshot must be included in this fresh stopped
snapshot. Any later drift requires a new snapshot/rehearsal; never edit receipts.

## 5. Exact source swap and first production M4.3 launch

Review the final bundle and approve deployment separately. Derive unique sibling
names from the newly approved window; retain the actual M4.2 source. Both
destinations must be absent, including symlinks. Only `src/` is replaced:

```bash
QP_TAG="$(basename "$QP_WINDOW")"
QP_NEXT="$QP_LIVE/src.m43-next-$QP_TAG"
QP_OLD="$QP_LIVE/src.m42-before-m43-$QP_TAG"
test ! -e "$QP_NEXT" && test ! -L "$QP_NEXT"
test ! -e "$QP_OLD" && test ! -L "$QP_OLD"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --stopped-durable
cp -R "$QP_RELEASE/src" "$QP_NEXT"
python3 - "$QP_NEXT" "$QP_PREP/release-manifest.json" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1]);m=json.load(open(sys.argv[2]))
assert not any(f.is_symlink() for f in p.rglob('*'))
actual={str(f.relative_to(p)):hashlib.sha256(f.read_bytes()).hexdigest()
        for f in p.rglob('*') if f.is_file()}
assert actual=={n[4:]:h for n,h in m.items() if n.startswith('src/')}
assert 'multiplayer/classic-bank.json' in actual
print('PASS: exact reviewed source including Classic bank.')
PY
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --stopped-durable
mv "$QP_LIVE/src" "$QP_OLD"
if ! mv "$QP_NEXT" "$QP_LIVE/src"; then
  mv "$QP_OLD" "$QP_LIVE/src"
  exit 1
fi
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" \
  --source release --stopped-durable
nohup python3 "$QP_OPS/m43_backend_ops.py" launch "$QP_WINDOW" "$QP_PREP" \
  --mode enabled > "$QP_WINDOW/m43-start.log" 2>&1 < /dev/null &
```

Require exclusive private logs and unchanged private env, tokens, package and
absolute DB. Interrupted renames need verified source recovery before any launch.
Never copy staging DBs, env/token files, tests or Android files into production.

The launcher loads the captured existing environment privately and execs the
verified Node executable with the original absolute DB path,
`HOST=127.0.0.1`, `PORT=8787`, `NODE_ENV=production` and
`QUIZPK_CLASSIC_ENABLED=true`. **This first Store constructor is the first live
schema-1→schema-2 migration.** A background shell return is not startup success.
Do not print private startup logs or start a second instance on failure.

After successful startup, under the approved window:

```bash
python3 "$QP_OPS/m43_backend_ops.py" current-pid "$QP_WINDOW" "$QP_PREP" --source release
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP"
python3 "$QP_OPS/m43_backend_backup.py" --label preflight \
  > "$QP_WINDOW/post-start-backup-result.json"
```

Require verified TLS/no redirects/health 200, plain loopback HTTPS_REQUIRED,
missing and **latest revoked** bearer 401, replacement and unchanged owner
identities accepted, ordinary old-client lobby shape and new-client Classic
capability. If no current lobby exists the smoke skips that capability check;
mark it pending physical acceptance rather than claim it passed. Full smoke has
normal authenticated bookkeeping writes, which are permitted only in this future
approved window. Never generate an uncached Gemini request for validation.

Extract `QP_POST` from the fresh post-start backup result only after its four
online snapshot/restore/config checks are true, then:

```bash
python3 "$QP_OPS/m43_verify_snapshot.py" "$QP_WINDOW" "$QP_POST" "$QP_PREP"
```

Only private backups are opened: require schema 2, integrity/foreign keys,
unchanged `user_version`, all prior users/jobs and current credential validity.
Migration-time original room/member/receipt equality is established on the exact
stopped-copy rehearsal; running housekeeping may expire old room/receipt/limit
rows. Investigate unexpected changes and never rewind data to force equality.

## 6. Recovery decision: inspect a fresh private stopped copy

Stop only the verified current release PID if it still exists, then wait for
stopped/no-owner/free-port guards. If it exited, skip signalling; never use a
stale PID. Keep the tunnel and capture a NEW stopped main+WAL snapshot with all
five checks true, using `--runtime-from "$QP_WINDOW"`. Verify schema from this
private snapshot, not from a partial log or a direct live SQLite open.

### Before schema 2 commits

M4.2 recovery is possible **only when the fresh current snapshot proves schema 1**,
original schema/rows preserved and source/config/durable gates pass. Retained
`QP_OLD` must exactly equal the original verified M4.2 source in
`QP_WINDOW/host/src`. If schema/version/commit state is ambiguous, remain stopped.

Preserve rejected M4.3 source under a new absent private sibling and restore only
the verified M4.2 source. Retain the **current DB and current rotated token**.
Take another NEW stopped backup after restoring M4.2 source: a failure backup
captured while M4.3 source was installed is not a M4.2 baseline manifest. Rehearse
that new schema-1 snapshot against the exact frozen release/context and its
captured unmodified M4.2 source. Only after its own fresh bound receipt and strict
stopped-byte gate pass may the reviewed recovery command be used:

```bash
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_RECOVERY" "$QP_PREP" --stopped-durable
nohup python3 "$QP_OPS/m43_backend_ops.py" launch "$QP_RECOVERY" "$QP_PREP" \
  --mode m42-recovery > "$QP_RECOVERY/m42-recovery-start.log" 2>&1 < /dev/null &
```

`QP_RECOVERY` is the new verified post-source-restoration stopped snapshot, not
the online preflight or failed launch backup. Verify HTTPS/auth/cache/lobbies and
latest-token rejection after recovery. If staging itself failed, do not invent an
unchecked automatic recovery receipt; keep stopped pending a reviewed decision.

### After schema 2 commits

Keep the current DB and exact M4.3 source. With the fresh stopped schema-2 snapshot
as `QP_FALLBACK` and original successful schema-1 window as `QP_WINDOW`:

```bash
python3 "$QP_OPS/m43_verify_snapshot.py" "$QP_WINDOW" "$QP_FALLBACK" "$QP_PREP"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_FALLBACK" "$QP_PREP" \
  --source release --stopped-durable --reference "$QP_WINDOW"
nohup python3 "$QP_OPS/m43_backend_ops.py" launch "$QP_FALLBACK" "$QP_PREP" \
  --mode disabled --reference "$QP_WINDOW" \
  > "$QP_FALLBACK/fallback-start.log" 2>&1 < /dev/null &
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP" --fallback
```

Wait for startup before smoke. The new launch keeps production HTTPS, loopback,
the same absolute DB and `QUIZPK_CLASSIC_ENABLED=false`. Existing in-flight
matches finish INTERRUPTED; ungraded answers remain accepted audit rows with no
new grades or leaked keys. Previous revealed scores, AI jobs, identities, existing
lobbies and final results remain. Capture a new online post-fallback backup,
require its four checks and compare it with `QP_FALLBACK` using
`m43_verify_snapshot.py` to preserve existing answer/key/player records as well.
Keep phones idle during this comparison; investigate legitimate concurrent drift.

If M4.3 cannot start even with Classic disabled, keep service stopped for a
schema-2-compatible repair. No old source, ledger edit, table removal, WAL
overwrite, automatic DB restore or pre-rotation credential recovery is a normal
fallback. An exceptional data restore needs a separate loss-window and credential
revocation review, with all current files/evidence retained.

## 7. Immediate stop conditions and remaining approvals

Stop on unhealthy/unconfirmed/stale origin; TLS or redirect failure; incomplete
native process/listener/owner visibility; unexpected PID/start/executable;
occupied/non-loopback/duplicate listener; wrong/empty DB; source/package/protocol/
operator/context/config/token drift; symlink or reused destination; missing/failed
backup check; stopped main/WAL drift; integrity/foreign-key/data mismatch;
unreviewed multiplayer rotation helper; failed rotation or rehearsal; unknown
migration state; failed auth/post-start/fallback checks. Preserve failed artifacts.
No bypasses, patched receipts, stale approvals or destructive cleanup.

Current blockers: unhealthy supplied HTTPS origin; unverified native runtime;
no fresh online preflight/current-origin context/rehearsal; latest phone01 token
still needs revocation; multiplayer-aware rotation helper and tests absent.
Approvals still required for any live recovery/restart/tunnel action, the new
phone01 rotation, exact M4.3 deployment/maintenance window, and physical testing
that changes a server process or installs an APK on another phone. None is granted
by this preparation document.

## 8. Follow-on two-phone acceptance

After separately reviewed deployment, follow
[M4_3_DEVICE_TEST_PLAN.md](M4_3_DEVICE_TEST_PLAN.md). The handoff says M4.3 version 6
is already installed on the user's phone: do not rebuild/reinstall it to address
an M4.2 server's absent Start capability. Check the second phone's installed
version/signer before proposing an update; any needed install requires separate
approval and must preserve app data.

Use two distinct identities for create/join/ready/start, shared questions/timers,
locked answers, wrong-first/fastest-correct, timeout, reveal, complete results,
disconnect/retry/background/rotation, final-result recovery, M1 themes/navigation,
M2 history and M3 saved offline replay. Record device/OS/network/app/backend
versions without tokens or invite codes. Backend interruption testing needs its
own approved window, preferably an isolated test backend. Physical M4.3 gameplay
is pending; simulated clients cannot certify it. No M4.4, Moderator gameplay,
LAN or monetization work is part of this runbook.

## 9. Deferred M4.3.1 — Connection & Guest Onboarding (design only)

Dependency: M4.3 live deployment and physical gameplay acceptance, followed by a
stable controlled HTTPS origin using a properly configured named tunnel/domain
or persistent hosting. Bundle that verified origin first; never hardcode a
temporary Quick Tunnel. Optional signed remote configuration needs a separate
threat model. Unauthenticated discovery must not redirect credentials to arbitrary
hosts; reject wrong/stale origins and certificate/redirect failures.

Proposed flow: friendly display-name screen → Continue as Guest → TLS registration
→ one server-issued random scoped identity/credential per installation → Android
Keystore-protected storage and automatic reconnection. Keep manual endpoint/token
entry behind an intentional operator/development setting. Legacy tester settings
and saved AI/offline data need a planned compatible transition.

Backlog: design registration/refresh/revocation APIs with server-owned roles,
quotas, per-IP/network/device risk controls and anti-automation defenses. Never
trust names, client device IDs, embedded APK secrets or client-selected roles as
identity. Minimize personal data; define expiry/rotation, stolen-device revocation,
durable account linking and reinstall/new-device behavior (guest progress may be
lost without linking). Do not distribute owner/phone01, Gemini or admin
credentials through APKs, remote config, QR links, screenshots or a public
pre-created-token dispenser. Signup abuse, token theft and hostile endpoint
redirection are the principal security risks; preserve server scoring/authority.

Acceptance backlog: first install; independent second device; stale/wrong origin;
TLS failure; offline mode; reconnect; expiry/revocation/rotation; duplicate
registration; throttling; reinstall/new device; migration of existing settings/
saved AI data; and secret-free logs/APKs. No onboarding code, live guests,
authentication weakening, hosting purchase or deployment was performed here.

**STOP for user review. Current preparation is blocked, not deployment-ready.**
