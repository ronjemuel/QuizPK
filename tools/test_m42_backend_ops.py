"""Maintenance safety tests with synthetic files and mocked exec; no live access."""
import contextlib
import hashlib
import io
import json
from pathlib import Path
import sqlite3
import tempfile
import unittest
import subprocess
import sys
from unittest.mock import patch
import m42_backend_backup as backup_tool
import m42_backend_guard as guard
import m42_backend_launch as launcher


def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()


def write(p, value):
    p.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    p.write_text(json.dumps(value) if isinstance(value, dict) else value)
    p.chmod(0o600)


class OpsTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='quizpk-ops-synthetic-')
        self.root = Path(self.tmp.name).resolve()
        self.live = self.root / 'live'
        self.backups = self.root / 'backups'
        self.backups.mkdir(mode=0o700)
        self.before = self.backups / 'before'
        self.before.mkdir(parents=True, mode=0o700)
        self.stage = Path(str(self.before) + '-staging')
        self.runtime = {'pid': 999999, 'starttime': '1', 'exe': '/synthetic/node',
                        'env': {'GEMINI_MODEL': 'synthetic', 'GEMINI_FREE_TIER_CONFIRMED': 'true',
                                'SYNTHETIC_SECRET': 'never-print-this-fixture'}}
        for p, data in [(self.live / 'src/server.mjs', 'synthetic source'),
                        (self.live / 'package.json', '{}'),
                        (self.live / 'GEMINI_API_KEY.env', 'synthetic private config'),
                        (self.live / 'data/quizpk.sqlite', 'synthetic nonempty file')]:
            write(p, data)
        write(self.before / 'runtime.json', self.runtime)
        original = [self.live / 'src/server.mjs', self.live / 'package.json', self.live / 'GEMINI_API_KEY.env']
        self.manifest = {'files': {'runtime.json': sha(self.before / 'runtime.json')},
                         'copied': {str(p): {'sha256': sha(p)} for p in original},
                         'liveDurableBytesEqual': True,
                         'liveDurableAfter': {str(self.live / 'data/quizpk.sqlite'): sha(self.live / 'data/quizpk.sqlite')}}
        write(self.before / 'manifest.json', self.manifest)
        write(self.stage / 'result.json', {'status': 'PASS', 'checkCount': 55, 'checks': {str(i): True for i in range(55)}})
        self.release = {str(p.relative_to(self.live)): sha(p) for p in original[:2]}
        write(self.stage / 'release-manifest.json', self.release)
        for p in original[:2]:
            write(self.stage / 'm41' / p.relative_to(self.live), p.read_text())
        result = json.loads((self.stage / 'result.json').read_text())
        result.update(backupManifestSha256=sha(self.before / 'manifest.json'),
                      releaseManifestSha256=sha(self.stage / 'release-manifest.json'),
                      stagingDirectory=str(self.stage), releaseDirectory=str(self.stage / 'm41'))
        write(self.stage / 'result.json', result)
        self.patches = [patch.object(m, name, value) for m in [guard, launcher, backup_tool]
                        for name, value in [('LIVE', self.live), ('ROOT', self.backups)]]
        for p in self.patches:
            p.start()
        # Synthetic maintenance fixtures must not probe the real phone listener.
        # Production ensure_stopped remains intact; model a vacant test port.
        socket_patch = patch.object(backup_tool.socket, 'socket')
        self.synthetic_socket = socket_patch.start()
        self.synthetic_socket.return_value.__enter__.return_value.connect_ex.return_value = 111
        self.addCleanup(socket_patch.stop)
        self.addCleanup(self.tmp.cleanup)
        self.addCleanup(lambda: [p.stop() for p in reversed(self.patches)])

    def launch(self, mode='production', occupied=False):
        with patch.object(launcher.os, 'execve') as execute, patch.object(launcher.os, 'chdir'), patch.object(launcher.os, 'umask'), patch.object(launcher.socket, 'socket') as sock:
            sock.return_value.__enter__.return_value.connect_ex.return_value = 0 if occupied else 111
            launcher.launch(self.before, mode)
            return execute.call_args

    def test_production_uses_captured_environment_without_secret_arguments(self):
        args = self.launch().args
        self.assertEqual(args[0], '/synthetic/node')
        self.assertEqual(args[1], ['/synthetic/node', 'src/server.mjs'])
        self.assertEqual(args[2]['NODE_ENV'], 'production')
        self.assertEqual(args[2]['QUIZPK_DB_PATH'], str(self.live / 'data/quizpk.sqlite'))
        self.assertEqual(args[2]['SYNTHETIC_SECRET'], self.runtime['env']['SYNTHETIC_SECRET'])

    def test_original_environment_is_exact_m3_recovery(self):
        args = self.launch('original-environment').args
        self.assertNotIn('NODE_ENV', args[2])
        write(self.live / 'src/server.mjs', 'different source')
        with self.assertRaises(RuntimeError):
            self.launch('original-environment')

    def test_launch_rejects_missing_live_database(self):
        (self.live / 'data/quizpk.sqlite').unlink()
        with self.assertRaises(RuntimeError):
            self.launch()

    def test_launch_rejects_active_listener(self):
        with self.assertRaises(RuntimeError):
            self.launch(occupied=True)

    def test_launch_rejects_failed_or_incomplete_rehearsal(self):
        write(self.stage / 'result.json', {'status': 'FAIL', 'checkCount': 55, 'checks': {'fake': True}})
        with self.assertRaises(RuntimeError):
            self.launch()

    def test_guard_rejects_source_and_release_drift(self):
        guard.verify(self.before)
        write(self.live / 'src/server.mjs', 'changed after review')
        with self.assertRaises(RuntimeError):
            guard.verify(self.before)
        guard.verify(self.before, check_live_source=False)
        write(self.stage / 'm41/src/server.mjs', 'changed release')
        with self.assertRaises(RuntimeError):
            guard.verify(self.before, check_live_source=False)

    def test_backup_or_config_tamper_prevents_start(self):
        write(self.before / 'runtime.json', {'env': {}})
        with self.assertRaises(RuntimeError):
            self.launch()

    def test_online_backup_includes_committed_wal_and_ignores_uncommitted_data(self):
        db_path = self.live / 'data/quizpk.sqlite'
        db_path.unlink()
        db = sqlite3.connect(db_path)
        self.addCleanup(db.close)
        db.executescript('PRAGMA journal_mode=WAL; CREATE TABLE users(id TEXT PRIMARY KEY, token_hash TEXT, enabled INTEGER); CREATE TABLE jobs(user_id TEXT, key TEXT, fingerprint TEXT, created INTEGER, response TEXT, status INTEGER); CREATE TABLE throttle(key TEXT, window INTEGER, count INTEGER); PRAGMA user_version=7;')
        db.execute('INSERT INTO users VALUES (?,?,1)', ('fixture', 'fixture-hash'))
        db.commit()
        db.execute('INSERT INTO users VALUES (?,?,1)', ('uncommitted-fixture', 'other-fixture-hash'))
        write(self.live / 'private/fixture.token', 'synthetic-token')
        runtime = {**self.runtime, 'env': {'QUIZPK_DB_PATH': str(db_path)}}
        output = io.StringIO()
        with patch.object(backup_tool, 'processes', return_value=[runtime]), contextlib.redirect_stdout(output):
            backup_tool.backup('preflight')
        result = json.loads(output.getvalue())
        restored = Path(result['backupDirectory']) / 'quizpk.sqlite'
        ro = backup_tool.readonly(restored)
        self.addCleanup(ro.close)
        self.assertEqual(ro.execute('SELECT count(*) FROM users').fetchone()[0], 1)
        self.assertEqual(ro.execute('PRAGMA user_version').fetchone()[0], 7)
        self.assertEqual(ro.execute('PRAGMA integrity_check').fetchone()[0], 'ok')
        self.assertFalse(Path(str(restored) + '-wal').exists())
        self.assertTrue(result['liveDurableBytesEqual'])

    def test_stopped_backup_never_creates_live_wal(self):
        db_path = self.live / 'data/quizpk.sqlite'; db_path.unlink()
        db = sqlite3.connect(db_path)
        db.executescript('PRAGMA journal_mode=WAL; CREATE TABLE users(id TEXT,token_hash TEXT,enabled INTEGER); CREATE TABLE jobs(user_id TEXT,key TEXT,fingerprint TEXT,created INTEGER,response TEXT,status INTEGER); CREATE TABLE throttle(key TEXT,window INTEGER,count INTEGER); INSERT INTO users VALUES("synthetic","hash",1);')
        db.close()
        self.assertFalse(Path(str(db_path) + '-wal').exists())
        output = io.StringIO()
        with patch.object(backup_tool, 'processes', return_value=[]), contextlib.redirect_stdout(output):
            backup_tool.backup('maintenance', str(self.before))
        result = json.loads(output.getvalue())
        self.assertTrue(result['liveDurableBytesEqual'])
        self.assertFalse(Path(str(db_path) + '-wal').exists())
        self.assertFalse(Path(str(db_path) + '-shm').exists())

    def test_stopped_backup_includes_nonempty_orphan_wal(self):
        db_path = self.live / 'data/quizpk.sqlite'; db_path.unlink()
        script = '''import sqlite3,os,sys
db=sqlite3.connect(sys.argv[1])
db.executescript('PRAGMA journal_mode=WAL; CREATE TABLE users(id TEXT,token_hash TEXT,enabled INTEGER); CREATE TABLE jobs(user_id TEXT,key TEXT,fingerprint TEXT,created INTEGER,response TEXT,status INTEGER); CREATE TABLE throttle(key TEXT,window INTEGER,count INTEGER); INSERT INTO users VALUES("wal-only-synthetic","hash",1);')
os._exit(0)
'''
        subprocess.run([sys.executable, '-c', script, str(db_path)], check=True)
        self.assertGreater(Path(str(db_path) + '-wal').stat().st_size, 0)
        before = backup_tool.durable_hashes(db_path)
        output = io.StringIO()
        with patch.object(backup_tool, 'processes', return_value=[]), contextlib.redirect_stdout(output):
            backup_tool.backup('maintenance', str(self.before))
        result = json.loads(output.getvalue())
        db = backup_tool.readonly(Path(result['backupDirectory']) / 'quizpk.sqlite')
        try: self.assertEqual(db.execute('SELECT count(*) FROM users').fetchone()[0], 1)
        finally: db.close()
        self.assertEqual(backup_tool.durable_hashes(db_path), before)

    def test_stopped_guard_refuses_db_drift_after_rehearsal(self):
        guard.verify(self.before, check_stopped_durable=True)
        with (self.live / 'data/quizpk.sqlite').open('a') as f: f.write('synthetic drift')
        with self.assertRaises(RuntimeError): guard.verify(self.before, check_stopped_durable=True)

    def test_rehearsal_from_another_backup_is_rejected(self):
        r = json.loads((self.stage / 'result.json').read_text()); r['backupManifestSha256'] = 'synthetic-wrong-binding'
        write(self.stage / 'result.json', r)
        with self.assertRaises(RuntimeError): guard.verify(self.before)

    def test_required_revocation_receipt_cannot_be_omitted(self):
        with self.assertRaises(RuntimeError): guard.verify(self.before, require_revocation=True)

    def test_recovery_launch_requires_requested_revocation_proof(self):
        with self.assertRaises(RuntimeError): launcher.launch(self.before, 'original-environment', True)


if __name__ == '__main__':
    unittest.main()
