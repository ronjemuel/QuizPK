"""Inventory M4.1 against the verified M3 checkpoint, excluding private/generated files."""
from pathlib import Path
import tarfile

root = Path(__file__).resolve().parent.parent
excluded = {'.gradle', '.cg', 'build', 'artifacts', '.git', 'GEMINI_API_KEY', 'node_modules', '__pycache__'}

def allowed(path):
    name = path.name
    return not (
        any(part in excluded for part in path.parts)
        or name.endswith(('.env', '.token', '.keystore', '.jks', '.sqlite', '.sqlite-wal', '.sqlite-shm'))
        or name.startswith('.env') and name != '.env.example'
        or str(path).startswith(('backend/data/', 'backend/private/', 'docs/validation/'))
        or name in ('release.properties', 'local.properties', 'backend.properties')
    )

checkpoint = root.parent / 'QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz'
with tarfile.open(checkpoint) as archive:
    before = {Path(m.name): archive.extractfile(m).read() for m in archive.getmembers() if m.isfile() and allowed(Path(m.name))}
now = {p.relative_to(root): p.read_bytes() for p in root.rglob('*') if p.is_file() and allowed(p.relative_to(root))}
supplied = {Path('QuizPK_M4_ONLINE_AND_LOCAL_PROMPT.md')}
added = sorted(set(now) - set(before) - supplied)
modified = sorted(p for p in now if p in before and now[p] != before[p])
removed = sorted(set(before) - set(now))
lines = [
    '# M4.1 changed files', '',
    'Compared with `../QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz`.',
    'The replacement Online + Local brief is user-supplied, not an implementation',
    'change. Secret/signing files, private DBs, builds and validation outputs are',
    'excluded. No Git repository exists. No Android source/resource/build change.', '',
    '## Scope and rationale', '',
    '- `store.mjs`: additive multiplayer migration hook; existing M3 methods unchanged.',
    '- `server.mjs`: authenticated room dispatch and protocol-specific errors; M3 generation unchanged.',
    '- `multiplayer/`: isolated schema ledger, strict commands, transactional lobby authority, HTTP adapter.',
    '- `check-room-db.mjs`: operator-only read-only identity/schema check, boolean output.',
    '- Room/protocol tests and JSON fixtures: real loopback integration plus future-rule design vectors.',
    '- `.gitignore`: exclude access-token files wherever they are placed.',
    '- Docs/tools: architecture, contract, private setup/recovery, inventory and preservation evidence.', '',
    '## SQLite migration', '',
    'Adds mp_schema version 1, mp_rooms, mp_members, mp_requests, mp_limits and indexes.',
    'Does not alter/drop users/jobs/throttle or change their contents/user_version.',
    'Android SQLite remains v3. Migration/reopen of M3 fixtures,',
    'auth/provisioning and on-disk restart are tested; actual Termux identities checked',
    'read-only with both existing tokens, with durable DB/WAL unchanged and M3 schema retained.', ''
]
for label, paths in [('Modified', modified), ('Added', added), ('Removed', removed)]:
    lines += [f'## {label} ({len(paths)})', ''] + [f'- [{p}](../{p})' for p in paths] + ['']
lines += [
    '## Generated evidence and retained delivery', '',
    '- `docs/validation/m4.1/`: restored M3 tests, all backend/Android regressions,',
    '  first failed error-shape check and corrected passes, test XML, signature,',
    '  checkpoint, read-only DB status, preservation/security and inventory evidence.',
    '- `artifacts/QuizPK-M3-ai-debug.apk`: unchanged M3 delivery; no M4 APK/UI exists yet.',
    '- No live server/tunnel/provider call or durable private DB modification was performed.',
    '- M4.2 UI, actual rounds, authoring/results/history and embedded LAN runtime are deferred.',
    '- Stop for review under the replacement brief before M4.2.', ''
]
(root / 'docs/M4_CHANGED_FILES.md').write_text('\n'.join(lines))
print(f'M4.1 inventory: {len(modified)} modified, {len(added)} added, {len(removed)} removed source/documentation files.')

