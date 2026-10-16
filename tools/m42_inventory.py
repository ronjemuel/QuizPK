"""Inventory M4.2 against the verified approved M4.1 source checkpoint."""
from pathlib import Path
import tarfile

root = Path(__file__).resolve().parent.parent


def allowed(path):
    return not (
        any(p in {'.git', '.gradle', '.cg', 'build', 'artifacts', 'node_modules',
                  'GEMINI_API_KEY', '__pycache__'} for p in path.parts)
        or str(path).startswith(('backend/private/', 'backend/data/', 'docs/validation/'))
        or path.name.endswith(('.env', '.token', '.jks', '.keystore', '.sqlite',
                               '.sqlite-wal', '.sqlite-shm'))
        or path.name.startswith('.env') and path.name != '.env.example'
        or path.name in {'release.properties', 'local.properties', 'backend.properties'}
    )


checkpoint = root.parent / 'QuizPK_Checkpoints/QuizPK-M4.1-approved-before-M4.2.tar.gz'
with tarfile.open(checkpoint) as archive:
    before = {Path(m.name): archive.extractfile(m).read() for m in archive.getmembers()
              if m.isfile() and allowed(Path(m.name))}
now = {p.relative_to(root): p.read_bytes() for p in root.rglob('*')
       if p.is_file() and allowed(p.relative_to(root))}
added = sorted(set(now) - set(before))
modified = sorted(p for p in now if p in before and now[p] != before[p])
removed = sorted(set(before) - set(now))
lines = [
    '# M4.2 changed files', '',
    'Compared with the approved pre-edit M4.1 checkpoint:',
    '`../QuizPK_Checkpoints/QuizPK-M4.1-approved-before-M4.2.tar.gz` (192 files).',
    'No Git repository exists. Private files, signing, databases, generated builds,',
    'APKs and validation reports are excluded from this source inventory.',
    'The [M4.1 inventory](M4_1_CHANGED_FILES.md) is retained as history.', '',
    '## Scope and rationale', '',
    '- `MultiplayerFragment` and native XML: network choice, Online create/join,',
    '  separate nonplaying Moderator, eight-contestant roster, ready/settings,',
    '  invite copy/share, connection/errors and confirmed leave/close.',
    '- `LobbyViewModel`: serial lifecycle-aware polling/heartbeat, authenticated',
    '  recovery, exact-envelope explicit uncertain retries and credential isolation.',
    '- `RoomProtocol` and `RoomBackendClient`: strict common-v1 lobby projection',
    '  and bounded HTTPS individual-token adapter. No gameplay or LAN engine.',
    '- `app/build.gradle.kts`: version 4 / 1.3-M4.2, canonical protocol as test-only',
    '  resource. Existing dependencies, debug signer and SDK requirements unchanged.',
    '- Lobby tests and isolated Node fixture: protocol, native UI/lifecycle/errors',
    '  and actual Java-to-M4.1 Node/SQLite integration. No real provider or live DB.',
    '- Docs/tools: deployment plan (not executed), two-device acceptance plan,',
    '  checkpoint/preservation/security verification and reproducible inventory.', '',
    '## Preservation and migration', '',
    'No production backend/protocol change, new SQL migration, Android permission',
    'or dependency was introduced. Android SQLite remains v3. M1 startup/navigation,',
    'themes/brand, M2 bank/history/drafts, M3 AI/saved quiz/credentials implementations',
    'and their original tests are byte-preserved apart from the authorized multiplayer',
    'placeholder replacement and app version/test-resource configuration.',
    'The live Termux source, private env/tokens and durable DB/WAL were not edited.',
    'The M3 checkpoint and M3 APK remain unchanged. See TEST_NOTES for audit evidence.', '',
]
for label, paths in [('Modified', modified), ('Added', added), ('Removed', removed)]:
    lines += [f'## {label} ({len(paths)})', ''] + [f'- [{p}](../{p})' for p in paths] + ['']
lines += [
    '## Generated delivery and evidence', '',
    '- `artifacts/QuizPK-M4.2-online-lobby-debug.apk`: same-signer Android update.',
    '- `docs/validation/m4.2/`: checkpoint, backend/full Android logs, test XML,',
    '  lint, APK metadata/signature/hash, preservation and source inventory evidence.',
    '- Physical two-device acceptance remains pending; no attached ADB device.',
    '- Live matches, question authoring, rankings and LAN runtime remain deferred.',
    '- **STOP for M4.2 review.** Live backend deployment needs separate approval.', '',
]
(root / 'docs/M4_CHANGED_FILES.md').write_text('\n'.join(lines))
print(f'M4.2 inventory: {len(modified)} modified, {len(added)} added, {len(removed)} removed source/documentation files.')
