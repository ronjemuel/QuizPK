// Operator rehearsal only. Copies a verified private snapshot; never opens live SQLite.
import {DatabaseSync} from 'node:sqlite';
import {createHash, randomUUID, randomBytes} from 'node:crypto';
import {readFileSync,writeFileSync,existsSync,mkdirSync,copyFileSync,readdirSync,statSync,realpathSync,chmodSync} from 'node:fs';
import {resolve,dirname,join} from 'node:path';
import {pathToFileURL,fileURLToPath} from 'node:url';
import {spawn} from 'node:child_process';

const root='/data/data/com.termux/files/home/.local/share/quizpk-backups';
const workspace=resolve(dirname(fileURLToPath(import.meta.url)),'..');
const checks={};let phase='arguments',store,server,child;
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const requireCheck=(name,value)=>{phase=name;if(!value)throw Error();checks[name]=true;};
const hash=b=>createHash('sha256').update(b).digest('hex');
const files=p=>readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(join(p,e.name)):[join(p,e.name)]);
function contents(db,tables=['users','jobs','throttle']) {
  return Object.fromEntries(tables.map(t=>[t,db.prepare('SELECT * FROM "'+t+'" ORDER BY rowid').all()]));
}
const sqlCheck=db=>db.prepare('PRAGMA integrity_check').get().integrity_check==='ok' && db.prepare('PRAGMA foreign_key_check').all().length===0;
async function stop() {
  if(server){await new Promise(r=>server.close(r));server=null;}
  if(store){store.close();store=null;}
}

try {
  process.umask(0o077);
  const backup=realpathSync(process.argv[2]||'');
  requireCheck('privateBackupPath',backup.startsWith(root+'/') && !backup.includes('..'));
  const manifest=JSON.parse(readFileSync(join(backup,'manifest.json')));
  requireCheck('backupFileHashes',Object.entries(manifest.files).every(([p,h])=>hash(readFileSync(join(backup,p)))===h));
  requireCheck('snapshotHash',hash(readFileSync(join(backup,'quizpk.sqlite')))===manifest.snapshotSha256);
  const staging=backup+'-staging';mkdirSync(staging,{mode:0o700});
  const release=join(staging,'m41');mkdirSync(join(release,'src'),{recursive:true,mode:0o700});
  const inputRelease=process.argv[3]?realpathSync(process.argv[3]):join(workspace,'backend');
  for(const f of [...files(join(inputRelease,'src')),join(inputRelease,'package.json')]){
    const target=join(release,f.slice(inputRelease.length+1));
    mkdirSync(dirname(target),{recursive:true,mode:0o700});copyFileSync(f,target);chmodSync(target,0o600);
  }
  const releaseFiles=Object.fromEntries(files(release).map(p=>[p.slice(release.length+1),hash(readFileSync(p))]));
  writeFileSync(join(staging,'release-manifest.json'),JSON.stringify(releaseFiles,null,2)+'\n',{mode:0o600});
  const dbPath=join(staging,'rehearsal.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),dbPath);chmodSync(dbPath,0o600);
  const base=new DatabaseSync(dbPath,{readOnly:true});const original=contents(base),version=base.prepare('PRAGMA user_version').get().user_version;
  const schema=base.prepare("SELECT name,sql FROM sqlite_master WHERE type IN ('table','index') AND name NOT LIKE 'sqlite_%' ORDER BY name").all();
  requireCheck('restoredM3Integrity',sqlCheck(base));
  requireCheck('originalM3Only',base.prepare("SELECT count(*) n FROM sqlite_master WHERE name LIKE 'mp_%'").get().n===0);base.close();
  const {Store}=await import(pathToFileURL(join(release,'src/store.mjs')));
  const {createApi}=await import(pathToFileURL(join(release,'src/server.mjs')));
  const {Store:M3Store}=await import(pathToFileURL(join(backup,'host/src/store.mjs')));
  const {createApi:createM3Api}=await import(pathToFileURL(join(backup,'host/src/server.mjs')));
  const {validateRequest}=await import(pathToFileURL(join(release,'src/contract.mjs')));
  store=new Store(dbPath);
  requireCheck('additiveMigrationDataEqual',equal(contents(store.db),original));
  requireCheck('m3UserVersionPreserved',store.db.prepare('PRAGMA user_version').get().user_version===version);
  requireCheck('m3SchemasPreserved',schema.every(s=>store.db.prepare('SELECT sql FROM sqlite_master WHERE name=?').get(s.name)?.sql===s.sql));
  requireCheck('migrationVersionOne',store.db.prepare('SELECT version FROM mp_schema').get().version===1);
  requireCheck('migrationIntegrity',sqlCheck(store.db));
  const tokenDir=join(backup,'host/private');
  const tokens=readdirSync(tokenDir).filter(f=>f.endsWith('.token')).map(f=>readFileSync(join(tokenDir,f),'utf8').trim());
  const users=tokens.map(t=>store.authenticate('Bearer '+t));
  requireCheck('twoExistingTokensDistinctEnabled',tokens.length===2 && new Set(users).size===2);
  const revoked=process.argv[4]?readFileSync(realpathSync(process.argv[4]),'utf8').trim():null;
  if(revoked){
    let denied=false;try{store.authenticate('Bearer '+revoked);}catch(e){denied=e.code==='AUTH_REQUIRED';}
    requireCheck('exposedTokenRejectedM41',denied);
    requireCheck('replacementRetainsPhone01Identity',users.includes('phone01'));
  }
  let providerCalls=0;
  const provider=async()=>{providerCalls++;throw Error('REHEARSAL_PROVIDER_DISABLED');};
  let origin;
  async function start(api=createApi) {
    server=api({store,provider,model:'rehearsal-disabled',production:true});
    await new Promise(r=>server.listen(0,'127.0.0.1',r));origin='http://127.0.0.1:'+server.address().port;
  }
  async function request(token,path,body,headers={}) {
    const response=await fetch(origin+path,{method:body===undefined?'GET':'POST',redirect:'error',
      headers:{'X-Forwarded-Proto':'https',...(token?{Authorization:'Bearer '+token}:{}),'Content-Type':'application/json',...headers},
      ...(body===undefined?{}:{body:JSON.stringify(body)}),signal:AbortSignal.timeout(5000)});
    return {status:response.status,body:await response.json()};
  }
  const command=(type,payload={},extra={})=>({protocolVersion:1,requestId:randomUUID(),issuedAtMs:Date.now(),type,payload,...extra});
  await start();
  requireCheck('productionRejectsMissingHttps',(await request(null,'/health',undefined,{'X-Forwarded-Proto':''})).status===400);
  requireCheck('productionRejectsHttp',(await request(null,'/health',undefined,{'X-Forwarded-Proto':'http'})).body.error.code==='HTTPS_REQUIRED');
  requireCheck('productionHealth',(await request(null,'/health')).status===200);
  requireCheck('unauthenticatedRoomsRejected',(await request(null,'/api/rooms/current')).status===401);
  for(const t of tokens)requireCheck('existingTokensHttpAuthenticated',(await request(t,'/api/rooms/current')).status===200);
  if(revoked)requireCheck('exposedTokenRejectedM41Http',(await request(revoked,'/api/rooms/current')).status===401);
  const settings={category:'Science',difficulty:'Normal',questionCount:5,timerSeconds:15};
  const creation=command('room.create',{mode:'CLASSIC',displayName:'Rehearsal host',settings});
  const created=await request(tokens[0],'/api/rooms',creation);
  requireCheck('classicCreation',created.status===200);
  requireCheck('createExactRetry',equal(await request(tokens[0],'/api/rooms',creation),created));
  const room=created.body.result,roomPath='/api/rooms/'+room.roomId;
  const joined=await request(tokens[1],'/api/rooms/join',command('room.join',{code:room.inviteCode,displayName:'Rehearsal guest'}));
  requireCheck('classicJoin',joined.status===200);
  requireCheck('contestantReady',(await request(tokens[1],roomPath+'/ready',command('room.ready',{ready:true}))).status===200);
  const fresh=(await request(tokens[0],roomPath)).body;
  requireCheck('ownerSettings',(await request(tokens[0],roomPath+'/settings',command('room.settings',{...settings,timerSeconds:30},{expectedRevision:fresh.room.revision}))).status===200);
  requireCheck('settingsResetReady',(await request(tokens[1],roomPath)).body.room.members.every(m=>!m.ready));
  const beforeRestart=(await request(tokens[1],'/api/rooms/current')).body;
  await stop();store=new Store(dbPath);await start();
  const recovered=(await request(tokens[1],'/api/rooms/current')).body;
  requireCheck('authenticatedReconnectStableSeat',recovered.self.participantId===beforeRestart.self.participantId && recovered.room.id===room.roomId);
  requireCheck('restartChangesEpoch',recovered.authority.instanceId!==beforeRestart.authority.instanceId);
  requireCheck('restartResetsPresence',recovered.room.members.every(m=>!m.online && !m.ready));
  requireCheck('hostHeartbeat',(await request(tokens[0],roomPath+'/heartbeat',command('room.heartbeat'))).status===200);
  requireCheck('closeRoom',(await request(tokens[0],roomPath+'/leave',command('room.leave'))).status===200);
  requireCheck('closedCodeRejected',(await request(tokens[1],'/api/rooms/join',command('room.join',{code:room.inviteCode,displayName:'Guest'}))).status===404);
  const mod=await request(tokens[0],'/api/rooms',command('room.create',{mode:'MODERATOR',displayName:'Rehearsal moderator',settings}));
  requireCheck('moderatorCreation',mod.status===200);
  const modPath='/api/rooms/'+mod.body.result.roomId;
  requireCheck('moderatorCannotReady',(await request(tokens[0],modPath+'/ready',command('room.ready',{ready:true}))).status===403);
  requireCheck('moderatorContestantJoin',(await request(tokens[1],'/api/rooms/join',command('room.join',{code:mod.body.result.inviteCode,displayName:'Guest'}))).status===200);
  const modState=(await request(tokens[0],modPath)).body;
  requireCheck('moderatorSeparateRole',modState.self.role==='MODERATOR' && modState.room.members.filter(m=>m.role==='CONTESTANT').length===1);
  requireCheck('roomActivityPreservesAllM3Data',equal(contents(store.db),original));
  // Existing saved cached responses remain retrievable; original private rows never print.
  for(const job of original.jobs.filter(j=>j.response)) {
    requireCheck('existingSavedJobsReplay',equal(store.reserve(job.user_id,job.key,job.fingerprint),{status:job.status,body:JSON.parse(job.response)}));
  }
  requireCheck('existingSavedJobsChecked',original.jobs.some(j=>j.response));
  await stop();
  // Synthetic post-upgrade saved job: prove source rollback keeps newer data too.
  const requestBody=validateRequest({topic:'Staging rollback test',difficulty:'Normal',questionCount:5,timerSeconds:15});
  const jobKey=randomUUID(),fingerprint=hash(JSON.stringify(requestBody));
  const savedBody={schemaVersion:1,generationId:jobKey,quiz:{...requestBody,questions:[]},rehearsal:true};
  store=new Store(dbPath);store.db.prepare('INSERT INTO jobs VALUES (?,?,?,?,?,?)').run(users[0],jobKey,fingerprint,Date.now(),JSON.stringify(savedBody),200);
  const beforeRollback=contents(store.db),mpBefore=contents(store.db,['mp_schema','mp_rooms','mp_members','mp_requests','mp_limits']);store.close();store=null;
  store=new M3Store(dbPath);requireCheck('m3SourceReadsMigratedDb',equal(contents(store.db),beforeRollback));
  requireCheck('m3SourceLeavesMultiplayerTables',equal(contents(store.db,['mp_schema','mp_rooms','mp_members','mp_requests','mp_limits']),mpBefore));
  requireCheck('m3SourceAcceptsExistingTokens',tokens.every((t,i)=>store.authenticate('Bearer '+t)===users[i]));
  if(revoked){
    let denied=false;try{store.authenticate('Bearer '+revoked);}catch(e){denied=e.code==='AUTH_REQUIRED';}
    requireCheck('exposedTokenRejectedM3Rollback',denied);
  }
  await start(createM3Api);
  requireCheck('rollbackM3Health',(await request(null,'/health')).status===200);
  requireCheck('rollbackNewSavedJobHttpReplay',equal((await request(tokens[0],'/api/quizzes/generate',requestBody,{'Idempotency-Key':jobKey})).body,savedBody));
  for(const job of original.jobs.filter(j=>j.response))requireCheck('rollbackOriginalSavedJobReplay',equal(store.reserve(job.user_id,job.key,job.fingerprint),{status:job.status,body:JSON.parse(job.response)}));
  // M3 legitimately updates its throttle on HTTP; users/jobs must stay exact.
  requireCheck('rollbackPreservesOldAndNewJobs',equal(contents(store.db,['users','jobs']),{users:beforeRollback.users,jobs:beforeRollback.jobs}));
  requireCheck('rollbackPreservesMultiplayerRows',equal(contents(store.db,['mp_schema','mp_rooms','mp_members','mp_requests','mp_limits']),mpBefore));
  await stop();store=new Store(dbPath);
  requireCheck('reupgradePreservesSavedJobs',equal(contents(store.db,['users','jobs']),{users:beforeRollback.users,jobs:beforeRollback.jobs}));
  requireCheck('reupgradeIntegrity',sqlCheck(store.db));await stop();
  // A conflicting table must roll back the whole multiplayer DDL transaction.
  const failureDb=join(staging,'failed-migration.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),failureDb);
  const collision=new DatabaseSync(failureDb);collision.exec('CREATE TABLE mp_rooms(collision TEXT)');collision.close();
  let rejected=false;try {const unexpected=new Store(failureDb);unexpected.close();}catch {rejected=true;}
  const failed=new DatabaseSync(failureDb,{readOnly:true});
  requireCheck('migrationCollisionRejected',rejected);
  requireCheck('failedMigrationPreservesM3',equal(contents(failed),original) && failed.prepare('PRAGMA user_version').get().user_version===version);
  requireCheck('failedMigrationRollsBackLedger',!failed.prepare("SELECT 1 FROM sqlite_master WHERE name IN ('mp_schema','mp_members','mp_requests','mp_limits')").get());
  requireCheck('failedMigrationIntegrity',sqlCheck(failed));failed.close();
  // Real executable/config loader in production, with no provider credential.
  const entryDb=join(staging,'entry.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),entryDb);
  const env={...process.env,HOST:'127.0.0.1',PORT:'0',NODE_ENV:'production',QUIZPK_DB_PATH:entryDb,GEMINI_API_KEY:'',GEMINI_MODEL:'',GEMINI_FREE_TIER_CONFIRMED:'false'};
  const log=join(staging,'entry-private.log');
  const {openSync,closeSync}=await import('node:fs');const fd=openSync(log,'wx',0o600);
  // PORT=0 chooses a kernel-assigned loopback port, never the live 8787 listener.
  child=spawn(process.execPath,[join(release,'src/server.mjs')],{cwd:release,env,stdio:['ignore',fd,fd]});closeSync(fd);
  for(let i=0;i<100;i++){
    await new Promise(r=>setTimeout(r,50));
    if(child.exitCode!==null)throw Error();
    if(readFileSync(log,'utf8').includes('QuizPK backend listening'))break;
  }
  requireCheck('termuxProductionEntrypointStarts',readFileSync(log,'utf8').includes('QuizPK backend listening'));
  const exited=new Promise(r=>child.once('exit',r));child.kill('SIGTERM');await exited;
  requireCheck('termuxGracefulShutdown',child.exitCode===0);child=null;
  const entry=new DatabaseSync(entryDb,{readOnly:true});requireCheck('entrypointMigrationPreservesM3',equal(contents(entry),original));entry.close();
  requireCheck('providerNeverCalled',providerCalls===0);
  // Frozen release and backup stay intact after every migration/rollback check.
  requireCheck('releaseUnchanged',Object.entries(releaseFiles).every(([p,h])=>hash(readFileSync(join(release,p)))===h));
  requireCheck('backupUnchanged',Object.entries(manifest.files).every(([p,h])=>hash(readFileSync(join(backup,p)))===h));
  const result={status:'PASS',checks,checkCount:Object.keys(checks).length,stagingDirectory:staging,releaseDirectory:release,
    backupManifestSha256:hash(readFileSync(join(backup,'manifest.json'))),
    releaseManifestSha256:hash(readFileSync(join(staging,'release-manifest.json'))),
    revokedTokenVerified:Boolean(revoked),
    existingTokenCount:tokens.length,originalCachedJobsChecked:original.jobs.filter(j=>j.response).length,
    providerCalls,liveDatabaseOpened:false,liveDeploymentPerformed:false};
  writeFileSync(join(staging,'result.json'),JSON.stringify(result,null,2)+'\n',{mode:0o600});console.log(JSON.stringify(result,null,2));
} catch {
  console.error(JSON.stringify({status:'FAIL',failedPhase:phase,checks,liveDeploymentPerformed:false}));process.exitCode=1;
} finally {
  await stop();if(child){child.kill('SIGTERM');}
}
