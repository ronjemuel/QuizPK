# M4.2 deployment audit — preparation passed; live deployment NOT executed

**Current status after maintenance failure:** backend stopped, exposed phone01
token rotated, no M4 deployment. The failed byte check was a read-only-created
empty WAL; data backup remained valid. See [recovery and refreshed procedure](M4_2_MAINTENANCE_RECOVERY.md)
for the new post-rotation backup and 59-check rehearsal. Older paths/tokens and
the prior running-server status below are historical, not restart authorization.

2026-10-13. Conditional approval covered private backup and isolated rehearsal.
The working `~/quizpk-server` continues to run M3. Its source, provider configuration
and two individual tokens remain byte-identical. Normal live activity added one
job during preparation; all original users/jobs remain intact, and a newer backup
includes that activity. No live migration/restart, initiated provider call, provisioning,
tunnel change, Android update or M4.3 work occurred. Read-only SQLite backup may
update transient `-shm` coordination; it is never restored over a running DB.

**Stop here for review.** Use the [exact maintenance/rollback runbook](M4_2_DEPLOYMENT_RUNBOOK.md)
after review. The historical general plan below is superseded where it differs.

## Verified private recovery point

Verified backup:
`/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T023118Z-preflight`.
Directories are 0700; files are 0600, inside Termux internal storage and outside
shared storage/source checkpoints. Earlier private snapshots are retained too.
Do not upload these directories.

It contains a consistent SQLite snapshot including committed WAL contents, exact
M3 source/package, provider env, both token files, captured backend environment
(including inherited model/free-tier settings), tunnel launch configuration,
private hashes/invariants and an independently restored DB copy. Files/directories
were flushed to storage. Integrity, foreign-key, schema, full-row equality and
standalone restore checks pass. No credentials, token hashes or DB rows are
published. The portable snapshot uses DELETE journal mode; the server switches
its own restored copy to WAL normally.

The online backup avoids a DB-only file copy while the writer is active, using
[Python's backup API](https://docs.python.org/3/library/sqlite3.html#sqlite3.Connection.backup)
and [SQLite's online backup](https://www.sqlite.org/backup.html). Only the source
is opened read-only with a read snapshot. No live Store constructor, checkpoint,
journal-mode change or write statement is used for backup.

## Audit findings

| Area | Result and deployment treatment |
| --- | --- |
| Data loss | Complete users/jobs/throttle rows and schemas remain equal after migration/lobby use. Keep the current live DB during deployment and normal rollback. Fresh maintenance backup/rehearsal protects subsequent writes. Never copy workspace/staging data, env or tokens into live. |
| Tokens | Both existing distinct tokens resolve to their original enabled identities in M4.1 staging and rolled-back M3; SHA-256 authentication is unchanged. No re-provisioning/rotation is needed. All four current cached responses replay, including the three original ones; a synthetic new saved response survives rollback/re-upgrade. |
| SQLite | One additive transaction creates mp_schema version 1, mp_rooms, mp_members, mp_requests, mp_limits and indexes, preserving M3 user_version. A deliberate table collision rolls back new DDL/ledger without changing M3 rows. Store startup is the migration trigger. Source rollback keeps additive tables/rows. |
| HTTPS | Actual live bind is 127.0.0.1:8787 behind a direct Cloudflare Quick Tunnel. **NODE_ENV is unset**, so backend HTTPS-header enforcement is currently inactive. Public HTTPS health/certificate validation passes. Production staging rejects missing/http X-Forwarded-Proto and missing bearer; both existing tokens pass. Proposed launch sets NODE_ENV=production; public room/auth smoke remains a post-approval gate. |
| Termux | Actual Node v24.18.0, built-in SQLite, zero new dependencies. Actual production entrypoint migrated another copied DB on kernel-assigned loopback port 0 and shut down via SIGTERM. Actual live M3 is manual npm start from a terminal; no backend supervisor was identified. No systemd/pm2 or broad Node/tunnel stop is appropriate. |
| Rollback | Actual backed-up M3 source runs on migrated staging, retaining original tokens, multiplayer rows and old/new cached responses. Preferred rollback restores only source and captured startup environment. DB rewind is exceptional, loses subsequent writes and must never be automatic. |

Cloudflare documents that it overwrites X-Forwarded-Proto with the visitor protocol.
Production mode should work through this tunnel, an inference until the deployed
room smoke passes. Loopback remains the local-proxy trust boundary. See
[Cloudflare HTTP headers](https://developers.cloudflare.com/fundamentals/reference/http-headers/#x-forwarded-proto).

## Verification and remaining gates

Evidence is under `docs/validation/m4.2-deployment-preflight/`:

- **54/54 backend regressions**, including capacity, concurrency, expiry, quotas,
  revocation and all existing M3 provider/API tests with mocks.
- **55/55 actual-snapshot rehearsal checks**: preservation, existing-token HTTP
  authentication, Classic/Moderator/ready/settings/close/retry, restart/reconnect,
  source rollback/new-response preservation/re-upgrade, migration failure and real
  Termux production entrypoint. **Zero provider calls**.
- **8/8 synthetic operator safety tests**, with exec mocked: absent DB, occupied
  port, failed rehearsal, private/source drift, captured environment, and committed
  WAL versus uncommitted data. Two initial private-directory fixture failures are
  retained; explicit 0700 setup and resolved paths fix the test without relaxing
  the private guard.
- Private permissions/hashes, public HTTPS health and live/source/checkpoint/app
  preservation checked separately. Private copied DBs are not release payloads.

Staging is `.../20261013T023118Z-preflight-staging/`. Its `m41/` contains reviewed
backend source and identical package.json only. Frozen `operations/` helpers and
hash manifest accompany the runbook; production backend code remains unchanged.

Risks/gates: production enforcement changes the actual unset NODE_ENV and must
pass public HTTPS room/auth smoke; Quick Tunnel hostname can change on restart, so
keep it running; Android can terminate background Termux despite nohup; graceful
shutdown may wait for an in-flight provider request, never force-kill it; any
process/source/config drift needs a new preflight; fresh maintenance backup and
rehearsal are required before source replacement. Backups share the phone's disk
and do not protect against phone/storage loss. Source rollback restores the old
unset NODE_ENV and its prior header-enforcement limitation. Physical two-phone
acceptance, live matches and LAN remain unverified/deferred. Installed M3 Android
and saved app data remain untouched. See the [device plan](M4_2_DEVICE_TEST_PLAN.md).

---

# Historical initial M4.2 migration/deployment plan — NOT EXECUTED

The Android lobby client requires the approved workspace M4.1 backend. The live
Termux installation still has M3 source/schema. M4.2 changed no production backend
file or SQL migration and did not restart the server, tunnel or proxy. An older
host shows “This server does not support these online rooms yet”; AI Quiz continues
to use its existing endpoints. Deployment requires separate authorization.

## Prepare and review

1. Identify the running service's working directory, Node 24 executable, absolute
   DB path, private env/token paths and process supervisor privately. Retain the
   existing `127.0.0.1:8787` bind, HTTPS origin, proxy trust and Caddy/cloudflared
   configuration. Do not print environment values or process arguments.
2. Run `npm test` against the workspace backend and stage its source in a separate
   private directory. Do not copy workspace `data/`, `private/`, env files or
   signing material into the host. There are no new runtime dependencies.
3. Schedule a short maintenance window. Stop only the identified backend through
   its existing supervisor and verify it cannot auto-restart during backup. Do not
   replace or initialize its DB. Create a private recoverable backup of the actual
   stopped DB and any WAL/journal sidecars, existing server source and configuration.
   Use restrictive permissions outside shared storage; verify hashes privately and
   restore a copy into an isolated staging directory. Keep live credentials separate
   from distributable source archives. Never delete a live WAL or copy only the main
   SQLite file while a writer is running.
4. On that disposable restored copy, capture counts/invariants for existing
   `users`, `jobs`, `throttle` and `PRAGMA user_version` without publishing rows,
   identity IDs or hashes. Start the staged M4.1 backend with an explicit staging DB
   path and a different loopback port; disable provider use for this rehearsal.
   `Store` applies additive `mp_schema` version 1, `mp_rooms`, `mp_members`,
   `mp_requests`, `mp_limits` and their indexes transactionally. Confirm the M3
   invariants and saved job content remain equal, and existing tokens are enabled.
   Test two isolated staging identities through create/join/ready/reconnect/close.
   Remove staging output privately when finished; do not export its private DB.

## Approved deployment window

1. With the live service stopped and its verified private backup available, replace
   only reviewed backend source, retaining the actual env file, token files, DB,
   service user, paths and proxy configuration. Start once using the existing
   absolute `QUIZPK_DB_PATH`; the same additive migration runs on that DB.
2. Check health and the read-only `check-room-db.mjs` with each already-provisioned
   user's token file. Its boolean-only output must show the expected existing DB,
   identity schema, room schema and enabled identity. Do not provision replacement
   users simply because a wrong DB path produced an empty store.
3. Verify existing M3 saved-job retrieval and AI endpoint authentication, then the
   [two-device lobby plan](M4_2_DEVICE_TEST_PLAN.md) over the unchanged public HTTPS
   edge. AI generation is an optional separately intended provider action, not a
   prerequisite for lobby smoke tests. Review sanitized logs without request bodies,
   Authorization headers, invitation codes, provider responses or private DB rows.

## Recovery

If source deployment fails before migration, stop the service and restore the
previous source/configuration. If migration succeeded, older M3 source can ignore
the additive multiplayer tables; rehearse this on the disposable copy before the
window. Retain the migrated DB so newer saved jobs are not lost. A full DB restore
is an exceptional operator decision with the writer stopped and an explicit loss
window; never rewind the live DB automatically or merge/erase multiplayer tables
to “undo” this stage. Keep the verified pre-migration backup private.

This document is a plan for review. No step targeting the live host was executed
during M4.2, and no deployment is bundled into Android installation.
