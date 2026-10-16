import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID} from 'node:crypto';
import {mkdtempSync,rmSync,readFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {Store} from '../src/store.mjs';
import {RoomService} from '../src/multiplayer/rooms.mjs';
import {createApi} from '../src/server.mjs';
import {validateCommand} from '../src/multiplayer/protocol.mjs';
import {CLASSIC,score,validateBank} from '../src/multiplayer/classic.mjs';
import {DatabaseSync} from 'node:sqlite';
import {migrateMultiplayer as migrateV1} from './fixtures/m42-migrations.mjs';

const epoch=1791849600000;
const settings={category:'Science',difficulty:'Normal',questionCount:5,timerSeconds:10};
const bank=Array.from({length:20},(_,i)=>({id:'private-'+i,category:'Science',difficulty:'Normal',
 text:'Server selected private question number '+i,options:['Alpha','Beta','Gamma','Delta'],correct:2,
 explanation:'PRIVATE_EXPLANATION_CANARY_'+i}));
const token=i=>('synthetic_classic_identity_'+i+'_').padEnd(64,'x');
function fixture({path=':memory:',enabled=true}={}) {
 let time=epoch,wall=epoch,store=new Store(path,()=>wall);
 if(!store.db.prepare('SELECT 1 FROM users LIMIT 1').get())for(let i=0;i<12;i++)store.addUser('u'+i,token(i));
 let rooms=new RoomService(store,{bank,monotonic:()=>time,classicEnabled:enabled});rooms.start();
 const command=(user,type,payload={},id=null,extra={})=>rooms.command('u'+user,
  validateCommand({protocolVersion:1,requestId:randomUUID(),issuedAtMs:rooms.classic.get(id)?rooms.classic.time():wall,type,payload,...extra},type),id);
 const snapshot=(user,id)=>rooms.snapshot('u'+user,id,true);
 const lobby=(mode='CLASSIC',config=settings)=>{
  const r=command(0,'room.create',{mode,displayName:'Host',settings:config}).result;
  command(1,'room.join',{code:r.inviteCode,displayName:'Player'});
  if(mode==='CLASSIC')command(0,'room.ready',{ready:true},r.roomId);
  command(1,'room.ready',{ready:true},r.roomId);return r.roomId;
 };
 const start=id=>command(0,'match.start',{},id,{expectedRevision:snapshot(0,id).room.revision});
 const advance=ms=>{time+=ms;wall+=ms;rooms.tick();};
 const key=id=>{const r=snapshot(0,id).room.match.round;return store.db.prepare('SELECT correct_index FROM mp_answer_keys WHERE round_id=?').get(r.id).correct_index;};
 return {command,snapshot,lobby,start,advance,key,store:()=>store,rooms:()=>rooms,
  wallShift:ms=>wall+=ms,close:()=>store.close(),restart:()=>{store.close();store=new Store(path,()=>wall);rooms=new RoomService(store,{bank,monotonic:()=>time,classicEnabled:enabled});rooms.start();}};
}
function safeLive(s) {
 const text=JSON.stringify(s);
 for(const forbidden of ['correctIndex','PRIVATE_EXPLANATION_CANARY','selected_option','token_hash','user_id','question_json'])assert.ok(!text.includes(forbidden),forbidden);
 assert.equal(s.room.match.round.reveal,null);
}

test('Classic launch is host-only, requires two ready online contestants and sufficient server stock',()=>{
 const f=fixture();try{
  const one=f.command(0,'room.create',{mode:'CLASSIC',displayName:'Host',settings}).result.roomId;
  assert.throws(()=>f.start(one),/TOO_FEW_CONTESTANTS/);
  f.command(1,'room.join',{code:f.snapshot(0,one).room.inviteCode,displayName:'Player'});
  assert.throws(()=>f.command(1,'match.start',{},one,{expectedRevision:f.snapshot(1,one).room.revision}),/FORBIDDEN/);
  assert.throws(()=>f.start(one),/PLAYERS_NOT_READY/);
  f.command(0,'room.ready',{ready:true},one);f.command(1,'room.ready',{ready:true},one);
  const v=f.snapshot(0,one).room.revision;
  assert.throws(()=>f.command(0,'match.start',{},one,{expectedRevision:v-1}),/REVISION_CONFLICT/);
  f.command(0,'room.settings',{...settings,category:'Space'},one,{expectedRevision:v});
  f.command(0,'room.ready',{ready:true},one);f.command(1,'room.ready',{ready:true},one);
  assert.throws(()=>f.start(one),/INSUFFICIENT_QUESTIONS/);
  assert.equal(f.store().db.prepare('SELECT count(*) n FROM mp_matches').get().n,0);
 }finally{f.close();}
});
test('Moderator rooms never enter Classic gameplay',()=>{
 const f=fixture();try{const id=f.lobby('MODERATOR');assert.throws(()=>f.start(id),/CLASSIC_UNAVAILABLE/);
  assert.equal(f.snapshot(0,id).capabilities.matches,false);
 }finally{f.close();}
});
test('server chooses unique frozen questions; both clients share releases and deadlines with no prepared answer key',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);
  let a=f.snapshot(0,id),b=f.snapshot(1,id);assert.deepEqual(a.room.match,b.room.match);
  assert.equal(a.room.match.round.question,null);safeLive(a);
  const rows=f.store().db.prepare('SELECT question_json FROM mp_rounds').all();assert.equal(new Set(rows.map(r=>r.question_json)).size,5);
  f.advance(CLASSIC.prepareMs);a=f.snapshot(0,id);b=f.snapshot(1,id);
  assert.equal(a.room.state,'ROUND_LIVE');assert.deepEqual(a.room.match.round,b.room.match.round);safeLive(a);safeLive(b);
  assert.equal(a.room.match.round.deadlineMs-a.room.match.round.startedAtMs,10000);
  assert.throws(()=>f.command(2,'room.join',{code:a.room.inviteCode,displayName:'Late'}),/ROOM_NOT_FOUND/);
  assert.throws(()=>f.command(0,'room.settings',settings,id,{expectedRevision:a.room.revision}),/MATCH_IN_PROGRESS/);
 }finally{f.close();}
});
test('wrong first submission never earns fastest-correct; grades and scores stay private until reveal',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);
  const r=f.snapshot(0,id).room.match.round,k=f.key(id);
  f.advance(100);f.command(0,'match.answer',{roundId:r.id,option:(k+1)%4},id);
  f.advance(2400);f.command(1,'match.answer',{roundId:r.id,option:k},id);
  const live=f.snapshot(0,id);safeLive(live);
  assert.equal(live.room.match.round.submitted.length,2);assert.ok(live.room.match.standings.every(p=>p.score===0));
  assert.equal(live.room.match.round.ownOption,(k+1)%4);assert.equal(f.snapshot(1,id).room.match.round.ownOption,k);
  f.advance(7500);const reveal=f.snapshot(1,id).room.match.round.reveal;
  assert.equal(reveal.correctIndex,k);assert.notEqual(reveal.firstSubmission,reveal.fastestCorrect);
  assert.equal(reveal.answers.find(p=>p.participantId===reveal.fastestCorrect).points,137);
  assert.equal(reveal.answers.find(p=>p.participantId===reveal.firstSubmission).points,0);
  const before=JSON.stringify(f.snapshot(0,id).room.match.standings);f.rooms().tick();f.rooms().tick();
  assert.equal(JSON.stringify(f.snapshot(0,id).room.match.standings),before);
 }finally{f.close();}
});
test('same command replay is immutable; conflicting duplicate answers cannot replace a choice or score twice',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);
  const r=f.snapshot(0,id).room.match.round,c=validateCommand({protocolVersion:1,requestId:randomUUID(),issuedAtMs:r.startedAtMs,
   type:'match.answer',payload:{roundId:r.id,option:0}},'match.answer');
  const first=f.rooms().command('u0',c,id);assert.deepEqual(f.rooms().command('u0',c,id),first);
  assert.throws(()=>f.command(0,'match.answer',{roundId:r.id,option:1},id),/ANSWER_ALREADY_ACCEPTED/);
  assert.throws(()=>f.rooms().command('u0',{...c,payload:{...c.payload,option:1}},id),/IDEMPOTENCY_CONFLICT/);
  f.advance(10000);assert.deepEqual(f.rooms().command('u0',c,id),first);
  assert.equal(f.store().db.prepare('SELECT count(*) n FROM mp_answers').get().n,1);
 }finally{f.close();}
});
test('acceptance and grading use one timestamp when the clock crosses the deadline',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);
  const r=f.snapshot(0,id).room.match.round,k=f.key(id),authority=f.rooms().classic;
  let now=r.deadlineMs-1;
  authority.time=()=>now++;
  authority.answer('u0',id,{roundId:r.id,option:k});
  const accepted=f.store().db.prepare('SELECT accepted_at,elapsed_ms FROM mp_answers WHERE round_id=?').get(r.id);
  assert.equal(accepted.accepted_at,r.deadlineMs-1);assert.equal(accepted.elapsed_ms,9999);
  authority.advance();
  const result=f.store().db.prepare('SELECT correct,points FROM mp_answers WHERE round_id=?').get(r.id);
  assert.equal(result.correct,1);assert.equal(result.points,100);
 }finally{f.close();}
});
test('deadline boundary, stale rounds, forged grading and phase controls fail closed',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);const before=f.snapshot(0,id).room.match.round;
  assert.throws(()=>f.command(0,'match.answer',{roundId:before.id,option:0},id),/ROUND_NOT_LIVE/);
  f.advance(CLASSIC.prepareMs);
  assert.throws(()=>f.command(0,'match.answer',{roundId:randomUUID(),option:0},id),/STALE_ROUND/);
  for(const patch of [{score:150},{correct:true},{elapsedMs:0},{userId:'u1'}])
   assert.throws(()=>f.command(0,'match.answer',{roundId:before.id,option:0,...patch},id),/INVALID_COMMAND/);
  f.advance(10000);assert.throws(()=>f.command(0,'match.answer',{roundId:before.id,option:0},id),/ROUND_NOT_LIVE|ANSWER_TOO_LATE/);
  const reveal=f.snapshot(0,id).room.match.round.reveal;assert.equal(reveal.fastestCorrect,null);assert.equal(reveal.firstSubmission,null);
  assert.ok(reveal.answers.every(a=>a.points===0 && a.option===null));
 }finally{f.close();}
});
test('Classic clocks and scoring ignore phone timestamps and server wall-clock adjustments',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);
  const r=f.snapshot(0,id).room.match.round;f.advance(2500);f.wallShift(-3600000);
  f.rooms().command('u0',validateCommand({protocolVersion:1,requestId:randomUUID(),issuedAtMs:r.startedAtMs,
   type:'match.answer',payload:{roundId:r.id,option:f.key(id)}},'match.answer'),id);f.advance(7500);
  assert.equal(f.snapshot(0,id).room.match.round.reveal.answers.find(a=>a.option!==null).points,137);
 }finally{f.close();}
});
test('automatic progression completes all rounds, stable tied ranks and authenticated final recovery/dismissal',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);
  for(let i=0;i<5;i++){
   f.advance(CLASSIC.prepareMs);const r=f.snapshot(0,id).room.match.round;
   f.command(0,'match.answer',{roundId:r.id,option:f.key(id)},id);f.command(1,'match.answer',{roundId:r.id,option:f.key(id)},id);
   f.advance(10000);f.advance(CLASSIC.revealMs);
  }
  const done=f.snapshot(0,id);assert.equal(done.room.state,'MATCH_FINISHED');assert.equal(done.room.match.finishReason,'COMPLETED');
  assert.equal(done.room.match.standings[0].score,750);assert.equal(done.room.match.standings[0].correctCount,5);
  assert.equal(done.room.match.standings[0].fastestWins,5);assert.equal(done.room.match.standings[0].accuracy,100);
  assert.equal(f.rooms().snapshot('u1',null,true).room.id,id);
  assert.throws(()=>f.snapshot(2,id),/ROOM_NOT_FOUND/);
  f.command(0,'room.leave',{},id);assert.equal(f.rooms().snapshot('u0',null,true).room,null);
  assert.equal(f.snapshot(0,id).room.match.finishReason,'COMPLETED');
 }finally{f.close();}
});
test('equal sporting totals share rank; deterministic participant order does not break a tie',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs+10000);
  // A suspended prepared round safely interrupts; both zero totals tie.
  const s=f.snapshot(0,id);assert.equal(s.room.match.finishReason,'INTERRUPTED');
  assert.deepEqual(s.room.match.standings.map(p=>p.rank),[1,1]);
 }finally{f.close();}
});
test('interrupted scoring transaction rolls back every grade and total; retry awards once',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);
  const r=f.snapshot(0,id).room.match.round;f.command(0,'match.answer',{roundId:r.id,option:f.key(id)},id);
  f.store().db.exec("CREATE TRIGGER synthetic_grade_failure BEFORE UPDATE OF score ON mp_match_players BEGIN SELECT RAISE(ABORT,'synthetic_abort'); END");
  assert.throws(()=>f.advance(10000));
  assert.equal(f.store().db.prepare('SELECT correct FROM mp_answers WHERE round_id=?').get(r.id).correct,null);
  assert.equal(f.store().db.prepare('SELECT sum(score) total FROM mp_match_players').get().total,0);
  assert.equal(f.store().db.prepare('SELECT phase FROM mp_matches WHERE room_id=?').get(id).phase,'ROUND_LIVE');
  f.store().db.exec('DROP TRIGGER synthetic_grade_failure');f.rooms().tick();f.rooms().tick();
  assert.equal(f.store().db.prepare('SELECT sum(score) total FROM mp_match_players').get().total,150);
 }finally{f.close();}
});
test('restart preserves revealed scores, cancels unrevealed grading, changes epoch and preserves M3 rows',()=>{
 const dir=mkdtempSync(join(tmpdir(),'quizpk-classic-restart-'));const f=fixture({path:join(dir,'synthetic.sqlite')});
 try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);let r=f.snapshot(0,id).room.match.round;
  f.command(0,'match.answer',{roundId:r.id,option:f.key(id)},id);f.advance(10000);f.advance(CLASSIC.revealMs);f.advance(CLASSIC.prepareMs);
  r=f.snapshot(0,id).room.match.round;f.command(1,'match.answer',{roundId:r.id,option:f.key(id)},id);
  const before=f.snapshot(0,id),identities=JSON.stringify(f.store().db.prepare('SELECT * FROM users').all());
  f.restart();const after=f.snapshot(1,id);assert.notEqual(after.authority.instanceId,before.authority.instanceId);
  assert.equal(after.room.match.finishReason,'INTERRUPTED');assert.equal(after.room.match.round.question,null);safeLive(after);
  assert.equal(after.room.match.standings.find(p=>p.score>0).score,150);
  assert.ok(JSON.stringify(f.store().db.prepare('SELECT * FROM users').all())===identities);
  assert.equal(f.store().db.prepare('SELECT points FROM mp_answers WHERE round_id=?').get(r.id).points,null);
  f.restart();assert.deepEqual(f.snapshot(1,id).room.match,after.room.match);
 }finally{f.close();rmSync(dir,{recursive:true,force:true});}
});
test('Classic disabled fallback preserves all durable data and existing lobby roles',()=>{
 const f=fixture({enabled:false});try{const id=f.lobby();assert.throws(()=>f.start(id),/CLASSIC_UNAVAILABLE/);
  assert.equal(f.snapshot(0,id).capabilities.matches,false);assert.equal(f.snapshot(0,id).room.members.length,2);
  assert.equal(f.store().db.prepare('PRAGMA foreign_key_check').all().length,0);
 }finally{f.close();}
});
test('fallback restart interrupts active gameplay, preserves accepted records and serves safe final results',()=>{
 const dir=mkdtempSync(join(tmpdir(),'quizpk-classic-fallback-')),path=join(dir,'synthetic.sqlite');const f=fixture({path});let copy;
 try{const id=f.lobby();f.start(id);f.advance(CLASSIC.prepareMs);const r=f.snapshot(0,id).room.match.round;
  f.command(0,'match.answer',{roundId:r.id,option:f.key(id)},id);f.close();
  copy=new Store(path,()=>epoch+3000);const fallback=new RoomService(copy,{bank,classicEnabled:false});fallback.start();
  const s=fallback.snapshot('u0',id,true);assert.equal(s.room.match.finishReason,'INTERRUPTED');assert.equal(s.capabilities.matches,true);
  safeLive(s);assert.equal(copy.db.prepare('SELECT count(*) n FROM mp_answers').get().n,1);
  assert.equal(copy.db.prepare('SELECT sum(score) n FROM mp_match_players').get().n,0);
  assert.equal(copy.db.prepare('PRAGMA foreign_key_check').all().length,0);
 }finally{copy?.close();rmSync(dir,{recursive:true,force:true});}
});
test('API timer releases, reveals and prepares rounds without any polling client or host command',async()=>{
 let now=epoch;const store=new Store(':memory:',()=>now);store.addUser('u0',token(0));store.addUser('u1',token(1));
 const server=createApi({store,roomOptions:{bank,monotonic:()=>now},provider:async()=>{throw Error('FORBIDDEN');},model:'synthetic'});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));const rooms=new RoomService(store,{bank,monotonic:()=>now});
 const cmd=(u,type,payload={},id=null,extra={})=>rooms.command(u,validateCommand({protocolVersion:1,requestId:randomUUID(),issuedAtMs:now,type,payload,...extra},type),id);
 try{const r=cmd('u0','room.create',{mode:'CLASSIC',displayName:'Host',settings}).result;
  cmd('u1','room.join',{code:r.inviteCode,displayName:'Player'});for(const u of ['u0','u1'])cmd(u,'room.ready',{ready:true},r.roomId);
  cmd('u0','match.start',{},r.roomId,{expectedRevision:rooms.room(r.roomId).revision});
  const phase=()=>store.db.prepare('SELECT phase FROM mp_matches WHERE room_id=?').get(r.roomId).phase;
  const wait=()=>new Promise(r=>setTimeout(r,350));
  now+=3000;await wait();assert.equal(phase(),'ROUND_LIVE');
  now+=10000;await wait();assert.equal(phase(),'ROUND_REVEALED');
  now+=5000;await wait();assert.equal(phase(),'ROUND_PREPARED');
 }finally{await new Promise(r=>server.close(r));store.close();}
});
test('bank validation rejects invalid keys/options/duplicates; all shared scoring vectors use production authority',()=>{
 for(const change of [{correct:4},{options:['a','a','c','d']},{text:''},{explanation:''}])
  assert.throws(()=>validateBank([{...bank[0],...change},...bank.slice(1)]),/INVALID_CLASSIC_BANK/);
 assert.throws(()=>validateBank([bank[0],...bank]),/INVALID_CLASSIC_BANK/);
 const vectors=JSON.parse(readFileSync(new URL('../../protocol/multiplayer-v1.json',import.meta.url))).scoreVectors;
 for(const v of vectors)assert.equal(score(v.correct,v.elapsedMs,v.durationMs,v.cancelled),v.expected);
});
test('M4.2 to schema 2 preserves identities, jobs, quotas, lobby rows and replay receipts; failed DDL rolls back',()=>{
 const dir=mkdtempSync(join(tmpdir(),'quizpk-m43-migrate-')),path=join(dir,'synthetic.sqlite');let db,store;
 try{
  db=new DatabaseSync(path);db.exec(`CREATE TABLE users(id TEXT PRIMARY KEY,token_hash TEXT NOT NULL UNIQUE,enabled INTEGER NOT NULL DEFAULT 1);
   CREATE TABLE jobs(user_id TEXT,key TEXT,fingerprint TEXT,created INTEGER,response TEXT,status INTEGER,PRIMARY KEY(user_id,key));
   CREATE TABLE throttle(key TEXT PRIMARY KEY,window INTEGER,count INTEGER); PRAGMA user_version=7;`);
  migrateV1(db);
  db.exec("INSERT INTO users VALUES ('synthetic','synthetic-digest',1);INSERT INTO jobs VALUES ('synthetic','cached','fp',1,'{\"preserved\":true}',200);INSERT INTO throttle VALUES ('synthetic-peer',1,9)");
  const id=randomUUID();db.prepare('INSERT INTO mp_rooms(id,code,mode,owner_id,settings,created_at,expires_at) VALUES (?,\'ABC234\',\'CLASSIC\',\'synthetic\',?,1,9999999999999)').run(id,JSON.stringify(settings));
  db.prepare('INSERT INTO mp_members(room_id,user_id,participant_id,role,display_name,joined_at,last_seen) VALUES (?,\'synthetic\',?,\'CONTESTANT\',\'Saved player\',1,1)').run(id,randomUUID());
  db.exec("INSERT INTO mp_requests VALUES ('synthetic','saved-command','fp','{\"safe\":true}',1);INSERT INTO mp_limits VALUES ('synthetic-limit',1,4,9999999999999)");
  const tables=['users','jobs','throttle','mp_rooms','mp_members','mp_requests','mp_limits'];
  const before=JSON.stringify(tables.map(t=>db.prepare('SELECT * FROM '+t).all()));
  // A colliding new table must leave the entire v1 ledger and data intact.
  db.exec('CREATE TABLE mp_rounds(collision TEXT)');db.close();db=null;
  assert.throws(()=>new Store(path));db=new DatabaseSync(path);
  assert.equal(db.prepare('SELECT max(version) v FROM mp_schema').get().v,1);
  assert.equal(db.prepare("SELECT count(*) n FROM sqlite_master WHERE name='mp_matches'").get().n,0);
  assert.ok(JSON.stringify(tables.map(t=>db.prepare('SELECT * FROM '+t).all()))===before);
  db.exec('DROP TABLE mp_rounds');db.close();db=null;
  store=new Store(path);assert.ok(JSON.stringify(tables.map(t=>store.db.prepare('SELECT * FROM '+t).all()))===before);
  assert.equal(store.db.prepare('PRAGMA user_version').get().user_version,7);
  assert.equal(store.db.prepare('SELECT max(version) v FROM mp_schema').get().v,2);
  assert.equal(store.db.prepare('PRAGMA foreign_key_check').all().length,0);
  assert.equal(store.db.prepare('PRAGMA integrity_check').get().integrity_check,'ok');
  // The retained older migration refuses v2: source-only downgrade is unsafe.
  assert.throws(()=>migrateV1(store.db),/UNSUPPORTED_ROOM_SCHEMA/);
  assert.ok(JSON.stringify(tables.map(t=>store.db.prepare('SELECT * FROM '+t).all()))===before);
 }finally{db?.close();store?.close();rmSync(dir,{recursive:true,force:true});}
});
test('finished results survive expiry for 30 days; users can create again and earlier rounds stay private',()=>{
 const f=fixture();try{const id=f.lobby();f.start(id);f.advance(7200000);
  assert.equal(f.snapshot(0,id).room.match.finishReason,'ROOM_EXPIRED');safeLive(f.snapshot(0,id));
  f.advance(86400001);assert.equal(f.snapshot(0,id).room.state,'MATCH_FINISHED');
  assert.equal(f.command(0,'room.create',{mode:'CLASSIC',displayName:'Again',settings}).result.roomId!==id,true);
  f.advance(CLASSIC.retentionMs);assert.throws(()=>f.snapshot(0,id),/ROOM_NOT_FOUND/);
 }finally{f.close();}
});
test('eight-player match permits bounded one-second snapshots and reserves disconnected seats',()=>{
 const f=fixture();try{const id=f.lobby();const code=f.snapshot(0,id).room.inviteCode;
  for(let i=2;i<8;i++){f.command(i,'room.join',{code,displayName:'Player '+i});f.command(i,'room.ready',{ready:true},id);}
  f.start(id);f.advance(CLASSIC.prepareMs);
  for(let second=0;second<50;second++)for(let user=0;user<8;user++)assert.equal(f.snapshot(user,id).room.members.length,8);
  assert.equal(f.snapshot(7,id).room.match.standings.length,8);
  assert.throws(()=>f.command(8,'room.join',{code,displayName:'Extra'}),/ROOM_NOT_FOUND/);
 }finally{f.close();}
});

test('real HTTP: authentication, TLS, cross-room isolation, sanitized errors, duplicate taps and answer secrecy',async()=>{
 let now=epoch;const store=new Store(':memory:',()=>now);for(let i=0;i<4;i++)store.addUser('u'+i,token(i));
 let calls=0;const server=createApi({store,production:true,roomOptions:{bank,monotonic:()=>now},provider:async()=>{calls++;throw Error('must not call');},model:'mock'});
 await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const req=async(user,path,body,headers={})=>{
  const r=await fetch('http://127.0.0.1:'+server.address().port+path,{method:body?'POST':'GET',headers:{'Content-Type':'application/json',
   'x-forwarded-proto':'https','x-quizpk-features':'classic-v1',...(user===null?{}:{Authorization:'Bearer '+token(user)}),...headers},...(body?{body:JSON.stringify(body)}:{})});
  return {status:r.status,body:await r.json()};
 };
 const envelope=(type,payload={},extra={})=>({protocolVersion:1,requestId:randomUUID(),issuedAtMs:now,type,payload,...extra});
 try{
  const create=await req(0,'/api/rooms',envelope('room.create',{mode:'CLASSIC',displayName:'Host',settings}));const id=create.body.result.roomId;
  await req(1,'/api/rooms/join',envelope('room.join',{code:create.body.result.inviteCode,displayName:'Player'}));
  for(let u=0;u<2;u++)await req(u,'/api/rooms/'+id+'/ready',envelope('room.ready',{ready:true}));
  let s=(await req(0,'/api/rooms/'+id)).body;
  const start=envelope('match.start',{}, {expectedRevision:s.room.revision});
  assert.equal((await req(null,'/api/rooms/'+id+'/match/start',start)).status,401);
  assert.equal((await req(0,'/api/rooms/'+id+'/match/start',start,{'x-forwarded-proto':'http'})).status,400);
  assert.equal((await req(1,'/api/rooms/'+id+'/match/start',start)).status,403);
  const starts=await Promise.all([req(0,'/api/rooms/'+id+'/match/start',start),req(0,'/api/rooms/'+id+'/match/start',start)]);
  assert.ok(starts.every(r=>r.status===200));assert.deepEqual(starts[0].body,starts[1].body);
  now+=3000;s=(await req(1,'/api/rooms/'+id)).body;safeLive(s);
  const answer=envelope('match.answer',{roundId:s.room.match.round.id,option:0});
  const replies=await Promise.all([req(1,'/api/rooms/'+id+'/match/answer',answer),req(1,'/api/rooms/'+id+'/match/answer',answer)]);
  assert.ok(replies.every(r=>r.status===200));assert.deepEqual(replies[0].body,replies[1].body);
  safeLive((await req(0,'/api/rooms/current')).body);
  for(const suffix of ['', '/match/answer','/events','/questions','/answer-keys']){
   const r=await req(2,'/api/rooms/'+id+suffix,suffix==='/match/answer'?answer:undefined);
   assert.equal(r.status,404);assert.ok(!JSON.stringify(r.body).includes('CANARY'));
  }
  assert.equal((await req(0,'/api/rooms/'+id,undefined,{'x-quizpk-features':''})).body.error.code,'UPDATE_REQUIRED');
  store.db.prepare('UPDATE users SET enabled=0 WHERE id=?').run('u1');
  assert.equal((await req(1,'/api/rooms/'+id+'/match/answer',answer)).status,401);
  assert.equal(calls,0);assert.equal(store.db.prepare('SELECT count(*) n FROM mp_answers').get().n,1);
 }finally{await new Promise(r=>server.close(r));store.close();}
});
