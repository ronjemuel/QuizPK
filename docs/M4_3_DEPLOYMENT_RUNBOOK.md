# M4.3 Termux deployment preparation — review only

Prepared 2026-10-13. **No shutdown, source replacement, live migration, credential
change or APK installation has been performed.** The working M4.2 process remains
running. These commands describe a future separately approved maintenance window.
Do not execute sections 2–7 during preparation.

The current preflight and isolated rehearsal pass. Public HTTPS is an unresolved
gate: the hostname in the prior maintenance context fails DNS resolution here,
while a control hostname resolves. A cloudflared process remains running and
plain loopback correctly returns `400 HTTPS_REQUIRED`. Confirm the actual URL
saved on the working phones before deployment. Do not restart the tunnel, guess a
hostname, send tokens to an unverified origin, disable TLS checks or edit a receipt
to get past this gate. A corrected origin needs a fresh private context and bound
rehearsal; the frozen context below must not be patched in place.

## Verified preparation

- Private preflight: `~/.local/share/quizpk-backups/20261013T094238Z-preflight`.
  SQLite online read snapshot includes committed WAL data. Integrity, complete
  table/schema inventory equality, independent standalone restore and private
  configuration copies passed. Live main/WAL bytes also matched during this backup.
- Snapshot: schema 1; 2 identities, 4 saved AI jobs, 6 rooms, 7 membership rows,
  46 request receipts, 7 multiplayer limit rows and 1 throttle row. No pending AI
  generation was initiated by preparation. No private contents or credential
  hashes are published.
- Actual deployed `src/` matches the approved M4.2 checkpoint exactly. Existing
  tokens authenticate under their original identities; the rotated phone01 token
  matches its private recovery copy and the exposed token is rejected. All four
  original saved responses replay over the staging HTTP API without a provider call.
- Frozen M4.3 backend: 26 source/package/test files; **74/74 backend tests pass**
  from that copy. Package/dependencies and M3 source are unchanged.
- **87/87 isolated rehearsal checks pass**. Migration preserves all existing rows
  and schemas, appending exactly the new ledger row. Classic, duplicate answers,
  scoring, timeout, safe reveal, complete results, reconnect/restart, Moderator
  lobby separation and disabled-Classic fallback pass. Fallback uses a separate
  copy, retains a post-upgrade saved AI job and ungraded accepted answers, and is
  checked through the actual production entrypoint/environment flag.
- The unmodified deployed M4.2 source refuses schema 2, as expected. A source-only
  downgrade after migration is **not** the rollback procedure.

The initial staging comparison incorrectly required the migration ledger to stay
identical. Only its legitimate new version-2 row differed; all original rows were
equal. The corrected test requires the original ledger rows unchanged plus exactly
one version-2 row. The failed attempt and subsequent passing attempts are retained
privately. An operator fixture also left its synthetic SQLite connection open;
the owner guard correctly refused it, and the fixture now closes it. No guard was
weakened. See `validation/m4.3-deployment-prep/` for sanitized final evidence.

## 1. Verify the prepared release and current process — read-only

Run in native Termux as the existing app user. Keep shell tracing disabled.
`set -eu` is required: any failed command stops the sequence. A new terminal must
re-establish these variables; none contains a credential.

```bash
set -eu
umask 077
export PYTHONDONTWRITEBYTECODE=1
QP_LIVE=/data/data/com.termux/files/home/quizpk-server
QP_PRE=/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T094238Z-preflight
QP_PREP="$QP_PRE-m43-preparation"
QP_RELEASE="$QP_PREP/release"
QP_OPS="$QP_PREP/operations"
QP_NODE=/data/data/com.termux/files/usr/bin/node

python3 - "$QP_PREP" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1])
expected={
 'release-manifest.json':'297c4494094a29a7895e7a1f738da8f0295357c0497135d0db13f827e7b40e27',
 'protocol-manifest.json':'844d27711e5071205a9241a906335dfe2e97d50df85be9e0c7fe8843c679f3b2',
 'operations/operations-manifest.json':'7e642b6f222e1ab7c87fe1708760ea8766654c3437d280409a466399e2dd150f'
}
for name,h in expected.items():
 assert hashlib.sha256((p/name).read_bytes()).hexdigest()==h
ops=p/'operations'
for name,h in json.loads((ops/'operations-manifest.json').read_text()).items():
 assert hashlib.sha256((ops/name).read_bytes()).hexdigest()==h
print('PASS: frozen manifest and operator hashes.')
PY
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_PRE" "$QP_PREP"
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP" --health-only
```

Expected: manifest/helper hashes pass; backup/release/rehearsal/source/config and
original PID/start identity pass; `plainLoopbackRejected` and `publicTlsHealth`
are true. **The final command currently fails on the old origin and must stop this
sequence.** Fix that preparation prerequisite and review the refreshed context
before approving shutdown. Never continue because staging passed.

A restart, source/config/token change or a different database path requires a
new private preflight and rehearsal. Ordinary new DB activity does not invalidate
the running preflight; the stopped backup below must capture it. No live Store
constructor or direct live SQLite check is used by these guards.

## 2. Guarded graceful shutdown — separate approval required

Arrange a short maintenance window and stop quiz generation/player activity.
Keep the tunnel running. Do not force-kill a pending provider request. Recheck
section 1 immediately before this block. Never use a remembered PID or launch.pid.

```bash
QP_PID="$(python3 "$QP_OPS/m43_backend_ops.py" pid "$QP_PRE" "$QP_PREP")"
kill -TERM "$QP_PID"
python3 "$QP_OPS/m43_backend_ops.py" wait-stopped "$QP_PRE" "$QP_PREP"
```

Expected: graceful backend exit within 55 seconds, port 8787 free, no automatic
restart and no observable SQLite owner. The helper never sends a signal. If the
guard refuses, stop here; do not use SIGKILL or proceed with source replacement.
M4.2 resets lobby ready/presence on a later startup; clients must reconnect/ready.

## 3. Fresh stopped maintenance backup and repeat rehearsal

The old `m42_backend_backup.py` is M3-only and rejects multiplayer databases.
Use the frozen schema-aware M4.3 helper. It hashes/copies quiescent main **and WAL**
before opening only the private copy. It refuses a journal, live owner, listener,
source/config drift or durable-byte mismatch. It never creates stopped live
sidecars, ignores an orphan WAL, deletes data or relaxes a check.

```bash
test ! -e "$QP_PREP/maintenance-backup-result.json"
python3 "$QP_OPS/m43_backend_backup.py" --label maintenance --runtime-from "$QP_PRE" \
  > "$QP_PREP/maintenance-backup-result.json"
QP_WINDOW="$(python3 - "$QP_PREP/maintenance-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] is True for k in ['snapshotIntegrity','snapshotDataEqual',
 'standaloneRestoreEqual','configCopiesVerified','liveDurableBytesEqual'])
print(r['backupDirectory'])
PY
)"
QP_REVOKED=/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T031519Z-maintenance/host/private/phone01.token
QP_ROTATED=/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T032648Z-phone01-rotation/replacement-phone01.token
"$QP_NODE" "$QP_OPS/m43_backend_rehearsal.mjs" "$QP_WINDOW" "$QP_PREP" "$QP_REVOKED" "$QP_ROTATED" \
  > "$QP_WINDOW/rehearsal-output.json" 2> "$QP_WINDOW/rehearsal-private.log"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --stopped-durable
printf '%s\n' "$QP_WINDOW" > "$QP_PREP/maintenance-window.path"
```

Expected: all five backup checks true; a new separate staging DB with 87 passing
checks, zero provider calls, unchanged original rows, current token compatibility
and tested fallback. The guard binds the exact backup/release/protocol/context and
rehearsal-code hashes and rechecks live main/WAL bytes. `maintenance-window.path`
is only a path record written **after** the checks, never an approval bypass.

If staging fails, retain its private evidence. Do not overwrite the failed staging
directory or patch its result. Live source is still M4.2. Resuming M4.2 after a
failed rehearsal needs a reviewed recovery decision; this sequence deliberately
does not provide an unchecked automatic startup shortcut. The newly captured
backup must never be replaced by today's online preflight.

## 4. Copy and verify source; retain actual M4.2

These paths must not exist, including broken symlinks. Never erase a previous
attempt to satisfy the guard. Only `src/` changes; package, credentials and DB stay
in place. `classic-bank.json` is part of the frozen source and must be included.

```bash
QP_NEXT="$QP_LIVE/src.m43-next-20261013T094238Z"
QP_OLD="$QP_LIVE/src.m42-before-m43-20261013T094238Z"
test ! -e "$QP_NEXT" && test ! -L "$QP_NEXT"
test ! -e "$QP_OLD" && test ! -L "$QP_OLD"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --stopped-durable
cp -R "$QP_RELEASE/src" "$QP_NEXT"
python3 - "$QP_NEXT" "$QP_PREP/release-manifest.json" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1]);m=json.load(open(sys.argv[2]))
assert not any(f.is_symlink() for f in p.rglob('*'))
actual={str(f.relative_to(p)):hashlib.sha256(f.read_bytes()).hexdigest() for f in p.rglob('*') if f.is_file()}
assert actual=={n[4:]:h for n,h in m.items() if n.startswith('src/')}
print('PASS: copied source exactly matches frozen M4.3.')
PY
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --stopped-durable
mv "$QP_LIVE/src" "$QP_OLD"
if ! mv "$QP_NEXT" "$QP_LIVE/src"; then
  mv "$QP_OLD" "$QP_LIVE/src"
  exit 1
fi
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_WINDOW" "$QP_PREP" --source release --stopped-durable
```

Expected: exact M4.3 source installed, actual M4.2 source retained, live DB still
schema 1 and byte-identical to the stopped backup. If interrupted between renames,
resolve the missing source from the retained verified directories before any
launch. No database, env, token, test fixture or Android file is copied to live.

## 5. Production startup — first live migration

Only after all previous gates and deployment approval:

```bash
test ! -e "$QP_WINDOW/m43-start.log"
nohup python3 "$QP_OPS/m43_backend_ops.py" launch "$QP_WINDOW" "$QP_PREP" --mode enabled \
  > "$QP_WINDOW/m43-start.log" 2>&1 < /dev/null &
```

The launcher again requires no owner/listener, a verified maintenance backup and
rehearsal, exact release/config, existing DB and strict main/WAL equality. It loads
the captured environment privately and uses the captured Node executable with
`HOST=127.0.0.1`, `PORT=8787`, `NODE_ENV=production`, the original absolute DB path
and `QUIZPK_CLASSIC_ENABLED=true`. Secrets never appear in argv or shell expansion.
This Store constructor transactionally appends schema 2 and five match tables.

Wait for startup; check only the safe result with section 6. A background shell
exit is not proof of successful startup. On failure do not start another instance
or print the private startup log into chat. Use the fallback decision below.

## 6. Public HTTPS and private snapshot validation

```bash
python3 "$QP_OPS/m43_backend_ops.py" current-pid "$QP_WINDOW" "$QP_PREP" --source release
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP"
test ! -e "$QP_WINDOW/post-start-backup-result.json"
python3 "$QP_OPS/m43_backend_backup.py" --label preflight > "$QP_WINDOW/post-start-backup-result.json"
QP_POST="$(python3 - "$QP_WINDOW/post-start-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] is True for k in ['snapshotIntegrity','snapshotDataEqual','standaloneRestoreEqual','configCopiesVerified'])
print(r['backupDirectory'])
PY
)"
python3 "$QP_OPS/m43_verify_snapshot.py" "$QP_WINDOW" "$QP_POST" "$QP_PREP"
```

Expected: public HTTPS health 200, missing/exposed bearer 401, current tokens
accepted, plain loopback rejected, and correct advertised lobby capability where
a current lobby exists. TLS is verified and redirects refused. Room GETs perform
ordinary rate/presence housekeeping; no generation/provisioning occurs. The online
backup may observe legitimate live writes, so durable-byte equality is diagnostic
there; it remains mandatory for every stopped maintenance/launch gate.

Snapshot validation opens **only private backups**, requiring schema 2, integrity,
foreign keys, unchanged user_version, all prior users/jobs, current token validity
and exposed-token rejection. No read-only live SQLite checker is used. Rehearsal
already proved cached M3 HTTP replay; perform real phone acceptance with
[M4_3_DEVICE_TEST_PLAN.md](M4_3_DEVICE_TEST_PLAN.md) after separate APK-install
approval. M1/M2 are native offline features; their 106-test Android regression
record and untouched APK/source remain the implementation evidence, not new
backend HTTP routes. Stop for backend/physical review.

## 7. Fallback after schema 2 — retain current DB and M4.3 source

Use this if post-start validation or Classic acceptance fails. No source downgrade,
ledger edit, table drop, old token restore or DB rewind is permitted. Re-establish
section 1 variables and read the successful maintenance path privately:

```bash
QP_WINDOW="$(cat "$QP_PREP/maintenance-window.path")"
QP_PID="$(python3 "$QP_OPS/m43_backend_ops.py" current-pid "$QP_WINDOW" "$QP_PREP" --source release)"
kill -TERM "$QP_PID"
python3 "$QP_OPS/m43_backend_ops.py" wait-stopped "$QP_WINDOW" "$QP_PREP" --source release
```

If the failed process already exited, omit PID lookup/signal and run only the
wait-stopped guard. Do not signal a stale PID. Capture every post-upgrade write:

```bash
test ! -e "$QP_WINDOW/fallback-backup-result.json"
python3 "$QP_OPS/m43_backend_backup.py" --label maintenance --runtime-from "$QP_WINDOW" \
  > "$QP_WINDOW/fallback-backup-result.json"
QP_FALLBACK="$(python3 - "$QP_WINDOW/fallback-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] is True for k in ['snapshotIntegrity','snapshotDataEqual',
 'standaloneRestoreEqual','configCopiesVerified','liveDurableBytesEqual'])
print(r['backupDirectory'])
PY
)"
python3 "$QP_OPS/m43_verify_snapshot.py" "$QP_WINDOW" "$QP_FALLBACK" "$QP_PREP"
python3 "$QP_OPS/m43_backend_ops.py" verify "$QP_FALLBACK" "$QP_PREP" \
  --source release --stopped-durable --reference "$QP_WINDOW"
nohup python3 "$QP_OPS/m43_backend_ops.py" launch "$QP_FALLBACK" "$QP_PREP" \
  --mode disabled --reference "$QP_WINDOW" > "$QP_FALLBACK/fallback-start.log" 2>&1 < /dev/null &
```

Expected: verified current schema-2 snapshot, same M4.3 source, production HTTPS
configuration and **current DB**, with `QUIZPK_CLASSIC_ENABLED=false`. The earlier
bound rehearsal proves this fallback; fresh strict durable equality proves that
all later writes are retained. Startup finalizes in-flight games as interrupted,
without exposing/grading unrevealed questions. Earlier scores, accepted answer
records and saved AI jobs remain; new Classic starts are disabled. Ordinary
Classic/Moderator lobbies and authenticated final results remain usable.

```bash
python3 "$QP_OPS/m43_backend_smoke.py" "$QP_PREP" --fallback
test ! -e "$QP_FALLBACK/post-fallback-backup-result.json"
python3 "$QP_OPS/m43_backend_backup.py" --label preflight > "$QP_FALLBACK/post-fallback-backup-result.json"
QP_FALLBACK_POST="$(python3 - "$QP_FALLBACK/post-fallback-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] is True for k in ['snapshotIntegrity','snapshotDataEqual','standaloneRestoreEqual','configCopiesVerified'])
print(r['backupDirectory'])
PY
)"
python3 "$QP_OPS/m43_verify_snapshot.py" "$QP_FALLBACK" "$QP_FALLBACK_POST" "$QP_PREP"
```

The final comparison also preserves all prior match answer/key/player records.
Keep phones idle during this check; concurrent legitimate gameplay/dismissal can
produce drift requiring investigation. Never rewind data to make equality pass.

If launch failed before migration committed, the fresh stopped backup must show
schema 1 instead. The schema-2 fallback gate will refuse. Inspect that private copy
and retained source, then prepare a reviewed M4.2 source recovery. Do not infer the
schema from a partial startup log. If schema 2 committed but the new source cannot
run even with Classic disabled, keep the service stopped for a compatible repair.
An exceptional DB restore needs a separate data-loss and credential-revocation
review; it is not part of this procedure.

## Limits and review stop

The on-phone private backup is not protection against loss of the phone. Termux
background termination and temporary tunnel availability remain operational risks.
Existing expired room/receipt/limit cleanup and startup presence resets can occur
after migration; the migration itself preserved all original rows in rehearsal.
No claim of physical M4.3 acceptance is made. Confirm the current HTTPS origin and
review these commands before scheduling deployment. M4.4, moderator gameplay, LAN,
monetization and APK installation remain outside this preparation.
