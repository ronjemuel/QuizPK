"""Synthetic credential-rotation failure/preservation tests; no real token/DB access."""
import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import shutil
import sqlite3
import tempfile
import unittest
from unittest.mock import patch
import m42_rotate_phone_token as rotate_tool


class RotationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='quizpk-rotation-synthetic-')
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name).resolve()
        self.live = self.root / 'live'; self.backups = self.root / 'backups'
        self.backup = self.backups / 'before'; self.backup.mkdir(parents=True, mode=0o700)
        (self.live / 'data').mkdir(parents=True); (self.live / 'private').mkdir()
        self.old = 'synthetic_phone01_' + '_' * 30
        self.old_hash = hashlib.sha256(self.old.encode()).hexdigest()
        self.owner_hash = hashlib.sha256(b'synthetic-owner').hexdigest()
        (self.live / 'private/phone01.token').write_text(self.old + '\n')
        db = sqlite3.connect(self.live / 'data/quizpk.sqlite')
        db.executescript('CREATE TABLE users(id TEXT PRIMARY KEY,token_hash TEXT UNIQUE,enabled INTEGER); CREATE TABLE jobs(user_id TEXT,key TEXT,fingerprint TEXT,created INTEGER,response TEXT,status INTEGER); CREATE TABLE throttle(key TEXT,window INTEGER,count INTEGER); PRAGMA user_version=7;')
        db.executemany('INSERT INTO users VALUES(?,?,1)', [('owner', self.owner_hash), ('phone01', self.old_hash)])
        db.execute('INSERT INTO jobs VALUES(?,?,?,?,?,?)', ('phone01', 'synthetic-job', 'synthetic-fingerprint', 1, 'synthetic saved data', 200))
        db.commit(); db.close()
        shutil.copyfile(self.live / 'data/quizpk.sqlite', self.backup / 'quizpk.sqlite')
        digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
        manifest = {'files': {'quizpk.sqlite': digest(self.backup / 'quizpk.sqlite')},
                    'copied': {str(self.live / 'private/phone01.token'): {'sha256': digest(self.live / 'private/phone01.token')}}}
        (self.backup / 'manifest.json').write_text(json.dumps(manifest))
        for name, value in [('LIVE', self.live), ('ROOT', self.backups)]:
            p = patch.object(rotate_tool, name, value); p.start(); self.addCleanup(p.stop)
        p = patch.object(rotate_tool, 'assert_stopped'); p.start(); self.addCleanup(p.stop)

    def run_rotation(self):
        output = io.StringIO()
        with contextlib.redirect_stdout(output): rotate_tool.rotate(self.backup)
        self.assertNotIn(self.old, output.getvalue())
        return json.loads(output.getvalue())

    def test_rotation_preserves_identity_other_users_and_jobs(self):
        result = self.run_rotation(); self.assertTrue(result['allPassed'])
        db = sqlite3.connect(self.live / 'data/quizpk.sqlite'); self.addCleanup(db.close)
        self.assertEqual(db.execute('SELECT token_hash FROM users WHERE id="owner"').fetchone()[0], self.owner_hash)
        self.assertFalse(db.execute('SELECT 1 FROM users WHERE token_hash=?', (self.old_hash,)).fetchone())
        self.assertEqual(db.execute('SELECT response FROM jobs').fetchone()[0], 'synthetic saved data')
        self.assertEqual(db.execute('PRAGMA user_version').fetchone()[0], 7)
        self.assertEqual((self.live / 'private/phone01.token').stat().st_mode & 0o777, 0o600)

    def test_live_data_drift_refuses_rotation(self):
        db = sqlite3.connect(self.live / 'data/quizpk.sqlite')
        db.execute('UPDATE jobs SET response="new synthetic data"'); db.commit(); db.close()
        with self.assertRaises(RuntimeError): self.run_rotation()
        self.assertEqual((self.live / 'private/phone01.token').read_text().strip(), self.old)

    def test_file_failure_after_commit_never_reenables_exposed_token(self):
        with patch.object(rotate_tool.os, 'replace', side_effect=OSError('synthetic failure')):
            with self.assertRaises(OSError): self.run_rotation()
        db = sqlite3.connect(self.live / 'data/quizpk.sqlite'); self.addCleanup(db.close)
        self.assertFalse(db.execute('SELECT 1 FROM users WHERE token_hash=?', (self.old_hash,)).fetchone())
        recovery = next(self.backups.glob('*-phone01-rotation'))
        replacement = (recovery / 'replacement-phone01.token').read_text().strip()
        self.assertTrue(db.execute('SELECT 1 FROM users WHERE id="phone01" AND token_hash=?', (hashlib.sha256(replacement.encode()).hexdigest(),)).fetchone())
        self.assertTrue(json.loads((recovery / 'failure.json').read_text())['databaseCommitOccurred'])


if __name__ == '__main__': unittest.main()
