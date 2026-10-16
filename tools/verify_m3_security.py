"""Local audit. Prints only non-secret status; never print credential data on failure."""
from pathlib import Path
import re, json, tempfile, subprocess, zipfile, tarfile, hashlib
root=Path(__file__).resolve().parent.parent
apk=root/'artifacts/QuizPK-M3-ai-debug.apk'
result={}
# Exact local value is used only for in-memory comparison; it is never emitted.
secret_files=list((root/'GEMINI_API_KEY').glob('GEMINI_API_KEY.env'))+list((root/'backend').glob('GEMINI_API_KEY.env'))
secrets=[]
for path in secret_files:
    text=path.read_text().strip()
    lines=[line.strip() for line in text.splitlines() if line.strip() and not line.lstrip().startswith('#')]
    if len(lines)!=1 or not re.fullmatch(r'GEMINI_API_KEY\s*=\s*\S.*',lines[0]):raise SystemExit('Credential format audit failed; no contents printed.')
    value=lines[0].split('=',1)[1].strip().strip('"\'').encode()
    if value:secrets.append(value)
result['credential_files_valid_format']=bool(secrets)
with tempfile.TemporaryDirectory(prefix='quizpk-ignore-') as folder:
    folder=Path(folder);(folder/'.gitignore').write_text((root/'.gitignore').read_text())
    subprocess.run(['git','init','-q',str(folder)],check=True)
    paths=['GEMINI_API_KEY.env','GEMINI_API_KEY/GEMINI_API_KEY.env','backend/GEMINI_API_KEY.env','backend/.env','backend/private/user.token','backend/data/quizpk.sqlite']
    checked=subprocess.run(['git','-C',str(folder),'check-ignore','--no-index',*paths],check=True,capture_output=True,text=True)
    result['secret_ignore_rules_effective']=set(checked.stdout.splitlines())==set(paths)
    template=subprocess.run(['git','-C',str(folder),'check-ignore','--no-index','backend/.env.example'],capture_output=True)
    result['safe_template_trackable']=template.returncode==1
leaks=[]
def contains_secret(data):
    variants=[variant for value in secrets for variant in (value,value.decode().encode('utf-16le'),value.decode().encode('utf-16be'))]
    return any(value in data for value in variants) or bool(re.search(rb'AIza[A-Za-z0-9_-]{30,}',data))
for parent in ['app/src','backend/src','backend/test','docs']:
    for path in (root/parent).rglob('*'):
        if path.is_file():
            data=path.read_bytes()
            if contains_secret(data):leaks.append(str(path.relative_to(root)))
for path in [root/'README.md',root/'PLAN.md',root/'IMPLEMENTATION_LOG.md',root/'backend/README.md',root/'backend/.env.example']:
    if contains_secret(path.read_bytes()):leaks.append(str(path.relative_to(root)))
result['source_and_validation_secret_scan_clean']=not leaks
result['android_contains_no_provider_secret_or_private_path']=not any(b'GEMINI_API_KEY' in p.read_bytes() or b'generativelanguage.googleapis.com' in p.read_bytes() for p in (root/'app/src/main').rglob('*') if p.is_file())
if apk.exists():
    with zipfile.ZipFile(apk) as archive:
        result['apk_contains_no_secret_files']=not any('GEMINI_API_KEY' in name or name.endswith('.env') for name in archive.namelist())
        result['apk_secret_value_scan_clean']=all(not contains_secret(archive.read(name)) for name in archive.namelist())
        result['apk_no_backend_or_test_code']=not any('backend/' in name or 'sqlite4java' in name or 'robolectric' in name for name in archive.namelist()) and all(not any(marker in archive.read(name) for marker in (b'org/robolectric',b'com/almworks/sqlite4java',b'HostSqliteSupport')) for name in archive.namelist() if name.endswith('.dex'))
        result['m2_seed_asset_unchanged']=archive.read('assets/questions.json')==(root/'app/src/main/assets/questions.json').read_bytes()
checkpoint=root.parent/'QuizPK_Checkpoints/QuizPK-M2-approved-before-M3.tar.gz'
with tarfile.open(checkpoint) as archive:
    result['checkpoint_excludes_secrets']=not any('GEMINI_API_KEY' in name or name.endswith(('.env','.keystore','.jks')) for name in archive.getnames())
    result['checkpoint_secret_value_scan_clean']=all(not contains_secret(archive.extractfile(m).read()) for m in archive.getmembers() if m.isfile())
    protected=['app/src/main/java/com/zeke/quizpk/MainActivity.java','app/src/main/java/com/zeke/quizpk/ui/theme/ThemePreferences.java','app/src/main/assets/questions.json']
    protected += [m.name for m in archive.getmembers() if m.name.startswith(('app/src/main/res/values-night/','app/src/main/res/drawable/','app/src/main/res/mipmap','QuizPK_Codex_Starter/references/','QuizPK_Codex_Starter/additional_references/'))]
    protected += ['app/src/main/res/values/colors.xml','app/src/main/res/values/themes.xml','app/src/main/res/values/styles.xml']
    result['m1_startup_brand_themes_and_m2_bank_preserved']=all(archive.extractfile(name).read()==(root/name).read_bytes() for name in protected)
    engine=['app/src/main/java/com/zeke/quizpk/ui/screens/GameplayFragment.java']+['app/src/main/java/com/zeke/quizpk/domain/'+name+'.java' for name in ('Question','QuizSession','QuizScoring')]
    result['m2_gameplay_and_scoring_engine_unchanged']=all(archive.extractfile(name).read()==(root/name).read_bytes() for name in engine)
print(json.dumps(result,indent=2))
if not all(result.values()):raise SystemExit('M3 security/preservation audit needs attention. No credentials printed.')
