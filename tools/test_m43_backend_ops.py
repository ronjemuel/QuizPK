"""Synthetic M4.3 maintenance safety tests. No real host, listener or credentials."""
import contextlib
import io
import json
import os
from pathlib import Path
import shutil
import sqlite3
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import m43_backend_backup as b
import m43_backend_ops as ops
import m43_verify_snapshot as snapshot_tool


def write(p, value):
    p.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    p.write_text(json.dumps(value) if isinstance(value, dict) else value)
    p.chmod(0o600)


class BackupTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='quizpk-m43-synthetic-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.live = self.root / 'host'
        self.backups = self.root / 'backups'
        self.backups.mkdir(mode=0o700)
        self.dbpath = self.live / 'data/quizpk.sqlite'
        self.dbpath.parent.mkdir(parents=True)
        for name in ('package.json', 'src/server.mjs', 'GEMINI_API_KEY.env', 'private/fixture.token'):
            write(self.live / name, 'synthetic')
        self.runtime = {'pid': 999999, 'starttime': '1', 'exe': '/synthetic/node', 'env': {}}
        self.stack = contextlib.ExitStack()
        self.addCleanup(self.stack.close)
        self.stack.enter_context(patch.object(b, 'LIVE', self.live))
        self.stack.enter_context(patch.object(b, 'ROOT', self.backups))
        self.stack.enter_context(patch.object(b, 'processes', return_value=[self.runtime]))
        sock = self.stack.enter_context(patch.object(b.socket, 'socket'))
        sock.return_value.__enter__.return_value.connect_ex.return_value = 111
        db = sqlite3.connect(self.dbpath)
        db.executescript('''PRAGMA journal_mode=WAL; PRAGMA user_version=7;
          CREATE TABLE users(id TEXT, token_hash TEXT, enabled INTEGER);
          CREATE TABLE jobs(user_id TEXT, response TEXT);
          CREATE TABLE throttle(key TEXT,count INTEGER);
          CREATE TABLE mp_schema(version INTEGER,applied_at INTEGER);
          CREATE TABLE mp_rooms(id TEXT);
          CREATE TABLE mp_members(room_id TEXT,user_id TEXT);
          CREATE TABLE mp_requests(request_id TEXT);
          CREATE TABLE mp_limits(key TEXT,count INTEGER);
          INSERT INTO users VALUES('fixture','fixture-hash',1);
          INSERT INTO jobs VALUES('fixture','saved');
          INSERT INTO mp_schema VALUES(1,1);
          INSERT INTO mp_rooms VALUES('fixture-room');''')
        db.close()

    def backup(self, stopped=False):
        seed = self.backups / 'seed'
        if stopped:
            write(seed / 'runtime.json', self.runtime)
            write(seed / 'manifest.json', {'files': {'runtime.json': ops.sha(seed / 'runtime.json')}})
        output = io.StringIO()
        with contextlib.redirect_stdout(output), patch.object(b, 'processes', return_value=[] if stopped else [self.runtime]):
            b.backup('maintenance' if stopped else 'preflight', str(seed) if stopped else None)
        result = json.loads(output.getvalue())
        self.assertTrue(result['snapshotDataEqual'])
        return Path(result['backupDirectory'])

    def test_online_snapshot_includes_committed_wal_excludes_uncommitted_rows(self):
        db = sqlite3.connect(self.dbpath)
        self.addCleanup(db.close)
        db.execute("INSERT INTO mp_members VALUES('fixture-room','fixture')")
        db.commit()
        db.execute("INSERT INTO users VALUES('uncommitted','other',1)")
        p = self.backup()
        with contextlib.closing(b.readonly(p / 'quizpk.sqlite')) as copy:
            self.assertEqual(copy.execute('SELECT count(*) FROM users').fetchone()[0], 1)
            self.assertEqual(copy.execute('SELECT count(*) FROM mp_members').fetchone()[0], 1)
            self.assertEqual(copy.execute('PRAGMA user_version').fetchone()[0], 7)

    def test_stopped_snapshot_does_not_create_live_sidecars(self):
        before = b.durable_hashes(self.dbpath)
        p = self.backup(True)
        self.assertEqual(before, b.durable_hashes(self.dbpath))
        self.assertFalse(Path(str(self.dbpath) + '-wal').exists())
        self.assertTrue(json.loads((p / 'manifest.json').read_text())['liveDurableBytesEqual'])

    def test_stopped_orphan_wal_is_included(self):
        subprocess.run([sys.executable, '-c', 'import sqlite3,os,sys;d=sqlite3.connect(sys.argv[1]);d.execute("INSERT INTO mp_members VALUES(?,?)",("room","fixture"));d.commit();os._exit(0)', str(self.dbpath)], check=True)
        self.assertGreater(Path(str(self.dbpath) + '-wal').stat().st_size, 0)
        before = b.durable_hashes(self.dbpath)
        p = self.backup(True)
        self.assertEqual(before, b.durable_hashes(self.dbpath))
        with contextlib.closing(b.readonly(p / 'quizpk.sqlite')) as copy:
            self.assertEqual(copy.execute('SELECT count(*) FROM mp_members').fetchone()[0], 1)

    def test_schema_two_can_be_backed_up_for_fallback(self):
        with contextlib.closing(sqlite3.connect(self.dbpath)) as db:
            db.execute('INSERT INTO mp_schema VALUES(2,2)')
            db.commit()
        p = self.backup(True)
        self.assertEqual(json.loads((p / 'manifest.json').read_text())['inventory']['multiplayerVersion'], 2)

    def test_active_listener_prevents_stopped_backup(self):
        with patch.object(b, 'processes', return_value=[]), patch.object(b.socket, 'socket') as sock:
            sock.return_value.__enter__.return_value.connect_ex.return_value = 0
            with self.assertRaisesRegex(RuntimeError, 'LISTENER_PRESENT'):
                b.ensure_stopped()


class GuardTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='quizpk-m43-guards-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.live = self.root / 'host'; self.prep = self.root / 'prep'; self.backup = self.root / 'backup'
        self.stack = contextlib.ExitStack(); self.addCleanup(self.stack.close)
        for module in (ops, b):
            self.stack.enter_context(patch.object(module, 'LIVE', self.live))
            self.stack.enter_context(patch.object(module, 'ROOT', self.root))
        write(self.root / 'node', 'synthetic executable')
        write(self.root / 'revoked', 'synthetic old token')
        write(self.root / 'rotated', 'synthetic new token')
        for name, value in [('src/server.mjs', 'old'), ('package.json', '{}'), ('private/fixture.token', 'synthetic token'), ('GEMINI_API_KEY.env', 'synthetic private config'), ('data/quizpk.sqlite', 'synthetic DB')]:
            write(self.live / name, value)
        write(self.prep / 'release/src/server.mjs', 'new')
        write(self.prep / 'release/package.json', '{}')
        write(self.prep / 'protocol/fixture.json', '{}')
        write(self.prep / 'release-manifest.json', ops.tree(self.prep / 'release'))
        write(self.prep / 'protocol-manifest.json', ops.tree(self.prep / 'protocol'))
        write(self.prep / 'context.json', {'nodeExecutable': str(self.root / 'node'), 'nodeSha256': ops.sha(self.root / 'node'), 'revokedTokenFile': str(self.root / 'revoked'), 'revokedTokenFileSha256': ops.sha(self.root / 'revoked'), 'rotationTokenFile': str(self.root / 'rotated'), 'rotationTokenFileSha256': ops.sha(self.root / 'rotated')})
        self.runtime = {'pid': 999999, 'starttime': '1', 'exe': str(self.root / 'node'), 'dbPath': str(self.live / 'data/quizpk.sqlite'), 'env': {'NODE_ENV': 'production', 'SYNTHETIC_SECRET': 'never-print'}}
        write(self.backup / 'runtime.json', self.runtime)
        write(self.backup / 'quizpk.sqlite', 'synthetic backup')
        copied = {str(p): {'sha256': ops.sha(p)} for p in self.live.rglob('*') if p.is_file() and not p.is_relative_to(self.live / 'data')}
        self.manifest = {'format': 2, 'checks': dict.fromkeys(('snapshotIntegrity', 'snapshotDataEqual', 'standaloneRestoreEqual', 'configCopiesVerified'), True), 'files': {n: ops.sha(self.backup / n) for n in ['runtime.json', 'quizpk.sqlite']}, 'copied': copied, 'snapshotSha256': ops.sha(self.backup / 'quizpk.sqlite'), 'inventory': {'multiplayerVersion': 1}, 'sourceMode': 'stopped-verified-private-copy', 'liveDurableBytesEqual': True, 'liveDurableAfter': b.durable_hashes(self.live / 'data/quizpk.sqlite')}
        self.save()
        self.stage = Path(str(self.backup) + '-m43-staging')
        self.receipt = {'status': 'PASS', 'providerCalls': 0, 'checks': dict.fromkeys(ops.REQUIRED, True), 'checkCount': len(ops.REQUIRED), 'backupDirectory': str(self.backup), 'preparationDirectory': str(self.prep), 'stagingDirectory': str(self.stage), 'backupManifestSha256': ops.sha(self.backup / 'manifest.json'), 'releaseManifestSha256': ops.sha(self.prep / 'release-manifest.json'), 'contextSha256': ops.sha(self.prep / 'context.json'), 'protocolManifestSha256': ops.sha(self.prep / 'protocol-manifest.json'), 'rehearsalScriptSha256': ops.sha(Path(ops.__file__).with_name('m43_backend_rehearsal.mjs'))}
        write(self.stage / 'result.json', self.receipt)
        self.stack.enter_context(patch.object(b, 'ensure_stopped'))

    def save(self):
        write(self.backup / 'manifest.json', self.manifest)

    def test_valid_baseline_guard(self):
        ops.verified(self.backup, self.prep)

    def test_wrong_backup_receipt_rejected(self):
        self.receipt['backupManifestSha256'] = 'wrong'; write(self.stage / 'result.json', self.receipt)
        with self.assertRaises(RuntimeError): ops.verified(self.backup, self.prep)

    def test_missing_revocation_check_rejected(self):
        del self.receipt['checks']['exposedTokenHttpRejected']; write(self.stage / 'result.json', self.receipt)
        with self.assertRaises(RuntimeError): ops.verified(self.backup, self.prep)

    def test_source_and_private_file_drift_rejected(self):
        write(self.live / 'private/fixture.token', 'changed')
        with self.assertRaises(RuntimeError): ops.verified(self.backup, self.prep)

    def test_extra_release_file_rejected(self):
        write(self.prep / 'release/src/unreviewed.mjs', 'extra')
        with self.assertRaises(RuntimeError): ops.verified(self.backup, self.prep)

    def test_durable_drift_rejects_first_launch(self):
        write(self.live / 'src/server.mjs', 'new'); write(self.live / 'data/quizpk.sqlite', 'new write')
        with self.assertRaises(RuntimeError), patch.object(ops.os, 'execve') as execute:
            ops.launch(self.backup, self.prep, 'enabled')
        execute.assert_not_called()

    def test_enabled_launch_keeps_secrets_out_of_arguments(self):
        write(self.live / 'src/server.mjs', 'new')
        with patch.object(ops.os, 'execve') as execute, patch.object(ops.os, 'chdir'), patch.object(ops.os, 'umask'):
            ops.launch(self.backup, self.prep, 'enabled')
        args = execute.call_args.args
        self.assertEqual(args[1], [self.runtime['exe'], 'src/server.mjs'])
        self.assertEqual(args[2]['QUIZPK_CLASSIC_ENABLED'], 'true')
        self.assertEqual(args[2]['SYNTHETIC_SECRET'], 'never-print')
        self.assertEqual(args[2]['QUIZPK_DB_PATH'], str(self.live / 'data/quizpk.sqlite'))

    def test_changed_running_identity_rejected(self):
        with patch.object(b, 'processes', return_value=[{**self.runtime, 'starttime': 'changed'}]):
            with self.assertRaises(RuntimeError): ops.running(self.backup, self.prep)

    def test_source_downgrade_cannot_start_schema_two(self):
        self.manifest['inventory']['multiplayerVersion'] = 2; self.save()
        self.receipt['backupManifestSha256'] = ops.sha(self.backup / 'manifest.json'); write(self.stage / 'result.json', self.receipt)
        with self.assertRaises(RuntimeError), patch.object(ops.os, 'execve') as execute:
            ops.launch(self.backup, self.prep, 'm42-recovery')
        execute.assert_not_called()

    def test_fallback_keeps_current_database_and_uses_disabled_flag(self):
        write(self.live / 'src/server.mjs', 'new')
        reference = self.backup
        current = self.root / 'fallback'; shutil.copytree(reference, current)
        m = json.loads((current / 'manifest.json').read_text()); m['inventory']['multiplayerVersion'] = 2
        write(current / 'manifest.json', m)
        with patch.object(ops.os, 'execve') as execute, patch.object(ops.os, 'chdir'), patch.object(ops.os, 'umask'):
            ops.launch(current, self.prep, 'disabled', reference)
        self.assertEqual(execute.call_args.args[2]['QUIZPK_CLASSIC_ENABLED'], 'false')
        self.assertEqual(execute.call_args.args[2]['QUIZPK_DB_PATH'], str(self.live / 'data/quizpk.sqlite'))


class SnapshotTests(unittest.TestCase):
    def test_private_snapshot_validation_preserves_data_and_rejects_job_drift(self):
        with tempfile.TemporaryDirectory(prefix='quizpk-m43-snapshot-') as temp:
            root = Path(temp); before = root / 'before'; after = root / 'after'; prep = root / 'prep'
            before.mkdir(); after.mkdir(); prep.mkdir()
            replacement = 'synthetic-current-token'; exposed = 'synthetic-exposed-token'
            write(prep / 'current.token', replacement); write(prep / 'exposed.token', exposed)
            write(after / 'host/private/phone01.token', replacement)
            db = sqlite3.connect(before / 'quizpk.sqlite')
            db.executescript('CREATE TABLE users(id TEXT,token_hash TEXT,enabled INTEGER); CREATE TABLE jobs(user_id TEXT,key TEXT,response TEXT);')
            import hashlib
            db.execute('INSERT INTO users VALUES(?,?,1)', ('phone01', hashlib.sha256(replacement.encode()).hexdigest()))
            db.execute('INSERT INTO jobs VALUES(?,?,?)', ('phone01', 'saved', 'private synthetic response'))
            db.commit(); db.close()
            shutil.copyfile(before / 'quizpk.sqlite', after / 'quizpk.sqlite')
            def manifest(path):
                return ({'inventory': {'multiplayerVersion': 1 if path == before else 2}}, {})
            with patch.object(snapshot_tool, 'private', side_effect=lambda p: p), patch.object(snapshot_tool, 'verify_backup', side_effect=manifest), patch.object(snapshot_tool, 'verify_release', return_value=({}, {'revokedTokenFile': str(prep / 'exposed.token'), 'rotationTokenFile': str(prep / 'current.token')})):
                self.assertTrue(all(snapshot_tool.verify(before, after, prep).values()))
                db = sqlite3.connect(after / 'quizpk.sqlite')
                db.execute('DELETE FROM jobs'); db.commit(); db.close()
                with self.assertRaises(RuntimeError): snapshot_tool.verify(before, after, prep)


if __name__ == '__main__':
    unittest.main()
