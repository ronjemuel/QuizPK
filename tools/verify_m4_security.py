"""M4.1 preservation audit. Secrets/hashes stay in memory; output only booleans/counts.

Optional --preservation-manifest and --external-preservation-manifest point to
PRIVATE {absolute path: sha256} baselines outside the workspace. Never distribute
those manifests or print their contents.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import tarfile
import tempfile
import zipfile

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--preservation-manifest', type=Path)
parser.add_argument('--external-preservation-manifest', type=Path)
args = parser.parse_args()
checks = {}
counts = {}
secrets = []
try:
    # Load only for private in-memory comparisons; never log names/values/hashes.
    for path in [root / 'GEMINI_API_KEY/GEMINI_API_KEY.env', root / 'backend/GEMINI_API_KEY.env']:
        if path.exists():
            for line in path.read_text().splitlines():
                if re.match(r'^\s*GEMINI_API_KEY\s*=', line):
                    value = line.split('=', 1)[1].strip().strip('\"\'').encode()
                    if value:
                        secrets.append(value)
    if os.environ.get('GEMINI_API_KEY'):
        secrets.append(os.environ['GEMINI_API_KEY'].encode())
    token_paths = list((root / 'backend/private').glob('*.token'))
    external_before = json.loads(args.external_preservation_manifest.read_text()) if args.external_preservation_manifest else {}
    token_paths += [Path(path) for path in external_before if path.endswith('.token')]
    for path in token_paths:
        token = path.read_bytes().strip()
        if token:
            secrets += [token, hashlib.sha256(token).hexdigest().encode()]
    for path in external_before:
        if path.endswith('.env'):
            for line in Path(path).read_text().splitlines():
                if re.match(r'^\s*GEMINI_API_KEY\s*=', line):
                    value = line.split('=', 1)[1].strip().strip('\"\'').encode()
                    if value:
                        secrets.append(value)
    variants = [v for s in secrets for v in (s, s.decode().encode('utf-16le'), s.decode().encode('utf-16be'))]

    def clean(data):
        return not any(v in data for v in variants) and not re.search(rb'AIza[A-Za-z0-9_-]{30,}', data)

    def public_file(path):
        name = path.name
        return not (
            any(part in {'.git', '.cg', '.gradle', 'build', 'artifacts', 'node_modules', 'GEMINI_API_KEY', '__pycache__'} for part in path.parts)
            or str(path).startswith(('backend/private/', 'backend/data/'))
            or name.endswith(('.env', '.token', '.keystore', '.jks', '.sqlite', '.sqlite-wal', '.sqlite-shm'))
            or name.startswith('.env') and name != '.env.example'
            or name in {'release.properties', 'local.properties', 'backend.properties'}
        )

    files = [p for p in root.rglob('*') if p.is_file() and public_file(p.relative_to(root))]
    counts['publicFilesScanned'] = len(files)
    checks['sourceDocsFixturesAndEvidenceContainNoActualSecrets'] = all(clean(p.read_bytes()) for p in files)
    checks['workspacePrivateDatabaseStillAbsent'] = not any(p.is_file() for p in (root / 'backend/data').rglob('*'))
    if args.preservation_manifest:
        private_before = json.loads(args.preservation_manifest.read_text())
        counts['privateFilesComparedWithoutPrintingHashes'] = len(private_before)
        checks['preexistingCredentialTokenSigningAndDataFilesUnchanged'] = all(
            Path(path).is_file() and hashlib.sha256(Path(path).read_bytes()).hexdigest() == value
            for path, value in private_before.items()
        )
    if external_before:
        # SQLite read-only WAL readers update shared-memory reader coordination.
        # Never restore or require byte stability of the live -shm lock/index file.
        durable_before = {p: value for p, value in external_before.items() if not p.endswith('.sqlite-shm')}
        counts['externalDurablePrivateFilesComparedWithoutPrintingHashes'] = len(durable_before)
        checks['externalTermuxDatabaseWalTokensAndCredentialsUnchanged'] = all(
            Path(path).is_file() and hashlib.sha256(Path(path).read_bytes()).hexdigest() == value
            for path, value in durable_before.items()
        )

    checkpoint = root.parent / 'QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz'
    checks['m3CheckpointMatchesRecordedArchiveHash'] = hashlib.sha256(checkpoint.read_bytes()).hexdigest() == '8fe1a54a07768aaec255b67a1c54923b55127e2005e673d7939386d57f690d28'
    with tarfile.open(checkpoint) as archive:
        members = [m for m in archive.getmembers() if m.isfile()]
        counts['checkpointSourceFiles'] = len(members)
        checks['checkpointExcludesSecretsSigningPrivateStateAndBuilds'] = all(public_file(Path(m.name)) and not m.name.startswith('docs/validation/') for m in members)
        checks['checkpointContainsNoActualSecrets'] = all(clean(archive.extractfile(m).read()) for m in members)
        protected = [m for m in members if m.name.startswith(('app/', 'QuizPK_Codex_Starter/')) or m.name in {
            'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat',
            'backend/src/config.mjs', 'backend/src/contract.mjs', 'backend/src/gemini.mjs',
            'backend/src/create-user.mjs', 'backend/src/revoke-user.mjs', 'backend/src/check-config.mjs',
            'backend/src/smoke.mjs', 'backend/test/api.test.mjs', 'backend/test/persistence.test.mjs',
            'backend/package.json', 'backend/.env.example', 'backend/Caddyfile.example', 'backend/quizpk.service.example'
        } or m.name.startswith('gradle/')]
        counts['protectedM3SourceFilesCompared'] = len(protected)
        checks['androidBrandThemesBankReferencesBuildAndM3CoreUnchanged'] = all(
            (root / m.name).is_file() and (root / m.name).read_bytes() == archive.extractfile(m).read() for m in protected
        )
        protected_names = {m.name for m in protected if m.name.startswith('app/')}
        checks['noAndroidSourceOrResourceFilesAdded'] = all(
            str(p.relative_to(root)) in protected_names for p in (root / 'app').rglob('*')
            if p.is_file() and public_file(p.relative_to(root))
        )
    original = root.parent / 'QuizPK_Checkpoints/QuizPK-M3-approved-before-M4.1.tar.gz'
    checks['originalPreEditCheckpointRemainsUnchanged'] = hashlib.sha256(original.read_bytes()).hexdigest() == '065c5ea2b506ff80cfd165ad53d34ae41e4717e80bbba94085451c8586241ff6'
    with tarfile.open(original) as archive:
        checks['originalCheckpointContainsNoActualSecrets'] = all(
            clean(archive.extractfile(m).read()) for m in archive.getmembers() if m.isfile()
        )

    apk = root / 'artifacts/QuizPK-M3-ai-debug.apk'
    checks['retainedM3ApkBytesUnchanged'] = hashlib.sha256(apk.read_bytes()).hexdigest() == '7fe96067cabeac51ce337a022e91743729c06d5f6752100e1a43b98d1665ff76'
    with zipfile.ZipFile(apk) as archive:
        checks['apkContainsNoSecretFilesOrValues'] = all(
            clean(archive.read(name)) and not name.endswith(('.env', '.token', '.jks', '.keystore')) and 'GEMINI_API_KEY' not in name
            for name in archive.namelist()
        )
        checks['apkBankMatchesPreservedM2Bank'] = archive.read('assets/questions.json') == (root / 'app/src/main/assets/questions.json').read_bytes()

    with tempfile.TemporaryDirectory(prefix='quizpk-m41-ignore-') as folder:
        folder = Path(folder)
        (folder / '.gitignore').write_text((root / '.gitignore').read_text())
        subprocess.run(['git', 'init', '-q', str(folder)], check=True, capture_output=True)
        paths = ['user.token', 'elsewhere/user.token', 'backend/private/user.token',
                 'backend/data/quizpk.sqlite', 'GEMINI_API_KEY/GEMINI_API_KEY.env',
                 'GEMINI_API_KEY.env', 'backend/GEMINI_API_KEY.env', 'backend/.env', 'release.properties', 'private.jks']
        result = subprocess.run(['git', '-C', str(folder), 'check-ignore', '--no-index', *paths], capture_output=True, text=True, check=True)
        checks['privateFileIgnoreRulesEffective'] = set(result.stdout.splitlines()) == set(paths)
        checks['blankBackendTemplateRemainsTrackable'] = subprocess.run(
            ['git', '-C', str(folder), 'check-ignore', '--no-index', 'backend/.env.example'], capture_output=True
        ).returncode == 1
    print(json.dumps({'checks': checks, 'counts': counts}, indent=2))
    if not all(checks.values()):
        raise SystemExit('M4.1 audit needs attention. No credentials or private hashes printed.')
except SystemExit:
    raise
except Exception:
    raise SystemExit('M4.1 audit could not complete. No credentials or private hashes printed.') from None
