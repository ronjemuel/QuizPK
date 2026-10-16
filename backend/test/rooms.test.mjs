import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomUUID,createHash} from 'node:crypto';
import {mkdtempSync,rmSync,readFileSync,cpSync,mkdirSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {execFileSync} from 'node:child_process';
import {DatabaseSync} from 'node:sqlite';
import {Store} from '../src/store.mjs';
import {createApi} from '../src/server.mjs';
import {RoomService} from '../src/multiplayer/rooms.mjs';
import {validateCommand,LIMITS} from '../src/multiplayer/protocol.mjs';
const initial=1791792000000;
const config={category:'Science',difficulty:'Normal',questionCount:5,timerSeconds:15};
const mockToken=i=>'test_identity_'+String(i).padStart(4,'0')+'_'.repeat(30);
function cmd(type,payload={},time=initial,extra={}) {
  return {protocolVersion:1,requestId:randomUUID(),issuedAtMs:time,type,payload,...extra};
}
async function fixture(run,{path=':memory:',production=false,users=30}={}) {
  let now=initial,store=new Store(path,()=>now),server,calls=0;
  for(let i=0;i<users;i++)store.addUser('u'+i,mockToken(i));
  const start=async()=>{
    server=createApi({store,production,model:'mock-model',provider:async()=>{
      calls++;return {questions:Array.from({length:5},(_,i)=>({text:'Mock question number '+i,options:['One','Two','Three','Four'],correctIndex:0,explanation:'This is a deterministic mock answer explanation.'}))};
    }});await new Promise(r=>server.listen(0,'127.0.0.1',r));
  };
  await start();
  const request=async(user,path,body,method=body===undefined?'GET':'POST',headers={})=>{
    const r=await fetch('http://127.0.0.1:'+server.address().port+path,{
      method,headers:{'Content-Type':'application/json',...(user===null?{}:{Authorization:'Bearer '+mockToken(user)}),...headers},
      ...(body===undefined?{}:{body:typeof body==='string'?body:JSON.stringify(body)})});
    return {status:r.status,body:await r.json(),headers:r.headers};
  };
  const command=async(user,type,payload={},room=null,extra={})=>request(user,
    type==='room.create'?'/api/rooms':type==='room.join'?'/api/rooms/join':'/api/rooms/'+room+'/'+type.split('.')[1],
    cmd(type,payload,now,extra));
  const create=async(user=0,mode='CLASSIC')=>{
    const r=await command(user,'room.create',{mode,displayName:'Fixture '+user,settings:config});
    assert.equal(r.status,200);return r.body.result;
  };
  const enter=(user,room)=>command(user,'room.join',{code:room.inviteCode,displayName:'Fixture '+user});
  const snapshot=(user,room)=>request(user,'/api/rooms/'+room.roomId);
  const restart=async()=>{
    await new Promise(r=>server.close(r));store.close();store=new Store(path,()=>now);await start();
  };
  try {await run({request,command,create,enter,snapshot,restart,calls:()=>calls,store:()=>store,
    advance:ms=>now+=ms,now:()=>now});}
  finally {await new Promise(r=>server.close(r));store.close();}
}
test('room API authenticates; health stays unchanged; code is not authentication',()=>fixture(async f=>{
  assert.deepEqual((await f.request(null,'/health')).body,{status:'ok'});
  assert.equal((await f.request(null,'/api/rooms',cmd('room.create',{}))).status,401);
  const r=await f.create(),s=await f.snapshot(0,r);
  assert.equal(r.inviteCode.length,6);assert.equal(s.body.room.members.length,1);
  assert.equal(s.body.room.members[0].role,'CONTESTANT');assert.equal(s.body.self.isOwner,true);
  assert.equal(s.headers.get('cache-control'),'no-store');
  assert.equal((await f.snapshot(1,r)).status,404);
  assert.equal(f.calls(),0);
}));
test('moderator is separate from eight contestants; ninth contestant cannot join',()=>fixture(async f=>{
  const r=await f.create(0,'MODERATOR');
  for(let i=1;i<=8;i++)assert.equal((await f.enter(i,r)).status,200);
  assert.equal((await f.enter(9,r)).body.error.code,'ROOM_FULL');
  const s=(await f.snapshot(1,r)).body;
  assert.equal(s.room.members.length,9);assert.equal(s.room.members.filter(m=>m.role==='MODERATOR').length,1);
  assert.equal(s.room.members.filter(m=>m.role==='CONTESTANT').length,8);
  assert.equal((await f.command(0,'room.ready',{ready:true},r.roomId)).status,403);
}));
test('concurrent real HTTP join race never exceeds eight Classic seats',()=>fixture(async f=>{
  const r=await f.create();
  for(let i=1;i<=5;i++)assert.equal((await f.enter(i,r)).status,200);
  const results=await Promise.all([6,7,8].map(i=>f.enter(i,r)));
  assert.equal(results.filter(r=>r.status===200).length,2);assert.equal(results.filter(r=>r.status===409).length,1);
  assert.equal((await f.snapshot(0,r)).body.room.members.length,8);
}));
test('create retry is atomic/idempotent; same request with different payload conflicts',()=>fixture(async f=>{
  const c=cmd('room.create',{mode:'CLASSIC',displayName:'Fixture',settings:config});
  const [a,b]=await Promise.all([f.request(0,'/api/rooms',c),f.request(0,'/api/rooms',c)]);
  assert.equal(a.status,200);assert.deepEqual(a.body,b.body);
  assert.equal(f.store().db.prepare('SELECT count(*) AS n FROM mp_rooms').get().n,1);
  assert.equal((await f.request(0,'/api/rooms',{...c,payload:{...c.payload,mode:'MODERATOR'}})).status,409);
}));
test('duplicate joins preserve participant identity; leave retry frees only one seat',()=>fixture(async f=>{
  const r=await f.create(),a=await f.enter(1,r),b=await f.enter(1,r);
  assert.equal(a.body.result.participantId,b.body.result.participantId);
  assert.equal((await f.snapshot(0,r)).body.room.members.length,2);
  const c=cmd('room.leave'),path='/api/rooms/'+r.roomId+'/leave';
  const first=await f.request(1,path,c),second=await f.request(1,path,c);
  assert.equal(first.status,200);assert.deepEqual(first.body,second.body);
  assert.equal((await f.snapshot(1,r)).status,404);
  const again=await f.enter(1,r);assert.equal(again.body.result.participantId,a.body.result.participantId);
}));
test('one identity cannot occupy several rooms or claim creator role by display name',()=>fixture(async f=>{
  const a=await f.create(),b=await f.create(1);
  assert.equal((await f.enter(0,b)).body.error.code,'ALREADY_IN_ROOM');
  assert.equal((await f.command(0,'room.create',{mode:'MODERATOR',displayName:'x',settings:config})).status,409);
  const joined=await f.command(2,'room.join',{code:a.inviteCode,displayName:'Fixture 0'});
  assert.equal(joined.status,200);assert.equal((await f.snapshot(2,a)).body.self.isOwner,false);
}));
test('all cross-room reads/writes and role/owner forgery are rejected',()=>fixture(async f=>{
  const a=await f.create(0,'MODERATOR'),b=await f.create(1);await f.enter(2,a);
  assert.equal((await f.snapshot(2,b)).status,404);
  assert.equal((await f.command(2,'room.settings',config,a.roomId,{expectedRevision:2})).status,403);
  assert.equal((await f.command(2,'room.leave',{},b.roomId)).status,404);
  assert.equal((await f.command(3,'room.join',{code:a.inviteCode,displayName:'x',role:'MODERATOR'})).status,400);
  assert.equal((await f.command(3,'room.create',{mode:'MODERATOR',displayName:'x',settings:config,ownerId:'u0'})).status,400);
  for(const action of ['start','answer','reveal','queue'])
    assert.equal((await f.request(2,'/api/rooms/'+a.roomId+'/'+action,{})).status,404);
}));
test('only owner can update lobby settings with a fresh revision; settings reset readiness',()=>fixture(async f=>{
  const r=await f.create();await f.enter(1,r);await f.command(1,'room.ready',{ready:true},r.roomId);
  const before=(await f.snapshot(0,r)).body.room.revision;
  assert.equal((await f.command(1,'room.settings',config,r.roomId,{expectedRevision:before})).status,403);
  assert.equal((await f.command(0,'room.settings',{...config,timerSeconds:30},r.roomId,{expectedRevision:before})).status,200);
  const s=(await f.snapshot(1,r)).body;
  assert.equal(s.room.settings.timerSeconds,30);assert.ok(s.room.members.every(m=>!m.ready));
  assert.ok(s.room.revision>before);
  assert.equal((await f.command(0,'room.settings',config,r.roomId,{expectedRevision:before})).body.error.code,'REVISION_CONFLICT');
}));
test('presence expires durably; host cannot be impersonated; explicit authenticated heartbeat recovers',()=>fixture(async f=>{
  const r=await f.create(0,'MODERATOR');await f.enter(1,r);const v=(await f.snapshot(0,r)).body.room.revision;
  f.advance(LIMITS.presenceMs);
  const s=(await f.snapshot(1,r)).body;assert.equal(s.room.hostOnline,false);assert.ok(s.room.revision>v);
  assert.equal((await f.enter(2,r)).body.error.code,'HOST_UNAVAILABLE');
  assert.equal((await f.command(1,'room.settings',config,r.roomId,{expectedRevision:s.room.revision})).status,403);
  assert.equal((await f.command(0,'room.heartbeat',{},r.roomId)).status,200);
  assert.equal((await f.enter(2,r)).status,200);
  assert.equal((await f.snapshot(0,r)).body.self.role,'MODERATOR');
}));
test('disconnected players keep seats; reconnect is not an extra seat',()=>fixture(async f=>{
  const r=await f.create();for(let i=1;i<=7;i++)await f.enter(i,r);
  const before=(await f.snapshot(1,r)).body.self.participantId;f.advance(46000);
  await f.command(0,'room.heartbeat',{},r.roomId);
  assert.equal((await f.enter(8,r)).body.error.code,'ROOM_FULL');
  assert.equal((await f.enter(1,r)).status,200);assert.equal((await f.snapshot(1,r)).body.self.participantId,before);
}));
test('expired and unknown invite codes have identical errors and reveal no room status',()=>fixture(async f=>{
  const r=await f.create();f.advance(LIMITS.roomTtlMs);
  const expired=await f.enter(1,r),unknown=await f.command(1,'room.join',{code:'ZZZZZZ',displayName:'x'});
  assert.equal(expired.status,404);assert.deepEqual(expired.body,unknown.body);
  const s=(await f.snapshot(0,r)).body;
  assert.equal(s.room.state,'EXPIRED');assert.deepEqual(s.room.members,[]);assert.ok(!Object.hasOwn(s.room,'inviteCode'));
}));
test('owner leave closes room without authority transfer; terminal snapshots omit roster/code',()=>fixture(async f=>{
  const r=await f.create(0,'MODERATOR');await f.enter(1,r);
  assert.equal((await f.command(0,'room.leave',{},r.roomId)).status,200);
  const s=(await f.snapshot(1,r)).body;
  assert.equal(s.room.state,'CLOSED');assert.deepEqual(s.room.members,[]);
  assert.equal(s.self.isOwner,false);assert.equal(s.room.hostOnline,false);
  assert.equal((await f.command(1,'room.heartbeat',{},r.roomId)).status,404);
  assert.equal((await f.enter(2,r)).status,404);
}));
test('revoked identity cannot read, heartbeat or replay a previously successful creation',()=>fixture(async f=>{
  const c=cmd('room.create',{mode:'CLASSIC',displayName:'x',settings:config});
  const a=await f.request(0,'/api/rooms',c);const r=a.body.result;
  f.store().db.prepare('UPDATE users SET enabled=0 WHERE id=?').run('u0');
  assert.equal((await f.snapshot(0,r)).status,401);
  assert.equal((await f.request(0,'/api/rooms',c)).status,401);
  assert.equal((await f.command(0,'room.heartbeat',{},r.roomId)).status,401);
}));
test('allowlisted snapshots never expose cloud identities, token hashes, private records or answers',()=>fixture(async f=>{
  const r=await f.create(0,'MODERATOR');await f.enter(1,r);
  f.store().db.exec('CREATE TABLE private_test_keys(room_id TEXT,correctIndex INTEGER,explanation TEXT)');
  f.store().db.prepare('INSERT INTO private_test_keys VALUES (?,2,?)').run(r.roomId,'private-answer-canary');
  const response=(await f.snapshot(1,r)).body,serialized=JSON.stringify(response);
  for(const marker of ['correctIndex','explanation','private-answer-canary','token_hash','owner_id','user_id',mockToken(0)])
    assert.ok(!serialized.includes(marker));
  assert.equal(response.capabilities.matches,false);assert.equal(response.capabilities.localHost,false);
}));
test('invalid schemas/versions/JSON/types and oversized bodies fail with bounded sanitized errors',()=>fixture(async f=>{
  assert.equal((await f.request(0,'/api/rooms','not-json')).status,400);
  assert.equal((await f.request(0,'/api/rooms',cmd('room.create',{}),'POST',{'Content-Type':'text/plain'})).status,415);
  assert.equal((await f.request(0,'/api/rooms',{...cmd('room.create',{}),protocolVersion:2})).body.error.code,'PROTOCOL_UNSUPPORTED');
  const large=await f.request(0,'/api/rooms',JSON.stringify({privatePayload:'x'.repeat(5000)}));
  assert.equal(large.status,413);assert.ok(!JSON.stringify(large.body).includes('privatePayload'));
}));
test('room commands retain strict production HTTPS enforcement',()=>fixture(async f=>{
  const c=cmd('room.create',{mode:'CLASSIC',displayName:'x',settings:config});
  assert.equal((await f.request(0,'/api/rooms',c)).status,400);
  assert.equal((await f.request(0,'/api/rooms',c,'POST',{'x-forwarded-proto':'https'})).status,200);
}, {production:true}));
test('code-guess limit counts malformed and unsuccessful attempts; window expires',()=>fixture(async f=>{
  for(let i=0;i<5;i++)assert.equal((await f.command(0,'room.join',{code:'invalid',displayName:'x'})).status,404);
  assert.equal((await f.command(0,'room.join',{code:'ZZZZZZ',displayName:'x'})).status,429);
  f.advance(61000);assert.equal((await f.command(0,'room.join',{code:'ZZZZZZ',displayName:'x'})).status,404);
}));
test('hourly code guessing remains capped across minute windows',()=>fixture(async f=>{
  for(let minute=0;minute<4;minute++){
    for(let i=0;i<5;i++)assert.equal((await f.command(0,'room.join',{code:'ZZZZZZ',displayName:'x'})).status,404);
    f.advance(61000);
  }
  assert.equal((await f.command(0,'room.join',{code:'ZZZZZZ',displayName:'x'})).status,429);
}));
test('write budget caps persistent receipts while snapshots remain available',()=>fixture(async f=>{
  const r=await f.create();
  for(let i=0;i<19;i++)assert.equal((await f.command(0,'room.heartbeat',{},r.roomId)).status,200);
  assert.equal((await f.command(0,'room.heartbeat',{},r.roomId)).status,429);
  assert.equal((await f.snapshot(0,r)).status,200);
}));
test('room and ingress caps are enforced independently; unauthorized reads do not consume room budget',()=>fixture(async f=>{
  const r=await f.create();await f.enter(1,r);await f.snapshot(0,r);
  const digest=s=>createHash('sha256').update(s).digest('hex');
  f.store().db.prepare('UPDATE mp_limits SET count=299 WHERE key=?').run(digest('room:'+r.roomId));
  assert.equal((await f.snapshot(2,r)).status,404);
  assert.equal((await f.snapshot(0,r)).status,200);
  assert.equal((await f.snapshot(1,r)).status,429);
  const rooms=new RoomService(f.store());
  rooms.rate('ip:fixture',LIMITS.ingressMinute,60000);
  f.store().db.prepare('UPDATE mp_limits SET count=? WHERE key=?').run(LIMITS.ingressMinute,digest('ip:fixture'));
  assert.throws(()=>rooms.ingress('fixture'),/ROOM_RATE_LIMITED/);
  assert.deepEqual((await f.request(null,'/health')).body,{status:'ok'});
}));
test('command clock window rejects stale/future new requests; existing receipts remain idempotent',()=>fixture(async f=>{
  const payload={mode:'CLASSIC',displayName:'x',settings:config};
  for(const issued of [f.now()-LIMITS.commandAgeMs-1,f.now()+LIMITS.futureSkewMs+1]){
    const r=await f.request(0,'/api/rooms',cmd('room.create',payload,issued));
    assert.equal(r.body.error.code,'COMMAND_EXPIRED');
  }
  const c=cmd('room.create',payload,f.now()),first=await f.request(0,'/api/rooms',c);
  f.advance(LIMITS.commandAgeMs+1);
  assert.deepEqual((await f.request(0,'/api/rooms',c)).body,first.body);
}));
test('per-user room request throttle is separate from Gemini quotas/throttle',()=>fixture(async f=>{
  for(let i=0;i<120;i++)assert.equal((await f.request(0,'/api/rooms/current')).status,200);
  assert.equal((await f.request(0,'/api/rooms/current')).status,429);
  const request={topic:'Java Programming',difficulty:'Hard',questionCount:5,timerSeconds:20};
  assert.equal((await f.request(0,'/api/quizzes/generate',request,'POST',{'Idempotency-Key':randomUUID()})).status,200);
  assert.equal(f.calls(),1);
}));
test('global active-room cap is bounded',()=>fixture(async f=>{
  for(let i=0;i<LIMITS.maxRooms;i++)await f.create(i);
  assert.equal((await f.command(20,'room.create',{mode:'CLASSIC',displayName:'x',settings:config})).body.error.code,'ROOM_LIMIT');
}));
test('user daily creation cap survives closing rooms and cannot be overridden',()=>fixture(async f=>{
  for(let i=0;i<5;i++){const r=await f.create();await f.command(0,'room.leave',{},r.roomId);}
  assert.equal((await f.command(0,'room.create',{mode:'CLASSIC',displayName:'x',settings:config})).body.error.code,'ROOM_LIMIT');
}));
test('room terminal cleanup and aged command replay cannot create a duplicate',()=>fixture(async f=>{
  const c=cmd('room.create',{mode:'CLASSIC',displayName:'x',settings:config});
  const r=(await f.request(0,'/api/rooms',c)).body.result;
  await f.command(0,'room.leave',{},r.roomId);f.advance(LIMITS.retentionMs+1);
  const retry=await f.request(0,'/api/rooms',c);
  assert.equal(retry.body.error.code,'COMMAND_EXPIRED');
  assert.equal(f.store().db.prepare('SELECT count(*) AS n FROM mp_rooms').get().n,0);
  assert.equal(f.store().db.prepare('SELECT count(*) AS n FROM mp_members').get().n,0);
}));
test('on-disk API restart preserves rooms/receipts/identity, invalidates presence and changes epoch',async()=>{
  const folder=mkdtempSync(join(tmpdir(),'quizpk-rooms-restart-'));
  try{await fixture(async f=>{
    const c=cmd('room.create',{mode:'MODERATOR',displayName:'x',settings:config});
    const a=await f.request(0,'/api/rooms',c),r=a.body.result;await f.enter(1,r);
    const before=(await f.snapshot(1,r)).body;
    await f.restart();
    const after=(await f.snapshot(1,r)).body;
    assert.notEqual(after.authority.instanceId,before.authority.instanceId);
    assert.equal(after.self.participantId,before.self.participantId);assert.equal(after.room.hostOnline,false);
    assert.ok(after.room.revision>before.room.revision);
    assert.deepEqual((await f.request(0,'/api/rooms',c)).body,a.body);
    await f.command(0,'room.heartbeat',{},r.roomId);assert.equal((await f.snapshot(0,r)).body.self.role,'MODERATOR');
    assert.equal((await f.enter(2,r)).status,200);
  },{path:join(folder,'rooms.sqlite')});}finally{rmSync(folder,{recursive:true,force:true});}
});
test('code collision retry is bounded and rolls back partial creation',()=>{
  const store=new Store(':memory:',()=>initial);
  try{
    store.addUser('a',mockToken(0));store.addUser('b',mockToken(1));
    const rooms=new RoomService(store,{codeFactory:()=> 'AAAAAA'});
    const create=user=>rooms.command(user,validateCommand(cmd('room.create',{mode:'CLASSIC',displayName:'x',settings:config}),'room.create'));
    create('a');assert.throws(()=>create('b'),/CODE_UNAVAILABLE/);
    assert.equal(store.db.prepare('SELECT count(*) AS n FROM mp_rooms').get().n,1);
    assert.equal(store.db.prepare('SELECT count(*) AS n FROM mp_members').get().n,1);
    let attempts=0;const recover=new RoomService(store,{codeFactory:()=>++attempts===1?'AAAAAA':'BBBBBB'});
    recover.command('b',validateCommand(cmd('room.create',{mode:'CLASSIC',displayName:'x',settings:config}),'room.create'));
    assert.equal(attempts,2);
  }finally{store.close();}
});
test('additive M3 DB migration retains auth hashes, jobs, throttle and user_version without printing them',()=>{
  const folder=mkdtempSync(join(tmpdir(),'quizpk-m3-migration-')),path=join(folder,'m3.sqlite');
  let old=new DatabaseSync(path),store;
  try {
    old.exec('CREATE TABLE users(id TEXT PRIMARY KEY,token_hash TEXT NOT NULL UNIQUE,enabled INTEGER NOT NULL DEFAULT 1);CREATE TABLE jobs(user_id TEXT,key TEXT,fingerprint TEXT,created INTEGER,response TEXT,status INTEGER,PRIMARY KEY(user_id,key));CREATE TABLE throttle(key TEXT PRIMARY KEY,window INTEGER,count INTEGER)');
    // Populate through the preserved M3 auth/quota methods, not invented hash semantics.
    old.close();old=null;
    store=new Store(path,()=>initial);store.addUser('old',mockToken(0));
    store.reserve('old','old-job','fp');store.complete('old','old-job',200,{preserved:true});store.throttle('old-ip');
    // Remove only new empty room tables to form a genuine pre-M4 schema fixture.
    store.db.exec('DROP TABLE mp_answers;DROP TABLE mp_answer_keys;DROP TABLE mp_rounds;DROP TABLE mp_match_players;DROP TABLE mp_matches;DROP TABLE mp_members;DROP TABLE mp_rooms;DROP TABLE mp_requests;DROP TABLE mp_limits;DROP TABLE mp_schema');
    const before=JSON.stringify(['users','jobs','throttle'].map(t=>store.db.prepare('SELECT * FROM '+t).all()));
    store.close();store=new Store(path,()=>initial);
    const after=JSON.stringify(['users','jobs','throttle'].map(t=>store.db.prepare('SELECT * FROM '+t).all()));
    assert.ok(before===after);assert.equal(store.authenticate('Bearer '+mockToken(0)),'old');
    assert.deepEqual(store.reserve('old','old-job','fp'),{status:200,body:{preserved:true}});
    assert.equal(store.db.prepare('PRAGMA user_version').get().user_version,0);
    assert.equal(store.db.prepare('PRAGMA quick_check').get().quick_check,'ok');
    assert.equal(store.db.prepare('SELECT max(version) AS v FROM mp_schema').get().v,2);
    store.close();store=new Store(path,()=>initial);assert.equal(store.db.prepare('SELECT count(*) AS n FROM jobs').get().n,1);
  }finally{old?.close();store?.close();rmSync(folder,{recursive:true,force:true});}
});
test('existing create-user CLI provisions unique tokens against a populated M3-compatible private DB',()=>{
  const folder=mkdtempSync(join(tmpdir(),'quizpk-provision-')),backend=join(folder,'backend'),db=join(folder,'existing.sqlite');
  let store;
  try{
    mkdirSync(backend);cpSync(new URL('../src/',import.meta.url),join(backend,'src'),{recursive:true});
    cpSync(new URL('../package.json',import.meta.url),join(backend,'package.json'));
    store=new Store(db);store.addUser('existing',mockToken(0));store.close();store=null;
    const output=execFileSync(process.execPath,[join(backend,'src/create-user.mjs'),'new-user'],{env:{...process.env,QUIZPK_DB_PATH:db},encoding:'utf8'});
    const token=readFileSync(join(backend,'private/new-user.token'),'utf8').trim();
    assert.ok(!output.includes(token));store=new Store(db);
    assert.equal(store.authenticate('Bearer '+token),'new-user');assert.equal(store.authenticate('Bearer '+mockToken(0)),'existing');
    assert.equal(store.db.prepare('SELECT count(*) AS n FROM users').get().n,2);
    const safe=execFileSync(process.execPath,[join(backend,'src/check-room-db.mjs'),'--token-file',join(backend,'private/new-user.token')],{env:{...process.env,QUIZPK_DB_PATH:db},encoding:'utf8'});
    assert.deepEqual(JSON.parse(safe),{databasePresent:true,identitySchemaPresent:true,roomSchemaPresent:true,tokenMatchesEnabledIdentity:true});
    assert.ok(!safe.includes(token));
  }finally{store?.close();rmSync(folder,{recursive:true,force:true});}
});
