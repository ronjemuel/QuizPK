#!/usr/bin/env python3
"""Read-only maintenance guards. Never stops a process or opens live SQLite."""
import argparse
import hashlib
import json
from pathlib import Path
import socket
import time
from m42_backend_backup import LIVE, ROOT, processes, durable_hashes


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify_receipt(backup, staging, require_revocation=False):
    result = json.loads((staging / 'result.json').read_text())
    if result['status'] != 'PASS' or not all(result['checks'].values()) or result['checkCount'] < 55 or result['checkCount'] != len(result['checks']):
        raise RuntimeError()
    if result.get('backupManifestSha256') != sha(backup / 'manifest.json') or result.get('releaseManifestSha256') != sha(staging / 'release-manifest.json'):
        raise RuntimeError()
    if result.get('stagingDirectory') != str(staging) or result.get('releaseDirectory') != str(staging / 'm41'):
        raise RuntimeError()
    if require_revocation and (result.get('revokedTokenVerified') is not True or not all(result['checks'].get(k) is True for k in ['exposedTokenRejectedM41','replacementRetainsPhone01Identity','exposedTokenRejectedM41Http','exposedTokenRejectedM3Rollback'])):
        raise RuntimeError()
    return result


def verify(backup, check_live_source=True, check_stopped_durable=False, require_revocation=False):
    if not backup.is_relative_to(ROOT) or backup.stat().st_mode & 0o077:
        raise RuntimeError()
    manifest = json.loads((backup / 'manifest.json').read_text())
    if not all(sha(backup / p) == h for p, h in manifest['files'].items()):
        raise RuntimeError()
    if check_live_source and not all(sha(Path(p)) == v['sha256'] for p, v in manifest['copied'].items()):
        raise RuntimeError()  # Source/config drift needs a new review, never an overwrite.
    if check_stopped_durable:
        if manifest.get('liveDurableBytesEqual') is not True or durable_hashes(LIVE / 'data/quizpk.sqlite') != manifest['liveDurableAfter']:
            raise RuntimeError()  # No stale staging approval after stopped DB drift.
    staging = Path(str(backup) + '-staging')
    verify_receipt(backup, staging, require_revocation)
    release = json.loads((staging / 'release-manifest.json').read_text())
    if not all(sha(staging / 'm41' / p) == h for p, h in release.items()):
        raise RuntimeError()
    if sha(LIVE / 'package.json') != release['package.json']:
        raise RuntimeError()  # This deployment needs no package/dependency replacement.
    return json.loads((backup / 'runtime.json').read_text())


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('backup_directory')
    parser.add_argument('--pid', action='store_true')
    parser.add_argument('--stopped', action='store_true')
    parser.add_argument('--current-pid', action='store_true', help='Identify only this backend after deployment; source-independent')
    parser.add_argument('--wait-stopped', action='store_true', help='Source-independent shutdown check for rollback')
    parser.add_argument('--require-revocation', action='store_true', help='Require rotated-phone01 and exposed-token rejection proof')
    args = parser.parse_args()
    try:
        runtime = verify(Path(args.backup_directory).resolve(strict=True), not (args.current_pid or args.wait_stopped), args.stopped, args.require_revocation)
        if sum([args.pid, args.stopped, args.current_pid, args.wait_stopped]) > 1:
            raise RuntimeError()
        if args.stopped or args.wait_stopped:
            deadline = time.monotonic() + 60
            while processes() and time.monotonic() < deadline:
                time.sleep(0.5)
            if processes():
                raise RuntimeError()
            with socket.socket() as probe:
                probe.settimeout(1)
                if probe.connect_ex(('127.0.0.1', 8787)) == 0:
                    raise RuntimeError()
            # Another SQLite owner is also a reason to stop, without killing it.
            for p in Path('/proc').glob('[0-9]*/fd/*'):
                try:
                    if p.resolve().as_posix().startswith(str(LIVE / 'data/quizpk.sqlite')):
                        raise RuntimeError()
                except OSError:
                    pass
            print('Backend stopped; port free; no observable database owner. Backup/rehearsal verified.')
        else:
            running = processes()
            if len(running) != 1:
                raise RuntimeError()
            if not args.current_pid and (running[0]['pid'] != runtime['pid'] or running[0]['starttime'] != runtime['starttime']):
                raise RuntimeError()
            if args.pid or args.current_pid:
                print(running[0]['pid'])
            else:
                print('Preflight, frozen release, existing source/config and running process verified.')
    except Exception:
        print('Guard refused. Re-run private preflight after any process/source/config change; no live change performed.')
        raise SystemExit(1)
