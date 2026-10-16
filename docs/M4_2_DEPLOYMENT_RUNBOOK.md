# M4.2 exact Termux upgrade and rollback commands — REVIEW ONLY

**Superseded after Step 3 failure and phone01 token rotation. Do not execute the
pre-rotation paths/commands below.** The backend is now stopped; use the
[maintenance recovery findings and refreshed guarded procedure](M4_2_MAINTENANCE_RECOVERY.md).
The original runbook remains historical evidence. Do not restore its old token
files, bypass its failed check or start M3 as a recovery shortcut.

Prepared 2026-10-13. **Live steps have not been executed. Stop for review first.**
Run these blocks sequentially in native Termux as the same app user. Do not enable
shell tracing. No secret appears in an argument/output; no Android action is
included. [Audit/results/risks](M4_2_DEPLOYMENT_PLAN.md).

## 1. Establish reviewed paths; verify helpers and drift guard

```bash
set -eu
umask 077
QP_LIVE=/data/data/com.termux/files/home/quizpk-server
QP_PRE=/data/data/com.termux/files/home/.local/share/quizpk-backups/20261013T023118Z-preflight
QP_STAGE="$QP_PRE-staging"
QP_RELEASE="$QP_STAGE/m41"
QP_OPS="$QP_STAGE/operations"
python3 - "$QP_OPS" <<'PY'
from pathlib import Path
import hashlib,json,sys
p=Path(sys.argv[1]);m=json.loads((p/'operations-manifest.json').read_text())
assert all(hashlib.sha256((p/f).read_bytes()).hexdigest()==h for f,h in m.items())
print('Frozen operator helpers verified.')
PY
python3 "$QP_OPS/m42_backend_guard.py" "$QP_PRE"
```

Expected: verified backup/rehearsal/release, exact M3 source/config and same running
process start identity. A restart or source/config change aborts: prepare/review a
new preflight, never edit the stored PID or bypass the guard. New legitimate DB
writes are preserved by the maintenance backup below. No live SQL is opened by
this guard. Arrange a short maintenance window and stop user activity.

## 2. Stop only the identified backend; retain the tunnel

```bash
QP_PID="$(python3 "$QP_OPS/m42_backend_guard.py" "$QP_PRE" --pid)"
kill -TERM "$QP_PID"
python3 "$QP_OPS/m42_backend_guard.py" "$QP_PRE" --stopped
```

Expected: backend exits gracefully, manual npm parent returns to its terminal,
8787 is free and no observable DB owner/restart remains. The guard waits up to
60 seconds. On refusal, stop the procedure and inspect privately; never force-kill
or replace source. Cloudflare Tunnel stays running.

## 3. Fresh maintenance backup and repeated isolated rehearsal

```bash
python3 "$QP_OPS/m42_backend_backup.py" --label maintenance \
  --runtime-from "$QP_PRE" > "$QP_STAGE/maintenance-backup-result.json"
QP_WINDOW="$(python3 - "$QP_STAGE/maintenance-backup-result.json" <<'PY'
import json,sys
r=json.load(open(sys.argv[1]))
assert all(r[k] for k in ['snapshotIntegrity','snapshotDataEqual','standaloneRestoreEqual','configCopiesVerified','liveDurableBytesEqual'])
print(r['backupDirectory'])
PY
)"
printf '%s\n' "$QP_WINDOW" > "$QP_STAGE/approved-window.path"
node "$QP_OPS/m42_backend_rehearsal.mjs" "$QP_WINDOW" "$QP_RELEASE" \
  > "$QP_STAGE/maintenance-rehearsal.json" 2> "$QP_STAGE/maintenance-rehearsal-runtime.txt"
python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --stopped
```

Expected: new private snapshot/restore including every committed job since today's
preflight, 55 passing checks on a separate DB, zero provider calls, preservation
and successful rollback/re-upgrade. The frozen release is used, not workspace
code. No live migration has happened. A failure stops before source replacement.
With M3 source still untouched, resume it while investigating using:

```bash
nohup python3 "$QP_OPS/m42_backend_launch.py" "$QP_PRE" --mode original-environment \
  > "$QP_STAGE/m3-recovery-start.log" 2>&1 < /dev/null &
```

## 4. Verify copied source and retain M3 before swapping

package.json is identical; no dependency/env/token/data replacement is needed.
Both destination directories must be absent; never overwrite a recovery directory.

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
print('Source copy matches reviewed M4.1 release.')
PY
python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --stopped
mv "$QP_LIVE/src" "$QP_LIVE/src.m3-before-m42"
if ! mv "$QP_LIVE/src.m41-next" "$QP_LIVE/src"; then
  mv "$QP_LIVE/src.m3-before-m42" "$QP_LIVE/src"
  exit 1
fi
```

Expected: only src changed; old source retained. DB has not been migrated. If
interrupted between renames, restore retained M3 src before any launch. Do not
delete existing directories merely to make a gate pass.

## 5. Start once — the first live migration step

```bash
nohup python3 "$QP_OPS/m42_backend_launch.py" "$QP_WINDOW" --mode production \
  > "$QP_WINDOW/backend-start.log" 2>&1 < /dev/null &
printf '%s\n' "$!" > "$QP_WINDOW/launch.pid"
```

Launcher verifies backup, rehearsal, deployed source hashes, unchanged private
configuration, existing nonempty DB and free port before exec. It privately
restores the captured env, fixes the original absolute DB and 127.0.0.1:8787,
and sets NODE_ENV=production. Store applies the additive live migration now.
Env/token files and tunnel remain unchanged. Wait for startup before smoke; on
failure inspect the private log and use source rollback. Do not start another
instance or send a provider request.

## 6. HTTPS/schema/token smoke; stop for review

```bash
python3 "$QP_OPS/m42_backend_smoke.py" "$QP_OPS/smoke-context.json"
QUIZPK_DB_PATH="$QP_LIVE/data/quizpk.sqlite" node "$QP_LIVE/src/check-room-db.mjs" \
  --token-file "$QP_LIVE/private/owner.token"
QUIZPK_DB_PATH="$QP_LIVE/data/quizpk.sqlite" node "$QP_LIVE/src/check-room-db.mjs" \
  --token-file "$QP_LIVE/private/phone01.token"
```

Expected: plain loopback without HTTPS proof is 400 HTTPS_REQUIRED; public HTTPS
health is 200; missing bearer on public room lookup is 401 AUTH_REQUIRED; both
existing tokens obtain v1 ONLINE snapshots. All boolean smoke checks must be true;
each read-only DB check must show databasePresent, identitySchemaPresent,
roomSchemaPresent and tokenMatchesEnabledIdentity true. Smoke verifies TLS and
refuses redirects. Room reads legitimately update multiplayer rate/presence
bookkeeping; there is no generation/provisioning.

On any failure stop and roll back source. Never replace tokens, change DB paths,
bypass TLS or print private logs. Original cached responses were verified in
rehearsal. Two-phone/offline saved-quiz replay remain in the
[device plan](M4_2_DEVICE_TEST_PLAN.md); no APK installation is part of this window.
Stop after backend lobby smoke for review; M4.3/LAN remain deferred.

## Preferred rollback — source only, retaining all saved data

Re-establish the step 1 variables in a new terminal and verify frozen helpers.

```bash
QP_WINDOW="$(cat "$QP_STAGE/approved-window.path")"
```

If the new backend is running, stop only that identified process:

```bash
QP_FAILED_PID="$(python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --current-pid)"
kill -TERM "$QP_FAILED_PID"
python3 "$QP_OPS/m42_backend_guard.py" "$QP_WINDOW" --wait-stopped
```

If it already exited, run only --wait-stopped. Never kill a stale launch.pid.
Then verify and restore retained M3 source:

```bash
test -d "$QP_LIVE/src.m3-before-m42"
test ! -e "$QP_LIVE/src.m41-rejected"
python3 - "$QP_LIVE/src.m3-before-m42" "$QP_WINDOW/host/src" <<'PY'
from pathlib import Path
import hashlib,sys
def tree(p):
 return {str(f.relative_to(p)):hashlib.sha256(f.read_bytes()).hexdigest() for f in p.rglob('*') if f.is_file()}
assert tree(Path(sys.argv[1]))==tree(Path(sys.argv[2]))
print('Retained M3 source matches maintenance backup.')
PY
mv "$QP_LIVE/src" "$QP_LIVE/src.m41-rejected"
if ! mv "$QP_LIVE/src.m3-before-m42" "$QP_LIVE/src"; then
  mv "$QP_LIVE/src.m41-rejected" "$QP_LIVE/src"
  exit 1
fi
nohup python3 "$QP_OPS/m42_backend_launch.py" "$QP_WINDOW" --mode original-environment \
  > "$QP_WINDOW/m3-rollback-start.log" 2>&1 < /dev/null &
```

Expected: M3 AI authentication/cache works, original HTTPS health is 200, rooms
again return unsupported 404, and all current jobs plus additive multiplayer rows
remain in the same DB. No DB restore/drop/token replacement occurs. Original-env
recovery restores unset NODE_ENV and its prior header-enforcement limitation;
public TLS tunnel stays unchanged. Investigate before a subsequent deployment.

Check rollback health without a token or DB counter writes:

```bash
PYTHONPATH="$QP_OPS" python3 - "$QP_OPS/smoke-context.json" <<'PY'
import json,sys,urllib.request,ssl
from m42_backend_smoke import NoRedirect
origin=json.load(open(sys.argv[1]))['origin']
opener=urllib.request.build_opener(NoRedirect(),urllib.request.HTTPSHandler(context=ssl.create_default_context()))
with opener.open(origin+'/health',timeout=15) as r:
 assert r.status==200 and json.load(r)=={'status':'ok'}
print('M3 public HTTPS health passed.')
PY
```

## Exceptional full DB recovery — requires a separate loss-window decision

Normal rollback above needs no DB restore. If corruption makes a rewind necessary,
review the actual failure and accept loss of **all writes after the chosen verified
snapshot** before preparing executable restore commands. Keep all DB owners stopped;
verify snapshot hash/integrity/standalone restore; copy to a new verified
data/restore.next.sqlite; move the stopped current main DB and every -wal/-shm/
-journal sidecar together to a new private quarantine; rename the verified snapshot
to the original DB path with 0600 permissions and restore verified M3 source/env.
Never leave an old WAL beside a restored main DB or automatically rewind/erase
newer jobs/multiplayer tables. Retain current files and all backup snapshots.
This exceptional rewind is not part of the proposed source-only deployment.
