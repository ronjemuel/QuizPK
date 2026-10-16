#!/usr/bin/env python3
"""M4.3 guards and future launch. Only 'launch' starts a backend; never run it in preparation.

No live SQLite connection or credential output. All guards fail closed.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import time
import m43_backend_backup as backup_tool

LIVE = backup_tool.LIVE
ROOT = backup_tool.ROOT
REQUIRED = {
    'migrationAllExistingRowsEqual', 'migrationLedgerAppendOnly',
    'migrationAllExistingSchemaEqual', 'migrationUserVersionEqual',
    'migrationSchemaTwo', 'migrationIntegrity', 'rotatedTokenStillCurrent',
    'exposedTokenHttpRejected', 'currentTokensHttpCompatible', 'savedAiJobsReplay',
    'originalAiHttpCacheReplay', 'oldLobbyShapeCompatible', 'hostStart',
    'unrevealedAnswerKeyOmitted', 'exactAnswerRetry', 'changedAnswerRejected',
    'authoritativeScoring', 'automaticFinalResults', 'completedResultsDurable',
    'disabledFallbackInterruptsSafely', 'restartDoesNotGradeUnrevealed',
    'disabledFallbackOrdinaryClassicLobby', 'disabledFallbackStartRejected',
    'fallbackExposedTokenRejected', 'allOriginalUsersPreserved',
    'allOriginalJobsPreserved', 'fallbackKeepsPostUpgradeSavedJob',
    'fallbackAllSavedJobsCount', 'oldSourceRefusesSchemaTwo',
    'migrationFailureAtomic', 'termuxProductionEntrypoint',
    'entrypointReadsDisabledEnvironmentFlag', 'entrypointProductionHttps',
    'termuxGracefulShutdown', 'providerNeverCalled', 'backupStillIntact', 'releaseStillIntact'
}


def require(value):
    if not value:
        raise RuntimeError('GUARD_REFUSED')


def sha(p):
    return hashlib.sha256(Path(p).read_bytes()).hexdigest()


def private(p):
    p = Path(p)
    require(p.is_absolute() and p.resolve(strict=True) == p and p.is_relative_to(ROOT))
    require(not p.stat().st_mode & 0o077)
    return p


def read(p):
    return json.loads(private(p).read_text())


def tree(p):
    require(p.is_dir() and not p.is_symlink())
    out = {}
    for f in p.rglob('*'):
        require(not f.is_symlink())
        if f.is_file():
            out[str(f.relative_to(p))] = sha(f)
    return out


def verify_backup(p):
    p = private(p)
    m = read(p / 'manifest.json')
    require(m['format'] == 2 and all(v is True for v in m['checks'].values()))
    require(set(m['checks']) == {'snapshotIntegrity', 'snapshotDataEqual',
                               'standaloneRestoreEqual', 'configCopiesVerified'})
    for name, h in m['files'].items():
        f = private(p / name)
        require(f.is_relative_to(p) and sha(f) == h)
    require(sha(p / 'quizpk.sqlite') == m['snapshotSha256'])
    require(m['inventory']['multiplayerVersion'] in (1, 2))
    return m, read(p / 'runtime.json')


def verify_release(prep):
    prep = private(prep)
    manifest = read(prep / 'release-manifest.json')
    require(tree(prep / 'release') == manifest)
    require(tree(prep / 'protocol') == read(prep / 'protocol-manifest.json'))
    context = read(prep / 'context.json')
    require(sha(context['nodeExecutable']) == context['nodeSha256'])
    for key in ('revokedTokenFile', 'rotationTokenFile'):
        require(sha(private(context[key])) == context[key + 'Sha256'])
    return manifest, context


def verify_receipt(backup, prep):
    staging = Path(str(backup) + '-m43-staging')
    r = read(staging / 'result.json')
    require(r['status'] == 'PASS' and r['providerCalls'] == 0)
    require(r['checkCount'] == len(r['checks']) and REQUIRED.issubset(r['checks']))
    require(all(v is True for v in r['checks'].values()))
    require(r['backupDirectory'] == str(backup) and r['preparationDirectory'] == str(prep)
            and r['stagingDirectory'] == str(staging))
    require(r['backupManifestSha256'] == sha(backup / 'manifest.json'))
    require(r['releaseManifestSha256'] == sha(prep / 'release-manifest.json'))
    require(r['contextSha256'] == sha(prep / 'context.json'))
    require(r['protocolManifestSha256'] == sha(prep / 'protocol-manifest.json'))
    require(r['rehearsalScriptSha256'] == sha(Path(__file__).with_name('m43_backend_rehearsal.mjs')))
    return r


def current_inputs(m, prep, source):
    release, _ = verify_release(prep)
    expected = {k[4:]:v for k, v in release.items() if k.startswith('src/')}
    if source == 'baseline':
        expected = {str(Path(k).relative_to(LIVE / 'src')):v['sha256']
                    for k, v in m['copied'].items() if Path(k).is_relative_to(LIVE / 'src')}
    require(tree(LIVE / 'src') == expected)
    for name, item in m['copied'].items():
        if not Path(name).is_relative_to(LIVE / 'src'):
            require(not Path(name).is_symlink() and sha(name) == item['sha256'])
    expected_private = {str(Path(k).relative_to(LIVE / 'private')):v['sha256']
                        for k, v in m['copied'].items() if Path(k).is_relative_to(LIVE / 'private')}
    require(tree(LIVE / 'private') == expected_private)
    require(sha(LIVE / 'package.json') == release['package.json'])


def verified(backup, prep, source='baseline', reference=None):
    backup, prep = private(backup), private(prep)
    m, runtime = verify_backup(backup)
    verify_release(prep)
    reference = private(reference) if reference else backup
    if reference != backup:
        ref, _ = verify_backup(reference)
        require(ref['inventory']['multiplayerVersion'] == 1)
    verify_receipt(reference, prep)
    current_inputs(m, prep, source)
    require(runtime['dbPath'] == str(LIVE / 'data/quizpk.sqlite'))
    require(runtime['exe'] == read(prep / 'context.json')['nodeExecutable'])
    return m, runtime


def running(backup, prep, source='baseline', current=False):
    _, runtime = verified(backup, prep, source)
    found = backup_tool.processes()
    require(len(found) == 1)
    r = found[0]
    require(r['exe'] == runtime['exe'])
    require((LIVE / r['env'].get('QUIZPK_DB_PATH', './data/quizpk.sqlite')).resolve()
            == LIVE / 'data/quizpk.sqlite')
    require(r['env'].get('HOST', '127.0.0.1') == '127.0.0.1'
            and r['env'].get('PORT', '8787') == '8787'
            and r['env'].get('NODE_ENV') == 'production')
    if not current:
        require((r['pid'], r['starttime']) == (runtime['pid'], runtime['starttime']))
        require(r['env'] == {k:v for k, v in runtime['env'].items()})
    return r['pid']


def stopped(backup, prep, source='baseline', durable=False, reference=None):
    m, runtime = verified(backup, prep, source, reference)
    backup_tool.ensure_stopped()
    if durable:
        require(m['sourceMode'] == 'stopped-verified-private-copy')
        require(m['liveDurableBytesEqual'] is True)
        require(backup_tool.durable_hashes(LIVE / 'data/quizpk.sqlite') == m['liveDurableAfter'])
    return m, runtime


def launch(backup, prep, mode, reference=None):
    # Only called by the explicitly named launch subcommand after a separate approval.
    source = 'baseline' if mode == 'm42-recovery' else 'release'
    m, runtime = stopped(backup, prep, source, True, reference)
    schema_version = m['inventory']['multiplayerVersion']
    require(schema_version == 1 if mode in ('enabled', 'm42-recovery') else schema_version == 2)
    require(not reference if mode != 'disabled' else reference is not None)
    db = LIVE / 'data/quizpk.sqlite'
    require(db.is_file() and not db.is_symlink() and db.stat().st_size > 0)
    env = dict(runtime['env'])
    env.update(QUIZPK_DB_PATH=str(db), HOST='127.0.0.1', PORT='8787', NODE_ENV='production')
    if mode != 'm42-recovery':
        env['QUIZPK_CLASSIC_ENABLED'] = 'true' if mode == 'enabled' else 'false'
    backup_tool.ensure_stopped()
    os.umask(0o077)
    os.chdir(LIVE)
    os.execve(runtime['exe'], [runtime['exe'], 'src/server.mjs'], env)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['verify', 'pid', 'current-pid', 'wait-stopped', 'launch'])
    parser.add_argument('backup_directory')
    parser.add_argument('preparation_directory')
    parser.add_argument('--source', choices=['baseline', 'release'], default='baseline')
    parser.add_argument('--stopped-durable', action='store_true')
    parser.add_argument('--mode', choices=['enabled', 'disabled', 'm42-recovery'])
    parser.add_argument('--reference', help='Original schema-1 maintenance window for tested schema-2 fallback')
    args = parser.parse_args()
    try:
        backup, prep = Path(args.backup_directory), Path(args.preparation_directory)
        if args.action == 'launch':
            require(args.mode is not None)
            launch(backup, prep, args.mode, args.reference)
        elif args.action in ('pid', 'current-pid'):
            require(not args.reference and not args.stopped_durable)
            print(running(backup, prep, args.source, args.action == 'current-pid'))
        elif args.action == 'wait-stopped':
            # Wait only for this backend, never signal or kill anything.
            deadline = time.monotonic() + 55
            while backup_tool.processes() and time.monotonic() < deadline:
                time.sleep(.5)
            stopped(backup, prep, args.source, args.stopped_durable, args.reference)
            print('PASS: stopped, free port, no observable SQLite owner; all applicable guards passed.')
        elif args.stopped_durable:
            stopped(backup, prep, args.source, True, args.reference)
            print('PASS: exact stopped DB/WAL, source/config, release and bound rehearsal verified.')
        else:
            require(not args.reference)
            running(backup, prep, args.source)
            print('PASS: backup, release, rehearsal, source/config and original running process verified.')
    except Exception:
        print('M4.3 guard refused; preserve evidence and investigate privately. No credentials printed.')
        raise SystemExit(1)
