#!/usr/bin/env python3
"""M4.3 schema-aware private SQLite snapshot and Termux configuration backup.

No Store import, migration, service control, provider call or live write occurs.
Only the created private directory is writable. Never prints exception details.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import sqlite3
import time
import socket

TERMUX = Path('/data/data/com.termux/files/home')
LIVE = TERMUX / 'quizpk-server'
ROOT = TERMUX / '.local/share/quizpk-backups'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inventory(db):
    result = {'user_version': db.execute('PRAGMA user_version').fetchone()[0]}
    tables = [r[0] for r in db.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name")]
    if not {'users', 'jobs', 'throttle', 'mp_schema', 'mp_rooms', 'mp_members', 'mp_requests', 'mp_limits'}.issubset(tables):
        raise RuntimeError('M42_SCHEMA_REQUIRED')
    result['schema'] = db.execute("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name").fetchall()
    result['multiplayerVersion'] = db.execute('SELECT max(version) FROM mp_schema').fetchone()[0]
    if result['multiplayerVersion'] not in (1, 2):
        raise RuntimeError('SUPPORTED_MP_SCHEMA_REQUIRED')
    for table in tables:
        columns = [r[1] for r in db.execute(f'PRAGMA table_info("{table}")')]
        if not columns:
            raise RuntimeError('M3_SCHEMA_REQUIRED')
        order = ','.join('"' + c.replace('"', '""') + '"' for c in columns)
        rows = db.execute(f'SELECT * FROM "{table}" ORDER BY {order}').fetchall()
        encoded = json.dumps(rows, ensure_ascii=True, separators=(',', ':')).encode()
        result[table] = {'count': len(rows), 'sha256': hashlib.sha256(encoded).hexdigest(),
                         'schema': db.execute('SELECT sql FROM sqlite_master WHERE name=?', (table,)).fetchone()[0]}
    return result


def readonly(path):
    if not path.is_file():
        raise RuntimeError('DATABASE_MISSING')
    return sqlite3.connect(path.resolve().as_uri() + '?mode=ro', uri=True, timeout=5)


def processes():
    found = []
    for p in Path('/proc').glob('[0-9]*'):
        try:
            if Path(os.readlink(p / 'cwd')) != LIVE:
                continue
            args = [a.decode() for a in (p / 'cmdline').read_bytes().split(b'\0') if a]
            exe = os.readlink(p / 'exe')
            env = dict(a.decode().split('=', 1) for a in (p / 'environ').read_bytes().split(b'\0') if b'=' in a)
            if exe.endswith('/node') and any(a.endswith('src/server.mjs') for a in args):
                found.append({'pid': int(p.name), 'exe': exe, 'args': args, 'env': env,
                              'starttime': (p / 'stat').read_text().rsplit(')', 1)[1].split()[19]})
        except (OSError, UnicodeError):
            continue
    return found


def ensure_stopped():
    """No server/listener or observable SQLite owner; never stop/kill anything."""
    if processes():
        raise RuntimeError('MAINTENANCE_REQUIRES_STOPPED_SERVER')
    with socket.socket() as probe:
        probe.settimeout(1)
        if probe.connect_ex(('127.0.0.1', 8787)) == 0:
            raise RuntimeError('LISTENER_PRESENT')
    for p in Path('/proc').glob('[0-9]*/fd/*'):
        try:
            if os.readlink(p).startswith(str(LIVE / 'data/quizpk.sqlite')):
                raise RuntimeError('DATABASE_OWNER_PRESENT')
        except OSError:
            pass


def durable_hashes(db_path):
    return {str(p): digest(p) for p in [db_path, Path(str(db_path) + '-wal')] if p.exists()}


def write_json(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')
    path.chmod(0o600)


def backup(label, runtime_from=None):
    os.umask(0o077)
    if not LIVE.is_dir() or LIVE.is_symlink():
        raise RuntimeError('HOST_PATH_REQUIRED')
    # No shared-storage backup, symlink redirection or accidental reuse.
    ROOT.mkdir(parents=True, exist_ok=True, mode=0o700)
    if ROOT.resolve() != ROOT or ROOT.stat().st_mode & 0o077:
        raise RuntimeError('PRIVATE_ROOT_REQUIRED')
    dest = ROOT / (time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) + '-' + label)
    dest.mkdir(mode=0o700)
    if runtime_from:
        seed = Path(runtime_from).resolve()
        if not seed.is_relative_to(ROOT):
            raise RuntimeError('PRIVATE_RUNTIME_REQUIRED')
        seed_manifest = json.loads((seed / 'manifest.json').read_text())
        if digest(seed / 'runtime.json') != seed_manifest['files']['runtime.json']:
            raise RuntimeError('CAPTURED_RUNTIME_CHANGED')
        runtime = json.loads((seed / 'runtime.json').read_text())
        ensure_stopped()
    else:
        running = processes()
        if len(running) != 1:
            raise RuntimeError('ONE_BACKEND_REQUIRED')
        runtime = running[0]
    env = runtime['env']
    db_path = (LIVE / env.get('QUIZPK_DB_PATH', './data/quizpk.sqlite')).resolve()
    if db_path != LIVE / 'data/quizpk.sqlite':
        raise RuntimeError('REVIEW_DB_PATH')
    runtime['dbPath'] = str(db_path)
    write_json(dest / 'runtime.json', runtime)
    copied = {}
    inputs = [LIVE / 'package.json', *sorted((LIVE / 'src').rglob('*')),
              LIVE / 'GEMINI_API_KEY.env', *sorted((LIVE / 'private').rglob('*'))]
    parent_env = LIVE.parent / 'GEMINI_API_KEY/GEMINI_API_KEY.env'
    if parent_env.exists():
        inputs.append(parent_env)
    for f in inputs:
        if f.is_dir():
            continue
        if f.is_symlink() or not f.is_file():
            raise RuntimeError('REGULAR_CONFIG_REQUIRED')
        rel = ('host/' + str(f.relative_to(LIVE))) if f.is_relative_to(LIVE) else 'parent/GEMINI_API_KEY.env'
        target = dest / rel
        target.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
        before = digest(f)
        shutil.copyfile(f, target)
        target.chmod(0o600)
        if before != digest(target) or before != digest(f):
            raise RuntimeError('CONFIG_CHANGED_DURING_BACKUP')
        copied[str(f)] = {'backup': rel, 'sha256': before}
    # Capture the tunnel's launch configuration privately, without touching it.
    tunnels = []
    for p in Path('/proc').glob('[0-9]*'):
        try:
            if os.readlink(p / 'exe').endswith('/cloudflared') and Path(os.readlink(p / 'cwd')) == LIVE:
                tunnels.append({'pid': int(p.name), 'args': [a.decode() for a in (p / 'cmdline').read_bytes().split(b'\0') if a],
                    'env': dict(a.decode().split('=', 1) for a in (p / 'environ').read_bytes().split(b'\0') if b'=' in a)})
        except (OSError, UnicodeError):
            pass
    write_json(dest / 'tunnel.json', tunnels)
    before = durable_hashes(db_path)
    if runtime_from:
        # Stopped WAL DBs may create an empty live WAL even on mode=ro open.
        # Work exclusively on a verified private physical copy. Never ignore a
        # nonempty WAL with immutable=1, delete sidecars or relax byte equality.
        ensure_stopped()
        if Path(str(db_path) + '-journal').exists():
            raise RuntimeError('JOURNAL_REQUIRES_REVIEW')
        raw = dest / 'source-copy'
        raw.mkdir(mode=0o700)
        for name, expected in before.items():
            f = Path(name); copy = raw / f.name
            shutil.copyfile(f, copy); copy.chmod(0o600)
            if digest(copy) != expected:
                raise RuntimeError('QUIESCENT_COPY_MISMATCH')
        ensure_stopped()
        if durable_hashes(db_path) != before:
            raise RuntimeError('LIVE_CHANGED_DURING_COPY')
        source = readonly(raw / db_path.name)
    else:
        source = readonly(db_path)  # Online mode uses SQLite's read snapshot.
    target_path = dest / 'quizpk.sqlite'
    target = sqlite3.connect(target_path)
    started = time.monotonic()
    def progress(*unused):
        if time.monotonic() - started > 30:
            raise RuntimeError('BACKUP_TIMEOUT')
    try:
        source.execute('BEGIN')
        original = inventory(source)
        source.backup(target, pages=128, progress=progress, sleep=0.1)
        source.execute('COMMIT')
        if target.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
            raise RuntimeError('INTEGRITY_FAILED')
        if target.execute('PRAGMA foreign_key_check').fetchall() or inventory(target) != original:
            raise RuntimeError('SNAPSHOT_MISMATCH')
        target.execute('PRAGMA journal_mode=DELETE')
    finally:
        source.close()
        target.close()
    target_path.chmod(0o600)
    # A separately restored main file must work with no live WAL or shared memory.
    restore = dest / 'restore-check.sqlite'
    shutil.copyfile(target_path, restore)
    restore.chmod(0o600)
    db = readonly(restore)
    try:
        if db.execute('PRAGMA integrity_check').fetchall() != [('ok',)] or inventory(db) != original:
            raise RuntimeError('RESTORE_FAILED')
    finally:
        db.close()
    after = durable_hashes(db_path)
    if runtime_from:
        ensure_stopped()
        if after != before:
            raise RuntimeError('LIVE_CHANGED_DURING_MAINTENANCE_BACKUP')
    manifest = {'format': 2, 'createdUtc': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()),
                'copied': copied, 'inventory': original, 'snapshotSha256': digest(target_path),
                'liveDurableBefore': before, 'liveDurableAfter': after,
                'liveDurableBytesEqual': before == after, 'runtimeSource': runtime_from,
                'sourceMode': 'stopped-verified-private-copy' if runtime_from else 'online-readonly-snapshot',
                'checks': {'snapshotIntegrity': True, 'snapshotDataEqual': True,
                           'standaloneRestoreEqual': True, 'configCopiesVerified': True}}
    if any(digest(Path(name)) != item['sha256'] for name, item in copied.items()):
        raise RuntimeError('CONFIG_CHANGED_DURING_SNAPSHOT')
    if not runtime_from:
        current = processes()
        if len(current) != 1 or (current[0]['pid'], current[0]['starttime']) != (runtime['pid'], runtime['starttime']):
            raise RuntimeError('RUNNING_PROCESS_CHANGED')
    manifest['files'] = {str(f.relative_to(dest)): digest(f) for f in dest.rglob('*') if f.is_file()}
    write_json(dest / 'manifest.json', manifest)
    for f in dest.rglob('*'):
        if f.is_file():
            with f.open('rb') as stream:
                os.fsync(stream.fileno())
    for d in [p for p in dest.rglob('*') if p.is_dir()] + [dest, ROOT]:
        fd = os.open(d, os.O_RDONLY)
        try:
            os.fsync(fd)
        finally:
            os.close(fd)
    print(json.dumps({'backupDirectory': str(dest), **manifest['checks'],
                      'liveDurableBytesEqual': before == after, 'liveMigrationPerformed': False}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--label', choices=['preflight', 'maintenance'], default='preflight')
    parser.add_argument('--runtime-from', help='Private verified preflight directory; requires stopped backend')
    args = parser.parse_args()
    try:
        if bool(args.runtime_from) != (args.label == 'maintenance'):
            raise RuntimeError('MAINTENANCE_RUNTIME_REQUIRED')
        backup(args.label, args.runtime_from)
    except Exception:
        print('Private backup failed; no live deployment performed. Inspect privately, without printing credentials.')
        raise SystemExit(1)
