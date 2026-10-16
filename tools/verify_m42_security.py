"""M4.2 preservation/security audit. Private baselines and values never leave memory.

--preservation-manifest points to a PRIVATE {absolute-path: sha256} JSON outside
shared source. Do not publish that manifest, its paths, or its individual hashes.
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
parser.add_argument('--preservation-manifest', type=Path, required=True)
args = parser.parse_args()
checks, counts = {}, {}


def public_file(path):
    return not (
        any(p in {'.git', '.gradle', '.cg', 'build', 'artifacts', 'node_modules',
                  'GEMINI_API_KEY', '__pycache__'} for p in path.parts)
        or str(path).startswith(('backend/private/', 'backend/data/'))
        or path.name.endswith(('.env', '.token', '.jks', '.keystore', '.sqlite',
                               '.sqlite-wal', '.sqlite-shm'))
        or path.name.startswith('.env') and path.name != '.env.example'
        or path.name in {'release.properties', 'backend.properties', 'local.properties'}
    )


try:
    private_before = json.loads(args.preservation_manifest.read_text())
    counts['privateAndLiveDurableFilesCompared'] = len(private_before)
    checks['credentialsSigningLiveSourceDatabaseAndWalUnchanged'] = all(
        Path(path).is_file() and hashlib.sha256(Path(path).read_bytes()).hexdigest() == digest
        for path, digest in private_before.items()
    )
    # Never open private SQLite or execute configuration. Only compare file bytes.
    secrets = []
    candidates = {Path(p) for p in private_before} | {
        root / 'GEMINI_API_KEY/GEMINI_API_KEY.env', root / 'backend/GEMINI_API_KEY.env'}
    for path in candidates:
        if not path.is_file():
            continue
        if path.suffix == '.token':
            token = path.read_bytes().strip()
            if token:
                secrets.extend([token, hashlib.sha256(token).hexdigest().encode()])
        if path.suffix == '.env':
            for line in path.read_text().splitlines():
                if re.match(r'^\s*GEMINI_API_KEY\s*=', line):
                    value = line.split('=', 1)[1].strip().strip('\"\'').encode()
                    if value:
                        secrets.append(value)
    if os.environ.get('GEMINI_API_KEY'):
        secrets.append(os.environ['GEMINI_API_KEY'].encode())
    variants = [v for s in secrets for v in
                (s, s.decode().encode('utf-16le'), s.decode().encode('utf-16be'))]

    def clean(data):
        return not any(v in data for v in variants) and not re.search(rb'AIza[A-Za-z0-9_-]{30,}', data)

    files = [p for p in root.rglob('*') if p.is_file() and public_file(p.relative_to(root))]
    counts['publicFilesScanned'] = len(files)
    checks['sourceDocsTestsAndEvidenceContainNoActualSecrets'] = all(clean(p.read_bytes()) for p in files)
    checks['workspacePrivateDatabaseAbsent'] = not any(p.is_file() for p in (root / 'backend/data').rglob('*'))

    checkpoint = root.parent / 'QuizPK_Checkpoints/QuizPK-M4.1-approved-before-M4.2.tar.gz'
    checks['m41CheckpointUnchanged'] = hashlib.sha256(checkpoint.read_bytes()).hexdigest() == 'c76afd06599d4e9b0d551b2229ecc8925091aa22130e2c9fb66de6eedcf5f958'
    permitted_edits = {
        'app/build.gradle.kts',
        'app/src/main/java/com/zeke/quizpk/ui/screens/MultiplayerFragment.java',
        'app/src/main/res/layout/fragment_multiplayer.xml',
    }
    with tarfile.open(checkpoint) as archive:
        members = [m for m in archive.getmembers() if m.isfile()]
        checks['m41CheckpointExcludesPrivateBuildAndEvidence'] = all(
            public_file(Path(m.name)) and not m.name.startswith('docs/validation/') for m in members)
        checks['m41CheckpointContainsNoActualSecrets'] = all(clean(archive.extractfile(m).read()) for m in members)
        protected = [m for m in members if m.name not in permitted_edits and (
            m.name.startswith(('app/', 'backend/', 'protocol/', 'gradle/', 'QuizPK_Codex_Starter/'))
            or m.name in {'.gitignore', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat'})]
        counts['protectedM1ThroughM41SourceFilesCompared'] = len(protected)
        checks['soloStorageCredentialsBrandThemesBankBackendProtocolAndPriorTestsUnchanged'] = all(
            (root / m.name).is_file() and (root / m.name).read_bytes() == archive.extractfile(m).read() for m in protected)

    for name, digest in {
        'QuizPK-M3-source-before-M4.1.tar.gz': '8fe1a54a07768aaec255b67a1c54923b55127e2005e673d7939386d57f690d28',
        'QuizPK-M3-approved-before-M4.1.tar.gz': '065c5ea2b506ff80cfd165ad53d34ae41e4717e80bbba94085451c8586241ff6',
    }.items():
        archive_path = root.parent / 'QuizPK_Checkpoints' / name
        checks[name + 'Unchanged'] = hashlib.sha256(archive_path.read_bytes()).hexdigest() == digest
    stage = root.parent / 'QuizPK_Checkpoints/QuizPK-M4.2-online-lobby-review.tar.gz'
    with tarfile.open(stage) as archive:
        members = [m for m in archive.getmembers() if m.isfile()]
        counts['m42ReviewCheckpointSourceFiles'] = len(members)
        checks['m42ReviewCheckpointExcludesPrivateBuildAndEvidence'] = all(
            public_file(Path(m.name)) and not m.name.startswith('docs/validation/') for m in members)
        checks['m42ReviewCheckpointContainsNoActualSecrets'] = all(clean(archive.extractfile(m).read()) for m in members)
        checks['m42ReviewCheckpointMatchesFinalWorkspaceSource'] = all(
            (root / m.name).is_file() and (root / m.name).read_bytes() == archive.extractfile(m).read() for m in members)
    checks['retainedM3ApkUnchanged'] = hashlib.sha256((root / 'artifacts/QuizPK-M3-ai-debug.apk').read_bytes()).hexdigest() == '7fe96067cabeac51ce337a022e91743729c06d5f6752100e1a43b98d1665ff76'
    installed = json.loads((root / 'docs/validation/m4.2/installed-app-baseline.json').read_text())
    checks['installedM3PackageAndSignerVerifiedWithoutInstallation'] = all(installed.get(k) is True for k in (
        'inspectionCompleted', 'installedMatchesPreservedM3Signer', 'installedMatchesRetainedM3ApkBytes', 'newVersion4IsUpgrade'))

    apk = root / 'artifacts/QuizPK-M4.2-online-lobby-debug.apk'
    metadata = json.loads((root / 'docs/validation/m4.2/apk-summary.json').read_text())
    checks['deliveredApkMatchesVerifiedUpdateSignatureAndMetadata'] = (
        hashlib.sha256(apk.read_bytes()).hexdigest() == metadata['sha256']
        and metadata['package'] == 'com.zeke.quizpk' and metadata['versionCode'] == 4
        and metadata['certificateSha256'] == installed['installedCertificateSha256']
        and all(metadata[k] is True for k in (
            'v2SignatureVerified', 'matchesInstalledM3Certificate', 'versionIsUpgrade',
            'deliveryMatchesBuildOutput', 'globalCleartextDisabled')))
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        checks['apkContainsNoPrivateFilesOrValues'] = all(
            clean(archive.read(n)) and not n.endswith(('.env', '.token', '.jks', '.keystore', '.sqlite'))
            and 'GEMINI_API_KEY' not in n for n in names)
        checks['apkContainsNoBackendFixturesOrTestRuntime'] = all(
            not n.endswith(('.mjs', 'multiplayer-v1.json')) and 'robolectric' not in n.lower()
            and 'sqlite4java' not in n.lower() and not n.startswith('lib/') for n in names)
        dex = [archive.read(n) for n in names if n.endswith('.dex')]
        checks['apkDexContainsNoLobbyTestClassesOrRobolectric'] = all(
            marker not in data for data in dex for marker in (
                b'Lcom/zeke/quizpk/LobbyFixtures', b'Lcom/zeke/quizpk/LobbyFlowTest',
                b'Lcom/zeke/quizpk/LobbyProtocolTest', b'Lcom/zeke/quizpk/LobbyBackendIntegrationTest',
                b'Lorg/robolectric/', b'Lcom/almworks/sqlite4java/'))
        checks['apkBankBytesPreserved'] = archive.read('assets/questions.json') == (root / 'app/src/main/assets/questions.json').read_bytes()

    with tempfile.TemporaryDirectory(prefix='quizpk-m42-ignore-') as directory:
        folder = Path(directory)
        (folder / '.gitignore').write_bytes((root / '.gitignore').read_bytes())
        subprocess.run(['git', 'init', '-q', str(folder)], check=True, capture_output=True)
        paths = ['user.token', 'elsewhere/user.token', 'backend/private/user.token',
                 'backend/data/quizpk.sqlite', 'GEMINI_API_KEY/GEMINI_API_KEY.env',
                 'GEMINI_API_KEY.env', 'backend/.env', 'release.properties', 'private.jks']
        result = subprocess.run(['git', '-C', str(folder), 'check-ignore', '--no-index', *paths],
                                capture_output=True, text=True, check=True)
        checks['privateFileIgnoreRulesEffective'] = set(result.stdout.splitlines()) == set(paths)
    print(json.dumps({'checks': checks, 'counts': counts}, indent=2))
    if not all(checks.values()):
        raise SystemExit('M4.2 audit needs attention. No private values printed.')
except SystemExit:
    raise
except Exception:
    raise SystemExit('M4.2 audit could not complete. No private values printed.') from None
