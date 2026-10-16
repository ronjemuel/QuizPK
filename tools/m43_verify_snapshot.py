#!/usr/bin/env python3
"""Compare verified private backups only. Never opens live SQLite or imports Store."""
import contextlib
import hashlib
import json
from pathlib import Path
import sys
from m43_backend_ops import verify_backup, verify_release, private, require
from m43_backend_backup import readonly


def verify(before, after, prep):
    before, after, prep = private(before), private(after), private(prep)
    a, _ = verify_backup(before); b, _ = verify_backup(after)
    _, context = verify_release(prep)
    require(a['inventory']['multiplayerVersion'] in (1, 2) and b['inventory']['multiplayerVersion'] == 2)
    with contextlib.closing(readonly(before / 'quizpk.sqlite')) as old, contextlib.closing(readonly(after / 'quizpk.sqlite')) as new:
        require(new.execute('PRAGMA integrity_check').fetchall() == [('ok',)])
        require(not new.execute('PRAGMA foreign_key_check').fetchall())
        require(new.execute('PRAGMA user_version').fetchone() == old.execute('PRAGMA user_version').fetchone())
        for row in old.execute('SELECT * FROM users'):
            require(new.execute('SELECT * FROM users WHERE id=?', (row[0],)).fetchone() == row)
        for row in old.execute('SELECT * FROM jobs'):
            require(new.execute('SELECT * FROM jobs WHERE user_id=? AND key=?', row[:2]).fetchone() == row)
        if a['inventory']['multiplayerVersion'] == 2:
            for table, keys in [('mp_answers', ('round_id', 'participant_id')),
                                ('mp_answer_keys', ('round_id',)),
                                ('mp_match_players', ('room_id', 'user_id'))]:
                columns = [r[1] for r in old.execute('PRAGMA table_info("' + table + '")')]
                for row in old.execute('SELECT * FROM "' + table + '"'):
                    where = ' AND '.join('"' + k + '"=?' for k in keys)
                    values = [row[columns.index(k)] for k in keys]
                    require(new.execute('SELECT * FROM "' + table + '" WHERE ' + where, values).fetchone() == row)
        for p in (after / 'host/private').glob('*.token'):
            digest = hashlib.sha256(p.read_text().strip().encode()).hexdigest()
            require(new.execute('SELECT 1 FROM users WHERE enabled=1 AND token_hash=?', (digest,)).fetchone())
        revoked = hashlib.sha256(Path(context['revokedTokenFile']).read_text().strip().encode()).hexdigest()
        require(not new.execute('SELECT 1 FROM users WHERE enabled=1 AND token_hash=?', (revoked,)).fetchone())
        rotated = hashlib.sha256(Path(context['rotationTokenFile']).read_text().strip().encode()).hexdigest()
        require(new.execute('SELECT 1 FROM users WHERE id=? AND enabled=1 AND token_hash=?', ('phone01', rotated)).fetchone())
    return {'schemaTwo': True, 'integrityAndForeignKeys': True, 'priorIdentitiesAndJobsPreserved': True,
            'userVersionPreserved': True, 'currentTokensValid': True, 'exposedTokenRejected': True,
            'priorMatchRecordsPreservedWhenPresent': True}


if __name__ == '__main__':
    try:
        require(len(sys.argv) == 4)
        print(json.dumps(verify(*(Path(p) for p in sys.argv[1:])), indent=2))
    except Exception:
        print('Private snapshot validation failed; no live SQLite opened and no credentials printed.')
        raise SystemExit(1)
