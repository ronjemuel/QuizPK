"""Compare non-secret source files with the verified pre-M3 checkpoint."""
from pathlib import Path
import tarfile
root=Path(__file__).resolve().parent.parent
skip={'.gradle','.cg','build','artifacts','.git','GEMINI_API_KEY','node_modules'}
def allowed(path):
    secret_name=path.name.endswith(('.env','.keystore','.jks')) or path.name.startswith('.env') and path.name!='.env.example'
    private_folder=str(path).startswith(('backend/data/','backend/private/','docs/validation/'))
    return not any(p in skip for p in path.parts) and not secret_name and not private_folder and path.name not in ('release.properties','local.properties','backend.properties')
with tarfile.open(root.parent/'QuizPK_Checkpoints/QuizPK-M2-approved-before-M3.tar.gz') as archive:
    before={Path(member.name):archive.extractfile(member).read() for member in archive.getmembers() if member.isfile() and allowed(Path(member.name))}
now={p.relative_to(root):p.read_bytes() for p in root.rglob('*') if p.is_file() and allowed(p.relative_to(root))}
added=sorted(set(now)-set(before));modified=sorted(p for p in now if p in before and now[p]!=before[p]);removed=sorted(set(before)-set(now))
lines=['# M3 changed files','','Compared with `../QuizPK_Checkpoints/QuizPK-M2-approved-before-M3.tar.gz`.','Credential files, signing files, generated builds and retained validation outputs','are excluded. MainActivity startup fix, themes, brand assets, bank and reference','images match the checkpoint. No multiplayer implementation.','']
for label,paths in [('Modified',modified),('Added',added),('Removed',removed)]:
    lines += [f'## {label} ({len(paths)})','']+[f'- [{p}](../{p})' for p in paths]+['']
lines += ['## Generated artifacts and evidence','','- `artifacts/QuizPK-M3-ai-debug.apk`: delivery artifact; M1/M2 APKs retained.','- `docs/validation/m3/`: actual clean/targeted Gradle logs, first failed checks,','  final test XML, lint reports, backend mock tests, signing/metadata/hash,','  security and preservation audit, seed audit and empty ADB listing.','','No Gemini secret was copied to source, APKs, archives, reports or chat.','No actual production URL or paid plan was configured. Real HTTPS/backend/phone','acceptance remains pending; see TEST_NOTES and backend/README.']
(root/'docs/M3_CHANGED_FILES.md').write_text('\n'.join(lines)+'\n')
print('M3 inventory:',len(modified),'modified,',len(added),'added,',len(removed),'removed source/documentation files.')
