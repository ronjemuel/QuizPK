#!/usr/bin/env python3
"""Explicitly authorized phone01 rotation only. No Store/server/migration import.

Run once while stopped, from a verified pre-rotation private backup. A durable
replacement token is saved before committing; no secret/hash is ever printed.
After commit, never roll back to the compromised hash on a filesystem failure.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import secrets
import sqlite3
import time
from m42_backend_backup import LIVE, ROOT, processes, digest, write_json


def assert_stopped():
    import socket
    if processes():
        raise RuntimeError('SERVER_RUNNING')
    with socket.socket() as probe:
        probe.settimeout(1)
        if probe.connect_ex(('127.0.0.1', 8787)) == 0:
            raise RuntimeError('LISTENER_RUNNING')
    for p in Path('/proc').glob('[0-9]*/fd/*'):
        try:
            if os.readlink(p).startswith(str(LIVE / 'data/quizpk.sqlite')):
                raise RuntimeError('DATABASE_OWNER_PRESENT')
        except OSError:
            pass


def rows(db, table):
    return db.execute('SELECT * FROM "' + table + '" ORDER BY rowid').fetchall()


def rotate(backup):
    os.umask(0o077)
    backup = Path(backup).resolve(strict=True)
    if not backup.is_relative_to(ROOT) or backup.stat().st_mode & 0o077:
        raise RuntimeError('PRIVATE_BACKUP_REQUIRED')
    manifest = json.loads((backup / 'manifest.json').read_text())
    if not all(digest(backup / p) == h for p, h in manifest['files'].items()):
        raise RuntimeError('BACKUP_CHANGED')
    if not all(digest(Path(p)) == item['sha256'] for p, item in manifest['copied'].items()):
        raise RuntimeError('CONFIG_CHANGED')
    assert_stopped()
    private = ROOT / (time.strftime('%Y%m%dT%H%M%SZ', time.gmtime()) + '-phone01-rotation')
    private.mkdir(mode=0o700)
    token = secrets.token_urlsafe(32)
    pending = private / 'replacement-phone01.token'
    with pending.open('x') as f:
        f.write(token + '\n'); f.flush(); os.fsync(f.fileno())
    fd = os.open(private, os.O_RDONLY); os.fsync(fd); os.close(fd)
    old_token = (LIVE / 'private/phone01.token').read_text().strip()
    old_hash = hashlib.sha256(old_token.encode()).hexdigest()
    new_hash = hashlib.sha256(token.encode()).hexdigest()
    db = sqlite3.connect((LIVE / 'data/quizpk.sqlite').as_uri() + '?mode=rw', uri=True, timeout=5)
    original = sqlite3.connect((backup / 'quizpk.sqlite').as_uri() + '?mode=ro&immutable=1', uri=True)
    committed = False
    try:
        db.execute('PRAGMA synchronous=FULL')
        db.execute('BEGIN IMMEDIATE')
        before = {t: rows(db, t) for t in ['users', 'jobs', 'throttle']}
        schema = db.execute('SELECT type,name,sql FROM sqlite_master ORDER BY name').fetchall()
        version = db.execute('PRAGMA user_version').fetchone()
        if any(before[t] != rows(original, t) for t in before):
            raise RuntimeError('LIVE_DATA_CHANGED_SINCE_BACKUP')
        if db.execute("SELECT 1 FROM sqlite_master WHERE name LIKE 'mp_%'").fetchone():
            raise RuntimeError('EXPECTED_M3')
        if db.execute('SELECT count(*) FROM users WHERE id=? AND enabled=1 AND token_hash=?', ('phone01', old_hash)).fetchone()[0] != 1:
            raise RuntimeError('TARGET_TOKEN_MISMATCH')
        changed = db.execute('UPDATE users SET token_hash=? WHERE id=? AND token_hash=? AND enabled=1', (new_hash, 'phone01', old_hash)).rowcount
        if changed != 1:
            raise RuntimeError('TARGET_UPDATE_FAILED')
        checks = {
            'onlyPhone01CredentialChanged': rows(db, 'users') == [(r[0], new_hash if r[0] == 'phone01' else r[1], *r[2:]) for r in before['users']],
            'allSavedJobsUnchanged': rows(db, 'jobs') == before['jobs'],
            'throttleUnchanged': rows(db, 'throttle') == before['throttle'],
            'schemaUnchanged': db.execute('SELECT type,name,sql FROM sqlite_master ORDER BY name').fetchall() == schema,
            'userVersionUnchanged': db.execute('PRAGMA user_version').fetchone() == version,
            'exposedTokenRejected': not db.execute('SELECT 1 FROM users WHERE token_hash=? AND enabled=1', (old_hash,)).fetchone(),
            'replacementTokenAccepted': bool(db.execute('SELECT 1 FROM users WHERE id=? AND token_hash=? AND enabled=1', ('phone01', new_hash)).fetchone()),
        }
        if not all(checks.values()) or db.execute('PRAGMA integrity_check').fetchall() != [('ok',)]:
            raise RuntimeError('PRESERVATION_CHECK_FAILED')
        db.commit(); committed = True
        # Atomic token-file replacement; pending remains a durable recovery copy.
        temporary = LIVE / 'private/phone01.rotation-next'
        with temporary.open('x') as f:
            f.write(token + '\n'); f.flush(); os.fsync(f.fileno())
        os.replace(temporary, LIVE / 'private/phone01.token')
        fd = os.open(LIVE / 'private', os.O_RDONLY); os.fsync(fd); os.close(fd)
        checks['privateTokenFileMatchesReplacement'] = (LIVE / 'private/phone01.token').read_text().strip() == token
        checks['tokenFile600'] = (LIVE / 'private/phone01.token').stat().st_mode & 0o777 == 0o600
        result = {'checks': checks, 'allPassed': all(checks.values()), 'usersPreserved': len(before['users']),
                  'savedJobsPreserved': len(before['jobs']), 'rotationDirectory': str(private),
                  'replacementTokenFile': str(LIVE / 'private/phone01.token'), 'backendStarted': False,
                  'liveMigrationPerformed': False}
        write_json(private / 'result.json', result)
        print(json.dumps(result, indent=2))
    except Exception:
        if not committed:
            db.rollback()
        # Keep replacement privately if commit happened; never re-enable old token.
        write_json(private / 'failure.json', {'databaseCommitOccurred': committed, 'backendMustRemainStopped': True})
        raise
    finally:
        db.close(); original.close()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('backup_directory')
    p.add_argument('--execute-authorized-rotation', action='store_true', required=True)
    args = p.parse_args()
    try:
        rotate(args.backup_directory)
    except Exception:
        print('Rotation stopped. Keep backend stopped; inspect private rotation result. No secret printed.')
        raise SystemExit(1)
