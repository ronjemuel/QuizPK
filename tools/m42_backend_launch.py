#!/usr/bin/env python3
"""REVIEW ONLY: foreground launcher for the separately approved maintenance window.

Reads the captured private environment without shell expansion or secret arguments.
This tool starts the real server and CAN migrate live SQLite; never run in preflight.
"""
import argparse
import json
import os
from pathlib import Path
import socket
import hashlib
from m42_backend_backup import durable_hashes, ensure_stopped
from m42_backend_guard import verify_receipt

LIVE = Path('/data/data/com.termux/files/home/quizpk-server')
ROOT = Path('/data/data/com.termux/files/home/.local/share/quizpk-backups')

def launch(backup_directory, mode, require_revocation=False):
    backup = Path(backup_directory).resolve(strict=True)
    if not backup.is_relative_to(ROOT) or backup.stat().st_mode & 0o077:
        raise RuntimeError()
    runtime = json.loads((backup / 'runtime.json').read_text())
    manifest = json.loads((backup / 'manifest.json').read_text())
    sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
    if not all(sha(backup / p) == h for p, h in manifest['files'].items()):
        raise RuntimeError()
    for p, item in manifest['copied'].items():
        f = Path(p)
        if not f.is_relative_to(LIVE / 'src'):
            if sha(f) != item['sha256']:
                raise RuntimeError()
    env = runtime['env'].copy()
    db = LIVE / 'data/quizpk.sqlite'
    if not db.is_file() or db.stat().st_size == 0 or db.is_symlink():
        raise RuntimeError()
    env.update(QUIZPK_DB_PATH=str(db), HOST='127.0.0.1', PORT='8787')
    if require_revocation:
        verify_receipt(backup, Path(str(backup) + '-staging'), True)
    if mode == 'production':
        if manifest.get('liveDurableBytesEqual') is not True or durable_hashes(db) != manifest['liveDurableAfter']:
            raise RuntimeError()
        staging = Path(str(backup) + '-staging')
        verify_receipt(backup, staging, require_revocation)
        release = json.loads((staging / 'release-manifest.json').read_text())
        if not all(sha(LIVE / p) == h for p, h in release.items()):
            raise RuntimeError()
        env['NODE_ENV'] = 'production'
    else:
        # Original mode may only run the exact backed-up M3 source.
        for p, item in manifest['copied'].items():
            if sha(Path(p)) != item['sha256']:
                raise RuntimeError()
    # Check the real listener before any Store constructor can migrate the DB.
    with socket.socket() as probe:
        probe.settimeout(1)
        if probe.connect_ex(('127.0.0.1', 8787)) == 0:
            raise RuntimeError()
    if not (LIVE / 'src/server.mjs').is_file():
        raise RuntimeError()
    ensure_stopped()
    os.umask(0o077)
    os.chdir(LIVE)
    os.execve(runtime['exe'], [runtime['exe'], 'src/server.mjs'], env)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup_directory')
    parser.add_argument('--mode', required=True, choices=['production', 'original-environment'])
    parser.add_argument('--require-revocation', action='store_true')
    args = parser.parse_args()
    try:
        launch(args.backup_directory, args.mode, args.require_revocation)
    except Exception:
        print('Launch refused or failed. Check private paths, occupied port and reviewed source; no credentials printed.')
        raise SystemExit(1)
