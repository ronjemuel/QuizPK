> Preparation update (2026-10-13): the fresh online backup and real-data rehearsal
> are recorded in [M4_3_DEPLOYMENT_RUNBOOK.md](M4_3_DEPLOYMENT_RUNBOOK.md).
> Its frozen schema-aware M4.3 helpers supersede the M3-only backup command below.
> The historical implementation plan follows. No live deployment has occurred;
> the previous HTTPS hostname does not resolve and remains a deployment gate.

# M4.3 Termux deployment and fallback plan — NOT EXECUTED

Only local implementation and isolated automated databases are authorized now.
No live backup, migration, deployment, restart, token provisioning/rotation or APK
installation was performed for M4.3. The operator's working M4.2 installation is
the baseline; earlier M3 deployment/recovery documents describe historical work.
Obtain separate approval for a concrete current maintenance window before running
any live-host steps below. Never reuse old approval receipts for this release.

## Change and rollback decision

The new backend adds schema ledger version 2 and five separate match tables plus
indexes, retaining every M3/v1 table and `user_version`. Start-up is the migration
trigger. Atomic migration and failure rollback are tested on populated v1 fixtures.
All questions/keys ship in `src/multiplayer/classic-bank.json`; deploy the entire
reviewed `src/`, including JSON. `package.json` and dependencies are unchanged.
Node 24 built-in SQLite, loopback port 8787 and the existing HTTPS proxy remain.

**The unmodified M4.2 migration rejects version 2.** Restoring only old source
after a successful upgrade will not start. Preferred fallback retains the current
M4.3 source/database and launches with `QUIZPK_CLASSIC_ENABLED=false`: ordinary
Classic/Moderator lobbies and M1–M3 backend behavior remain, new matches are disabled,
and any in-flight match is finalized as interrupted without unrevealed scoring.
This path is automated-tested, including preservation of accepted answer records.
Restoring an older DB would lose newer AI jobs, game records and possibly current
credentials; it is never an automatic rollback action. Do not lower `mp_schema`,
drop match tables, restore pre-rotation credentials or overwrite a WAL/main file.

## Prepare a fresh, private maintenance window after approval

1. Privately identify the actual `~/quizpk-server` process, Node executable,
   DB path, startup environment and proxy. Confirm one process and the existing
   loopback/HTTPS setup. Never print process arguments/environment or secrets.
   Keep the existing tunnel running; a Quick Tunnel hostname may change on restart.
2. Freeze reviewed source and source hashes in a new private release directory
   outside shared storage, excluding env/token/DB/signing files. Run `npm test`
   on this exact release source/test copy and preserve its results. Verify its
   `package.json` hash matches the live one. Retain the approved M4.2 checkpoint
   and actual deployed source separately; they are not assumed identical.
3. Use the corrected private backup helper to capture the current running runtime
   and a verified initial snapshot. Then gracefully stop only the identified
   backend through its established launch method, waiting for in-flight work and
   checking there is no automatic restart. Do not force-kill a provider job or
   stop unrelated Node/tunnel processes. Plan game interruption with participants.
4. With that freshly captured runtime, create a **new stopped maintenance backup**:

   ```bash
   umask 077
   python3 tools/m42_backend_backup.py --label preflight
   # Privately record the new verified preflight directory, then gracefully stop.
   python3 tools/m42_backend_backup.py --label maintenance --runtime-from "$QP_PREFLIGHT"
   ```

   `QP_PREFLIGHT` must be the new current preflight path, never the historical M3
   window. Privately record the new maintenance directory as `QP_BACKUP`. Require
   all five backup checks true, including strict `liveDurableBytesEqual`. The
   corrected stopped path hashes/copies main plus WAL and opens only the private
   copy; it does not open stopped live SQLite or manufacture sidecars. Verify all
   backup hashes, standalone restore, current rotated credentials and source/config
   copies. Any failed/missing check stops the procedure; retain failed evidence.

## Staging gate before any live Store constructor or source replacement

5. Create a new mode-0700 staging directory outside live/shared storage. Copy only
   the verified portable `QP_BACKUP/quizpk.sqlite` into staging with mode 0600;
   leave the backup immutable. Copy the frozen new source/package into a separate
   staging release. Use an explicit **different** SQLite path and kernel-assigned
   loopback port (0) for all tests. Inject a rejecting provider; do not call Gemini.
6. Before migration, privately record all users/jobs/throttle, room/member/receipt/
   quota rows, table definitions and `user_version`. Start the staged `Store` once
   on that copy, then compare every existing row/schema and enabled token identity
   by booleans only. Require schema 2, integrity/foreign-key checks and full-row
   equality immediately after migration. Existing credentials are reused; never
   rerun create-user to “repair” an empty/wrong-path DB.
7. Run two isolated staging identities through Classic create/join/ready, host-only
   start, same questions/deadlines, wrong-first/fastest-correct, duplicate answers,
   timeout/reveal/automatic finish, reconnect and restart. Verify all contestant
   responses lack unrevealed keys and pending grades. Recheck M3 cached response
   replay/authentication without invoking the provider. Actual tokens stay in
   private memory/header or token files, never shell arguments/output.
8. On a further staging copy, restart the new source with Classic disabled. Require
   preserved old/new saved AI jobs, identities, game records and final results,
   functioning old lobby shape/Moderator separation, and new Start denied. Also
   prove old unmodified M4.2 source refuses schema 2. This prevents relying on an
   unsafe source downgrade. Do not run the old M4.2 M3-only rehearsal/launcher on
   the new release: its version-1 assumptions/receipt bindings are incompatible.
9. Bind the staged migration/game/fallback evidence to exact backup and frozen
   release manifests. Review it before deployment. Recheck stopped process/port/
   SQLite-owner status, exact live main/WAL bytes against the stopped backup,
   config/token/source hashes and release hashes immediately before replacement.
   Any drift requires a new backup/rehearsal, not a patched receipt or bypass.

This M4.3 implementation does **not** claim that steps 3–9 have run against the
current live-data copy. Automated fixtures prove the migration/game/fallback
semantics; the current private-data rehearsal remains a deployment gate.

## Separately approved deployment and post-check

10. While stopped, copy the verified frozen `src/` to a fresh sibling such as
    `src.m43-next`; refuse if that or the recovery directory already exists.
    Verify every copied file against the reviewed release manifest. Retain the
    actual old `src/` as `src.m42-before-m43`, then rename the new directory into
    place. Resolve any interrupted rename from retained source before a launch.
    Keep live env/private/data/package files exactly where they are. Never copy a
    staging database, token, env or Android file over the host.
11. Launch once from the established Termux method, reusing the freshly captured
    private environment and absolute live DB path, with `HOST=127.0.0.1`,
    `PORT=8787`, `NODE_ENV=production`, `QUIZPK_CLASSIC_ENABLED=true`. This is the
    **first live migration** and requires deployment approval. Load captured
    secrets in memory/private startup configuration, without shell tracing,
    command substitution, printing env, or provider keys in arguments.
12. Require HTTPS health 200, no-bearer room request 401, plain loopback without
    HTTPS proxy proof rejected, existing tokens valid and saved M3 jobs preserved.
    Recheck ordinary lobby behavior for older clients and Classic capability for
    the new APK. Complete the [two-device plan](M4_3_DEVICE_TEST_PLAN.md). Public
    HTTPS/physical gameplay acceptance remains pending until these pass.

## Fallback without data rewind

If launch fails before migration commits, stop the failed process, verify it is
stopped, and retain diagnostics privately. Inspect a **copy** of the current DB to
determine whether schema 2 committed. If not, restoring retained source is possible
after full hash/path checks. Never guess from a partially written log.

After schema 2 commits, retain current DB and new source. Gracefully stop only this
backend, recheck port/owners/config/source, then launch the same reviewed version
with `QUIZPK_CLASSIC_ENABLED=false` and the same production HTTPS/environment/DB
settings. New matches are disabled; in-flight matches finish interrupted, prior
revealed scores and M3 data remain. Verify authentication, cached AI data, ordinary
lobbies and recoverable final results. If the new source itself cannot run, keep
the service stopped pending a schema-2-compatible repair; never force a downgrade.

Termux background termination, phone/storage loss, temporary tunnel availability,
network latency and private backup media are material limitations. The on-phone
backup does not protect against losing the phone. No production availability or
physical M4.3 acceptance is claimed by the automated tests.
