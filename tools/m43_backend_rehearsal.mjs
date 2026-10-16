// Private operator rehearsal. Live SQLite is never opened; imports use frozen source.
import {DatabaseSync} from 'node:sqlite';
import {createHash,randomBytes,randomUUID} from 'node:crypto';
import {readFileSync,writeFileSync,copyFileSync,mkdirSync,readdirSync,realpathSync,statSync,openSync,closeSync} from 'node:fs';
import {join,resolve} from 'node:path';
import {pathToFileURL} from 'node:url';
import {spawn} from 'node:child_process';

const root='/data/data/com.termux/files/home/.local/share/quizpk-backups';
const hash=b=>createHash('sha256').update(b).digest('hex');
const equal=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const checks={};let phase='arguments',store,server,child,providerCalls=0;
function check(name,value){phase=name;if(!value)throw Error();checks[name]=true;}
function allRows(db,tables){return Object.fromEntries(tables.map(t=>[t,db.prepare('SELECT * FROM "'+t+'" ORDER BY rowid').all()]));}
function tables(db){return db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name").all().map(r=>r.name);}
function schema(db){return db.prepare("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name").all();}
function integrity(db){return db.prepare('PRAGMA integrity_check').get().integrity_check==='ok'&&!db.prepare('PRAGMA foreign_key_check').all().length;}
function privatePath(p){const r=realpathSync(p);check('privatePaths',r.startsWith(root+'/')&&!(statSync(r).mode&0o077));return r;}
function fileTree(dir){return readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?fileTree(join(dir,e.name)):[join(dir,e.name)]);}
async function stop(){if(server){server.closeIdleConnections();await new Promise(r=>server.close(r));server=null;}if(store){store.close();store=null;}}

try{
 process.umask(0o077);
 const backup=privatePath(process.argv[2]),prep=privatePath(process.argv[3]);
 const manifest=JSON.parse(readFileSync(join(backup,'manifest.json'))),release=join(prep,'release');
 const releaseManifest=JSON.parse(readFileSync(join(prep,'release-manifest.json')));
 const protocolManifest=JSON.parse(readFileSync(join(prep,'protocol-manifest.json')));
 check('protocolHashes',Object.entries(protocolManifest).every(([p,h])=>hash(readFileSync(join(prep,'protocol',p)))===h));
 check('backupHashes',Object.entries(manifest.files).every(([p,h])=>hash(readFileSync(join(backup,p)))===h));
 check('backupIntegrityReceipt',Object.values(manifest.checks).every(v=>v===true));
 check('snapshotHash',hash(readFileSync(join(backup,'quizpk.sqlite')))===manifest.snapshotSha256);
 check('releaseHashes',Object.entries(releaseManifest).every(([p,h])=>hash(readFileSync(join(release,p)))===h));
 check('releaseExactFileSet',equal(fileTree(release).map(p=>p.slice(release.length+1)).sort(),Object.keys(releaseManifest).sort()));
 check('packageUnchanged',releaseManifest['package.json']===hash(readFileSync(join(backup,'host/package.json'))));
 const staging=backup+'-m43-staging';mkdirSync(staging,{mode:0o700});
 const dbPath=join(staging,'game.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),dbPath);
 const base=new DatabaseSync(dbPath,{readOnly:true}),oldTables=tables(base),original=allRows(base,oldTables),oldSchema=schema(base),version=base.prepare('PRAGMA user_version').get().user_version;
 check('restoredSchemaOne',base.prepare('SELECT max(version) v FROM mp_schema').get().v===1);
 check('restoredIntegrity',integrity(base));base.close();
 const {Store}=await import(pathToFileURL(join(release,'src/store.mjs')));
 const {createApi}=await import(pathToFileURL(join(release,'src/server.mjs')));
 const {RoomService}=await import(pathToFileURL(join(release,'src/multiplayer/rooms.mjs')));
 const {validateRequest}=await import(pathToFileURL(join(release,'src/contract.mjs')));
 let clock=Date.parse(manifest.createdUtc);
 store=new Store(dbPath,()=>clock);
 check('migrationAllExistingRowsEqual',oldTables.filter(t=>t!=='mp_schema').every(t=>equal(allRows(store.db,[t])[t],original[t])));
 const ledger=store.db.prepare('SELECT * FROM mp_schema ORDER BY version').all();
 check('migrationLedgerAppendOnly',ledger.length===original.mp_schema.length+1&&original.mp_schema.every(r=>equal(ledger.find(n=>n.version===r.version),r))&&ledger.at(-1).version===2);
 check('migrationAllExistingSchemaEqual',oldSchema.every(s=>equal(schema(store.db).find(n=>n.name===s.name&&n.type===s.type),s)));
 check('migrationUserVersionEqual',store.db.prepare('PRAGMA user_version').get().user_version===version);
 check('migrationSchemaTwo',store.db.prepare('SELECT max(version) v FROM mp_schema').get().v===2);
 check('migrationFiveTablesAdded',tables(store.db).length===oldTables.length+5);
 check('migrationIntegrity',integrity(store.db));
 const tokenNames=readdirSync(join(backup,'host/private')).filter(n=>n.endsWith('.token'));
 const tokens=tokenNames.map(n=>readFileSync(join(backup,'host/private',n),'utf8').trim());
 const users=tokens.map(t=>store.authenticate('Bearer '+t));
 check('currentTokensEnabledDistinct',tokens.length===2&&new Set(users).size===2);
 const exposed=readFileSync(privatePath(process.argv[4]),'utf8').trim();
 const replacement=readFileSync(privatePath(process.argv[5]),'utf8').trim();
 check('rotatedTokenStillCurrent',tokens[tokenNames.indexOf('phone01.token')]===replacement&&store.authenticate('Bearer '+replacement)==='phone01');
 function denied(token){try{store.authenticate('Bearer '+token);return false;}catch(e){return e.code==='AUTH_REQUIRED';}}
 check('exposedTokenRejected',denied(exposed));
 const legacyTokens=tokenNames.map(n=>readFileSync(join(backup,'host/private',n),'utf8').trim());
 check('allOriginalJobRowsPresent',equal(allRows(store.db,['jobs']).jobs,original.jobs));
 let cachedCount=0;
 for(const j of original.jobs.filter(j=>j.response)){check('savedAiJobsReplay',equal(store.reserve(j.user_id,j.key,j.fingerprint),{status:j.status,body:JSON.parse(j.response)}));cachedCount++;}
 check('savedAiJobsAvailable',cachedCount>0);
 const provider=async()=>{providerCalls++;throw Error('PROVIDER_FORBIDDEN');};let origin;
 async function start(enabled=true){server=createApi({store,provider,model:'rehearsal',production:true,roomOptions:{monotonic:()=>clock,classicEnabled:enabled}});await new Promise(r=>server.listen(0,'127.0.0.1',r));check('isolatedEphemeralPort',server.address().port!==8787);origin='http://127.0.0.1:'+server.address().port;}
 async function request(token,path,body,headers={}){
  const r=await fetch(origin+path,{method:body===undefined?'GET':'POST',redirect:'error',headers:{'X-Forwarded-Proto':'https',...(token?{Authorization:'Bearer '+token}:{}),'Content-Type':'application/json','X-QuizPK-Features':'classic-v1',...headers},...(body===undefined?{}:{body:JSON.stringify(body)}),signal:AbortSignal.timeout(5000)});
  return {status:r.status,body:await r.json()};
 }
 const command=(type,payload={},extra={})=>({protocolVersion:1,requestId:randomUUID(),issuedAtMs:clock,type,payload,...extra});
 await start();
 check('productionHttpsEnforced',(await request(null,'/health',undefined,{'X-Forwarded-Proto':''})).body.error.code==='HTTPS_REQUIRED');
 check('healthCompatible',(await request(null,'/health')).status===200);
 check('unauthenticatedRejected',(await request(null,'/api/rooms/current')).status===401);
 check('exposedTokenHttpRejected',(await request(exposed,'/api/rooms/current')).status===401);
 for(const t of legacyTokens)check('currentTokensHttpCompatible',(await request(t,'/api/rooms/current',undefined,{'X-QuizPK-Features':''})).status===200);
 const tokenForUser=new Map(users.map((u,i)=>[u,tokens[i]]));let httpCached=0;
 for(const j of original.jobs.filter(j=>j.response&&j.status===200&&tokenForUser.has(j.user_id))){
  const q=JSON.parse(j.response).quiz;
  if(!q)continue;
  const req=validateRequest({topic:q.topic,difficulty:q.difficulty,questionCount:q.questionCount,timerSeconds:q.timerSeconds});
  if(hash(JSON.stringify(req))!==j.fingerprint)continue;
  const r=await request(tokenForUser.get(j.user_id),'/api/quizzes/generate',req,{'Idempotency-Key':j.key});
  check('originalAiHttpCacheReplay',r.status===j.status&&equal(r.body,JSON.parse(j.response)));httpCached++;
 }
 check('originalAiHttpReplayCovered',httpCached>0);
 check('m3AuthStillRequired',(await request(null,'/api/quizzes/generate',{}, {'Idempotency-Key':randomUUID()})).status===401);
 // Synthetic identities exist only in this isolated DB; existing users/rooms stay owned by their original IDs.
 const gameTokens=[randomBytes(32).toString('base64url'),randomBytes(32).toString('base64url')],gameUsers=[randomUUID(),randomUUID()];
 gameUsers.forEach((u,i)=>store.addUser(u,gameTokens[i]));
 const settings={category:'Science',difficulty:'Normal',questionCount:5,timerSeconds:10};
 let id,path;
 async function lobby(mode='CLASSIC'){
  const c=await request(gameTokens[0],'/api/rooms',command('room.create',{mode,displayName:'Staging host',settings}));check('lobbyCreate'+mode,c.status===200);
  id=c.body.result.roomId;path='/api/rooms/'+id;
  check('lobbyJoin'+mode,(await request(gameTokens[1],'/api/rooms/join',command('room.join',{code:c.body.result.inviteCode,displayName:'Staging player'}))).status===200);
 }
 const snapshot=async(i=0,features='classic-v1')=>(await request(gameTokens[i],path,undefined,{'X-QuizPK-Features':features})).body;
 const ready=async()=>{for(const t of gameTokens)check('readySync',(await request(t,path+'/ready',command('room.ready',{ready:true}))).status===200);};
 const advance=async ms=>{clock+=ms;await new Promise(r=>setTimeout(r,280));};
 async function launch(){await ready();const s=await snapshot();const c=command('match.start',{}, {expectedRevision:s.room.revision});check('hostStart',(await request(gameTokens[0],path+'/match/start',c)).status===200);check('startDuplicateReceipt',(await request(gameTokens[0],path+'/match/start',c)).status===200);}
 function safe(s){const r=s.room.match.round;check('unrevealedAnswerKeyOmitted',r.reveal===null&&(!r.question||equal(Object.keys(r.question).sort(),['options','text'])));}
 await lobby();let s=await snapshot(0,'');check('oldLobbyShapeCompatible',!('match' in s.room)&&s.capabilities.matches===false&&s.room.members.length===2);
 const changed={...settings,timerSeconds:15};
 check('ownerSettingsCompatible',(await request(gameTokens[0],path+'/settings',command('room.settings',changed,{expectedRevision:s.room.revision}))).status===200);
 s=await snapshot();check('settingsResetReady',s.room.members.every(p=>!p.ready));
 check('resetTimer',(await request(gameTokens[0],path+'/settings',command('room.settings',settings,{expectedRevision:s.room.revision}))).status===200);
 await ready();s=await snapshot();
 check('contestantCannotStart',(await request(gameTokens[1],path+'/match/start',command('match.start',{}, {expectedRevision:s.room.revision}))).status===403);
 await launch();safe(await snapshot());let expected=0;
 for(let n=0;n<5;n++){
  await advance(3000);
  check('timerProgressesWithoutPolling',store.db.prepare('SELECT phase FROM mp_matches WHERE room_id=?').get(id).phase==='ROUND_LIVE');
  s=await snapshot();const other=await snapshot(1),round=s.room.match.round;
  safe(s);safe(other);check('sharedQuestionDeadline',equal(round.question,other.room.match.round.question)&&round.deadlineMs===other.room.match.round.deadlineMs);
  const k=store.db.prepare('SELECT correct_index FROM mp_answer_keys WHERE round_id=?').get(round.id).correct_index;
  await advance(100);const wrong=command('match.answer',{roundId:round.id,option:(k+1)%4});
  check('singleAnswerAccepted',(await request(gameTokens[0],path+'/match/answer',wrong)).status===200);
  check('exactAnswerRetry',(await request(gameTokens[0],path+'/match/answer',wrong)).status===200);
  check('changedAnswerRejected',(await request(gameTokens[0],path+'/match/answer',command('match.answer',{roundId:round.id,option:k}))).body.error.code==='ANSWER_ALREADY_ACCEPTED');
  await advance(900);
  check('correctAnswerAccepted',(await request(gameTokens[1],path+'/match/answer',command('match.answer',{roundId:round.id,option:k}))).status===200);
  s=await snapshot();safe(s);check('otherChoicePrivate',s.room.match.round.ownOption===(k+1)%4&&s.room.match.standings.every(p=>p.score===expected||p.score===0));
  check('pendingGradesPrivate',store.db.prepare('SELECT count(*) n FROM mp_answers WHERE round_id=? AND (correct IS NOT NULL OR points IS NOT NULL)').get(round.id).n===0);
  await advance(9000);s=await snapshot();const reveal=s.room.match.round.reveal;expected+=145;
  check('safeDeadlineReveal',s.room.state==='ROUND_REVEALED'&&reveal.correctIndex===k);
  check('wrongFirstVsFastestCorrect',reveal.firstSubmission===s.self.participantId&&reveal.fastestCorrect===other.self.participantId);
  check('authoritativeScoring',s.room.match.standings.find(p=>p.participantId===other.self.participantId).score===expected&&s.room.match.standings.find(p=>p.participantId===s.self.participantId).score===0);
  await advance(5000);
 }
 s=await snapshot();check('automaticFinalResults',s.room.state==='MATCH_FINISHED'&&s.room.match.finishReason==='COMPLETED');
 const final=s.room.match,epoch=s.authority.instanceId;
 await stop();store=new Store(dbPath,()=>clock);await start();s=await snapshot();
 check('completedResultsDurable',equal(s.room.match,final)&&s.authority.instanceId!==epoch);
 const newRequest=validateRequest({topic:'Isolated post-upgrade cache',difficulty:'Normal',questionCount:5,timerSeconds:10});
 const newKey=randomUUID(),newBody=JSON.parse(original.jobs.find(j=>j.status===200&&j.response).response);
 newBody.generationId=newKey;
 store.db.prepare('INSERT INTO jobs VALUES (?,?,?,?,?,?)').run(users[0],newKey,hash(JSON.stringify(newRequest)),clock,JSON.stringify(newBody),200);
 for(const t of gameTokens)check('resultsDismissal',(await request(t,path+'/leave',command('room.leave'))).status===200);
 await lobby();await launch();await advance(3000);s=await snapshot();
 check('unansweredRoundLive',s.room.state==='ROUND_LIVE');await advance(10000);s=await snapshot();
 check('timeoutsZero',s.room.match.round.reveal.answers.every(a=>a.option===null&&a.points===0)&&s.room.match.round.reveal.fastestCorrect===null);
 await advance(5000);await advance(3000);s=await snapshot();const liveRound=s.room.match.round;
 check('acceptedBeforeRestart',(await request(gameTokens[0],path+'/match/answer',command('match.answer',{roundId:liveRound.id,option:0}))).status===200);
 const accepted=store.db.prepare('SELECT * FROM mp_answers WHERE round_id=?').all(liveRound.id);
 await stop();const fallbackDb=join(staging,'fallback.sqlite');copyFileSync(dbPath,fallbackDb);
 store=new Store(fallbackDb,()=>clock);await start(false);s=await snapshot();
 check('disabledFallbackInterruptsSafely',s.room.state==='MATCH_FINISHED'&&s.room.match.finishReason==='INTERRUPTED'&&s.room.match.round.reveal===null&&s.room.match.round.question===null);
 check('restartDoesNotGradeUnrevealed',equal(store.db.prepare('SELECT * FROM mp_answers WHERE round_id=?').all(liveRound.id),accepted)&&s.room.match.standings.every(p=>p.score===0));
 for(const t of gameTokens)await request(t,path+'/leave',command('room.leave'));
 await lobby();await ready();s=await snapshot();
 check('disabledFallbackOrdinaryClassicLobby',s.room.state==='LOBBY'&&s.capabilities.matches===false);
 check('disabledFallbackStartRejected',(await request(gameTokens[0],path+'/match/start',command('match.start',{}, {expectedRevision:s.room.revision}))).body.error.code==='CLASSIC_UNAVAILABLE');
 await request(gameTokens[0],path+'/leave',command('room.leave'));
 await lobby('MODERATOR');s=await snapshot();
 check('moderatorLobbySeparate',s.self.role==='MODERATOR'&&!s.capabilities.matches&&s.room.members.filter(p=>p.role==='CONTESTANT').length===1);
 check('moderatorCannotReady',(await request(gameTokens[0],path+'/ready',command('room.ready',{ready:true}))).status===403);
 for(const t of tokens)check('fallbackCurrentTokensCompatible',(await request(t,'/api/rooms/current',undefined,{'X-QuizPK-Features':''})).status===200);
 check('fallbackExposedTokenRejected',(await request(exposed,'/api/rooms/current')).status===401);
 check('allOriginalUsersPreserved',original.users.every(u=>equal(store.db.prepare('SELECT * FROM users WHERE id=?').get(u.id),u)));
 check('allOriginalJobsPreserved',original.jobs.every(j=>equal(store.db.prepare('SELECT * FROM jobs WHERE user_id=? AND key=?').get(j.user_id,j.key),j)));
 check('fallbackKeepsPostUpgradeSavedJob',equal(await request(tokens[0],'/api/quizzes/generate',newRequest,{'Idempotency-Key':newKey}),{status:200,body:newBody}));
 check('fallbackAllSavedJobsCount',store.db.prepare('SELECT count(*) n FROM jobs').get().n===original.jobs.length+1);
 check('finalIntegrity',integrity(store.db));
 await stop();
 // Unmodified deployed M4.2 must refuse schema 2 rather than silently downgrade.
 const rejectedDb=join(staging,'old-source-refusal.sqlite');copyFileSync(dbPath,rejectedDb);
 const {Store:OldStore}=await import(pathToFileURL(join(backup,'host/src/store.mjs')));
 let rejected=false;try{const old=new OldStore(rejectedDb);old.close();}catch(e){rejected=e.message==='UNSUPPORTED_ROOM_SCHEMA';}
 check('oldSourceRefusesSchemaTwo',rejected);
 // A failed additive migration must preserve all baseline rows and schema.
 const collisionDb=join(staging,'collision.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),collisionDb);
 let db=new DatabaseSync(collisionDb);db.exec('CREATE TABLE mp_answers(collision TEXT)');db.close();
 rejected=false;try{const unexpected=new Store(collisionDb);unexpected.close();}catch{rejected=true;}
 db=new DatabaseSync(collisionDb,{readOnly:true});
 check('migrationFailureAtomic',rejected&&equal(allRows(db,oldTables),original)&&db.prepare('SELECT max(version) v FROM mp_schema').get().v===1&&!db.prepare("SELECT 1 FROM sqlite_master WHERE name='mp_matches'").get());db.close();
 // Real Termux entrypoint and config loader, isolated DB and explicit disabled flag.
 const entryDb=join(staging,'entry.sqlite');copyFileSync(join(backup,'quizpk.sqlite'),entryDb);
 db=new DatabaseSync(entryDb);db.prepare('INSERT INTO users(id,token_hash) VALUES (?,?)').run(gameUsers[0],hash(gameTokens[0]));db.close();
 const entryHook=join(staging,'entry-port.mjs');
 writeFileSync(entryHook,"import {Server} from 'node:net';const listen=Server.prototype.listen;Server.prototype.listen=function(...args){this.once('listening',()=>console.log('STAGING_PORT='+this.address().port));return listen.apply(this,args);};\n",{mode:0o600});
 const log=join(staging,'entry-private.log'),fd=openSync(log,'wx',0o600);
 const env={...process.env,HOST:'127.0.0.1',PORT:'0',NODE_ENV:'production',QUIZPK_DB_PATH:entryDb,QUIZPK_CLASSIC_ENABLED:'false',GEMINI_API_KEY:'',GEMINI_MODEL:'',GEMINI_FREE_TIER_CONFIRMED:'false'};
 child=spawn(process.execPath,['--import',entryHook,join(release,'src/server.mjs')],{cwd:release,env,stdio:['ignore',fd,fd]});closeSync(fd);
 for(let i=0;i<100;i++){await new Promise(r=>setTimeout(r,50));if(child.exitCode!==null)throw Error();if(readFileSync(log,'utf8').includes('QuizPK backend listening'))break;}
 check('termuxProductionEntrypoint',readFileSync(log,'utf8').includes('QuizPK backend listening'));
 const entryPort=Number(/STAGING_PORT=(\d+)/.exec(readFileSync(log,'utf8'))?.[1]);
 check('entrypointEphemeralPort',entryPort>0&&entryPort!==8787);origin='http://127.0.0.1:'+entryPort;clock=Date.now();
 check('entrypointProductionHttps',(await request(null,'/health',undefined,{'X-Forwarded-Proto':''})).status===400);
 const entryRoom=await request(gameTokens[0],'/api/rooms',command('room.create',{mode:'CLASSIC',displayName:'Entry fixture',settings}));
 check('entrypointLobbyWorks',entryRoom.status===200);const ep='/api/rooms/'+entryRoom.body.result.roomId;
 const es=(await request(gameTokens[0],ep)).body;
 check('entrypointReadsDisabledEnvironmentFlag',es.capabilities.matches===false&&(await request(gameTokens[0],ep+'/match/start',command('match.start',{}, {expectedRevision:es.room.revision}))).body.error.code==='CLASSIC_UNAVAILABLE');
 const exit=new Promise(r=>child.once('exit',r));child.kill('SIGTERM');await exit;check('termuxGracefulShutdown',child.exitCode===0);child=null;
 db=new DatabaseSync(entryDb,{readOnly:true});check('entrypointKeepsM3Data',original.users.every(u=>equal(db.prepare('SELECT * FROM users WHERE id=?').get(u.id),u))&&equal(allRows(db,['jobs']).jobs,original.jobs));db.close();
 check('providerNeverCalled',providerCalls===0);
 check('backupStillIntact',Object.entries(manifest.files).every(([p,h])=>hash(readFileSync(join(backup,p)))===h));
 check('releaseStillIntact',Object.entries(releaseManifest).every(([p,h])=>hash(readFileSync(join(release,p)))===h));
 const result={status:'PASS',checks,checkCount:Object.keys(checks).length,backupDirectory:backup,preparationDirectory:prep,stagingDirectory:staging,backupManifestSha256:hash(readFileSync(join(backup,'manifest.json'))),releaseManifestSha256:hash(readFileSync(join(prep,'release-manifest.json'))),protocolManifestSha256:hash(readFileSync(join(prep,'protocol-manifest.json'))),contextSha256:hash(readFileSync(join(prep,'context.json'))),rehearsalScriptSha256:hash(readFileSync(new URL(import.meta.url))),existingTokenCount:tokens.length,originalSavedJobs:cachedCount,originalHttpCacheReplays:httpCached,providerCalls,liveMigrationPerformed:false};
 writeFileSync(join(staging,'result.json'),JSON.stringify(result,null,2)+'\n',{mode:0o600});console.log(JSON.stringify(result,null,2));
}catch{console.error(JSON.stringify({status:'FAIL',failedPhase:phase,checks,liveMigrationPerformed:false}));process.exitCode=1;}
finally{await stop();if(child){child.kill('SIGTERM');}}
