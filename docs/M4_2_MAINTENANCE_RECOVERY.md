# M4.2 maintenance failure recovery — stopped for review

2026-10-13. **The live M3 backend remains stopped. No M4 source deployment, live
migration, DB-file replacement/restore or Android change occurred.** The only live
SQL write was the explicitly requested phone01 credential rotation. No provider
request was made. Isolated HTTP/SQLite staging ran on private copies and ephemeral
loopback ports; nothing listened on the live 8787 port.

This supersedes the previous runbook's pre-rotation paths and Step 3. Do not resume
the old commands or change a failed check to true.

## Exact mismatch and existing-backup status

The failed backup is `~/.local/share/quizpk-backups/20261013T031519Z-maintenance`.
Its private manifest records **main DB hash identical before/after**, WAL absent
before and a **zero-byte WAL** afterward. The current main at investigation was
still byte-equal to that captured main. Read-only immutable diagnostics, restricted
to this verified stopped/empty-WAL state, found equal users/jobs/throttle rows,
unchanged user_version, valid integrity/foreign keys and no multiplayer schema.
No database owner or 8787 listener existed.

The previous helper opened the stopped WAL-mode live DB using mode=ro. SQLite
created an empty WAL and shared-memory coordination file. Its dictionary of file
hashes then gained a WAL entry, making liveDurableBytesEqual false without changing
committed data. A disposable synthetic DB reproduced exactly that behavior. The
stopped-backup helper caused this false alarm; the strict guard correctly stopped
Step 3. See [SQLite's read-only WAL behavior](https://sqlite.org/wal.html#read_only_databases).

The failed backup's hashes, integrity, standalone restore and full-row content
all verify: **it is usable for data recovery**. It is not a deployment authorization
and its pre-rotation credentials/users row must never be reactivated. Keep it
private as evidence. No old backup was restored and no old manifest was edited.
The old approved-window.path and maintenance-rehearsal.json remain absent.

## Credential rotation completed

The current creation CLI uses exclusive token-file creation and INSERT, not a
credential rotation. At diagnosis, phone01's file and enabled DB credential still
matched the original preflight; the rerun had not replaced them. No extra identity
or job loss was found: two users and four saved jobs remain.

The requested rotation generated a new 256-bit token privately, flushed a recovery
copy, then transactionally changed **only phone01.token_hash**. The user ID and
enabled state were retained, so saved-job ownership remains intact. The old token
no longer matches any enabled identity; the replacement authenticates phone01.
The token file was replaced atomically with 0600 permissions and directory fsync.
Other users, all four complete saved-job rows, throttle, schema and user_version
were compared unchanged. No credential value or credential hash was printed.

Replacement: `~/quizpk-server/private/phone01.token`. Transfer it privately to
phone01's saved connection only after a separately approved backend restart/test;
never paste it into chat, logs, shell arguments or screenshots. No Android
connection was edited here. Owner's credential remains valid and unchanged.
Rotation's durable private recovery copy/results are at
`~/.local/share/quizpk-backups/20261013T032648Z-phone01-rotation`.
Do not rerun the creation or rotation script to resume deployment.

## Refreshed verified recovery point and rehearsal

New post-rotation backup:
`~/.local/share/quizpk-backups/20261013T033128Z-maintenance`.
All **five** backup checks pass, including strict liveDurableBytesEqual=true.
Stopped backup now verifies no server/listener/observable DB owner, copies the
quiescent main **and any WAL** privately, verifies both copies and unchanged source
hashes, and runs SQLite backup/restore verification only on that private copy.
It never opens stopped live SQLite, ignores a nonempty WAL, deletes sidecars or
relaxes equality. A journal file or any source drift fails closed. Online backup
still uses SQLite's read snapshot. Both paths retain restrictive permissions.

New staging: the backup path plus `-staging`. **59/59** checks pass, including the
original 55, new phone01 authentication under the same identity, exposed-token
401/rejection in M4.1, and rejection after actual M3 source rollback. All four
saved responses replay; a synthetic newer response survives source rollback and
re-upgrade. Provider calls: zero. Actual Termux production entrypoint was tested
only against another private DB/ephemeral port and then terminated.

The receipt now binds the exact backup and release manifests and staging paths.
Stopped/deployment guards also check current durable DB/WAL against this backup;
--require-revocation requires the additional token-rejection evidence. Production
launch retains that proof, free-port and credential/source gates. Any DB/config/
source change requires another backup/rehearsal; never patch a receipt or remove
a check to proceed.

Regression results: **54 backend tests and 17 operator safety tests pass**. Safety
tests cover absent/empty/nonempty orphan WAL, no live sidecar creation, stale DB/
receipt/revocation proofs, and token-file failure after commit without re-enabling
the exposed key. Android files/APKs and all M3/M4 review checkpoints are retained.
Evidence: `docs/validation/m4.2-maintenance-recovery/`.

## Safe next commands — verification only, no server start or deployment

Run in native Termux. Do not enable shell tracing or print private JSON/config.

```bash
set -eu
umask 077
QP_LIVE=/data/data/com.termux/files/home/quizpk-server
QP_WINDOW=/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T033128Z-maintenance
QP_STAGE="$QP_WINDOW-staging"
QP_RELEASE="$QP_STAGE/m41"
QP_OPS="$QP_STAGE/operations"
python3 - "$QP_OPS" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1]);m=json.loads((p/'operations-manifest.json').read_text())
assert all(hashlib.sha256((p/f).read_bytes()).hexdigest()==h for f,h in m.items())
print('Refreshed frozen operator helpers verified.')
PY
python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --stopped --require-revocation
```

Expected: verified helpers, post-rotation backup, 59-check bound receipt, unchanged
M3 source/current rotated credentials/current DB, port free and no observed owner.
**Stop here for review.** No approved-window.path is needed or created to skip
the failed old Step 3. A separate prepared-window.path is only a private record
of this verified recovery point, not deployment approval.

## Future deployment after review — prepared, NOT executed

Keep using the variables above and the refreshed frozen helpers. Do not restart
the tunnel, install Android, provision users, copy data/env/token files, or run
M4's Store before this gate. If --stopped refuses, stop and repeat backup/rehearsal
using the corrected helpers; do not continue with stale evidence.

After separately approving source deployment, retain M3 and verify the M4.1 copy:

```bash
test ! -e "$QP_LIVE/src.m41-next"
test ! -e "$QP_LIVE/src.m3-before-m42"
cp -R "$QP_RELEASE/src" "$QP_LIVE/src.m41-next"
python3 - "$QP_LIVE/src.m41-next" "$QP_STAGE/release-manifest.json" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1]);m=json.load(open(sys.argv[2]))
expected={f[4:]:h for f,h in m.items() if f.startswith('src/')}
actual={str(f.relative_to(p)):hashlib.sha256(f.read_bytes()).hexdigest() for f in p.rglob('*') if f.is_file()}
assert actual==expected
print('Reviewed source copy verified.')
PY
python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --stopped --require-revocation
mv "$QP_LIVE/src" "$QP_LIVE/src.m3-before-m42"
if ! mv "$QP_LIVE/src.m41-next" "$QP_LIVE/src"; then
  mv "$QP_LIVE/src.m3-before-m42" "$QP_LIVE/src"
  exit 1
fi
```

The DB is still unmigrated at this point. An interrupted rename must be resolved
from retained M3 source before any launch. Never overwrite recovery directories.

The following separately approved command would be the **first M4 live migration**;
it is forbidden during this recovery/preparation session and was not run:

```bash
nohup python3 "$QP_OPS/m42_backend_launch.py" "$QP_WINDOW" --mode production --require-revocation \
  > "$QP_WINDOW/backend-start.log" 2>&1 < /dev/null &
```

It uses the captured private environment, original absolute DB/loopback port and
NODE_ENV=production. After startup, HTTPS health must be 200, missing bearer 401,
both current tokens valid and plain loopback without HTTPS proof rejected. Use
the frozen HTTPS smoke and check-room-db with token-file arguments only, as in
the historical runbook's validation block. No live smoke was run while stopped.
The live host previously had NODE_ENV unset, so edge/header compatibility remains
a post-approval smoke gate. Quick Tunnel origin and Termux uptime remain risks.

## Source rollback after a future deployment

Stop only the new backend identified by the refreshed guard's --current-pid;
verify --wait-stopped --require-revocation. Verify retained src.m3-before-m42 against
this post-rotation backup's host/src. Retain the failed M4 source under a new
directory, restore only M3 source and, if separately authorized, use:

```bash
nohup python3 "$QP_OPS/m42_backend_launch.py" "$QP_WINDOW" --mode original-environment --require-revocation \
  > "$QP_WINDOW/m3-rollback-start.log" 2>&1 < /dev/null &
```

Keep the **current DB and rotated phone01 file**. No database rewind, table drop,
old token file or old pre-rotation environment/config restore is part of rollback.
M3 rollback on the migrated staging copy already rejects the exposed token and
preserves all old/new saved jobs. Original-environment recovery retains the old
unset NODE_ENV limitation behind the HTTPS tunnel. Full DB recovery would need a
new loss-window/security review: pre-rotation DBs would reactivate the exposed
credential unless rotation were reapplied before any start. Never restore one
automatically. Backend remains stopped pending this review.
