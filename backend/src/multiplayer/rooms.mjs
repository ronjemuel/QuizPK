import { randomInt, randomUUID, createHash } from 'node:crypto';
import { ApiError } from '../contract.mjs';
import { CODE_ALPHABET, LIMITS, UUID } from './protocol.mjs';
import { ClassicService, CLASSIC } from './classic.mjs';
const digest = text=>createHash('sha256').update(text).digest('hex');
const makeCode = ()=>Array.from({length:6},()=>CODE_ALPHABET[randomInt(CODE_ALPHABET.length)]).join('');

export class RoomService {
  constructor(store,{codeFactory=makeCode,...classicOptions}={}) {
    this.db=store.db;this.now=store.now;this.codeFactory=codeFactory;this.instanceId=randomUUID();
    this.classic=new ClassicService(this,classicOptions);
  }
  transaction(action) {
    this.db.exec('BEGIN IMMEDIATE');
    try { const result=action();this.db.exec('COMMIT');return result; }
    catch(e) {this.db.exec('ROLLBACK');throw e;}
  }
  // Called by API startup, not CLI/database opens: stale sessions cannot look online after restart.
  start() {
    this.transaction(()=>{
      this.classic.restart();
      this.maintain();
      this.db.exec("UPDATE mp_rooms SET revision=revision+1 WHERE state='LOBBY' AND id IN (SELECT room_id FROM mp_members WHERE online=1 AND left_at IS NULL)");
      this.db.exec('UPDATE mp_members SET online=0,ready=0 WHERE online=1');
    });
  }
  maintain() {
    this.classic.advance();
    const now=this.now();
    this.db.prepare("UPDATE mp_rooms SET state='EXPIRED',terminal_at=?,revision=revision+1 WHERE state='LOBBY' AND expires_at<=?").run(now,now);
    this.db.prepare("UPDATE mp_rooms SET revision=revision+1 WHERE state='LOBBY' AND id IN (SELECT room_id FROM mp_members WHERE left_at IS NULL AND online=1 AND (last_seen<=? OR user_id IN (SELECT id FROM users WHERE enabled=0)))").run(now-LIMITS.presenceMs);
    this.db.prepare('UPDATE mp_members SET online=0,ready=0 WHERE online=1 AND (last_seen<=? OR user_id IN (SELECT id FROM users WHERE enabled=0))').run(now-LIMITS.presenceMs);
    this.db.prepare("DELETE FROM mp_rooms WHERE state<>'LOBBY' AND terminal_at<=? AND id NOT IN (SELECT room_id FROM mp_matches)").run(now-LIMITS.retentionMs);
    this.db.prepare("DELETE FROM mp_rooms WHERE id IN (SELECT room_id FROM mp_matches WHERE phase='MATCH_FINISHED' AND finished_at<=?)").run(now-CLASSIC.retentionMs);
    this.db.prepare('DELETE FROM mp_requests WHERE created_at<=?').run(now-LIMITS.retentionMs);
    this.db.prepare('DELETE FROM mp_limits WHERE expires_at<=?').run(now);
  }
  tick() {this.transaction(()=>this.classic.advance());}
  // Separate committed counters: failed commands/code guesses cannot roll back their rate usage.
  rate(key, max, windowMs) {
    const now=this.now(), bucket=Math.floor(now/windowMs);
    this.db.prepare('DELETE FROM mp_limits WHERE expires_at<=?').run(now);
    this.db.prepare('INSERT INTO mp_limits VALUES (?,?,1,?) ON CONFLICT(key) DO UPDATE SET count=CASE WHEN bucket=excluded.bucket THEN count+1 ELSE 1 END,bucket=excluded.bucket,expires_at=excluded.expires_at')
      .run(digest(key),bucket,(bucket+1)*windowMs);
    if (this.db.prepare('SELECT count FROM mp_limits WHERE key=?').get(digest(key)).count>max)
      throw new ApiError(429,'ROOM_RATE_LIMITED');
  }
  ingress(peer) {this.rate('ip:'+peer,LIMITS.ingressMinute,60000);}
  userRate(user) {this.rate('user:'+user,LIMITS.userRequestsMinute,60000);}
  writeRate(user) {
    this.rate('write-minute:'+user,LIMITS.userCommandsMinute,60000);
    this.rate('write-hour:'+user,LIMITS.userCommandsHour,3600000);
    this.rate('global-write-hour',LIMITS.globalCommandsHour,3600000);
  }
  guessRate(user) {this.rate('guess-minute:'+user,5,60000);this.rate('guess-hour:'+user,20,3600000);}
  chargeRoom(user,id,leaveRetry=false) {
    const room=this.room(id),member=this.db.prepare('SELECT left_at FROM mp_members WHERE room_id=? AND user_id=?').get(id,user);
    const match=this.classic.get(id);
    if(!member || member.left_at!==null && room.state==='LOBBY' && !leaveRetry && !match)throw new ApiError(404,'ROOM_NOT_FOUND');
    this.rate('room:'+id,match?1200:LIMITS.roomRequestsMinute,60000);
  }
  member(room,user) {
    const row=this.db.prepare('SELECT * FROM mp_members WHERE room_id=? AND user_id=? AND left_at IS NULL').get(room,user);
    if (!row) throw new ApiError(404,'ROOM_NOT_FOUND');
    return row;
  }
  room(id) {
    const room=this.db.prepare('SELECT * FROM mp_rooms WHERE id=?').get(id);
    if (!room) throw new ApiError(404,'ROOM_NOT_FOUND');
    return room;
  }
  activeRoom(user) {
    return this.db.prepare("SELECT r.id FROM mp_rooms r JOIN mp_members m ON m.room_id=r.id WHERE m.user_id=? AND m.left_at IS NULL AND r.state='LOBBY' AND r.id NOT IN (SELECT room_id FROM mp_matches WHERE phase='MATCH_FINISHED')").get(user)?.id;
  }
  bump(id) {this.db.prepare('UPDATE mp_rooms SET revision=revision+1 WHERE id=?').run(id);}
  online(room,user) {
    this.db.prepare('UPDATE mp_members SET online=1,last_seen=? WHERE room_id=? AND user_id=?').run(this.now(),room,user);
    this.bump(room);
  }
  ownerOnline(room) {
    return Boolean(this.db.prepare('SELECT online FROM mp_members WHERE room_id=? AND user_id=? AND left_at IS NULL').get(room.id,room.owner_id)?.online);
  }
  insertMember(room,user,role,name) {
    const existing=this.db.prepare('SELECT participant_id FROM mp_members WHERE room_id=? AND user_id=?').get(room,user);
    const id=existing?.participant_id || randomUUID(),now=this.now();
    this.db.prepare('INSERT INTO mp_members(room_id,user_id,participant_id,role,display_name,joined_at,last_seen) VALUES (?,?,?,?,?,?,?) ON CONFLICT(room_id,user_id) DO UPDATE SET left_at=NULL,online=1,ready=0,last_seen=excluded.last_seen,display_name=excluded.display_name')
      .run(room,user,id,role,name,now,now);
    return id;
  }
  command(user,command,roomId=null) {
    // Validate/cap guesses at the HTTP boundary before parsing even malformed codes.
    this.transaction(()=>this.maintain());
    if(roomId)this.chargeRoom(user,roomId,command.type==='room.leave');
    return this.transaction(()=>{
      const fingerprint=digest(JSON.stringify({roomId,command}));
      const old=this.db.prepare('SELECT fingerprint,response FROM mp_requests WHERE user_id=? AND request_id=?').get(user,command.requestId);
      if (old) {
        if(old.fingerprint!==fingerprint)throw new ApiError(409,'IDEMPOTENCY_CONFLICT');
        return JSON.parse(old.response);
      }
      const now=roomId && this.classic.get(roomId)?this.classic.time():this.now();
      if(command.issuedAtMs<now-LIMITS.commandAgeMs || command.issuedAtMs>now+LIMITS.futureSkewMs)
        throw new ApiError(409,'COMMAND_EXPIRED');
      let result;
      if(command.type==='room.create')result=this.create(user,command.payload);
      else if(command.type==='room.join')result=this.join(user,command.payload);
      else {
        const room=this.room(roomId), match=this.classic.get(roomId);
        const member=match?this.classic.player(roomId,user):this.member(roomId,user);
        if(command.type==='match.start')this.classic.launch(user,room,command.expectedRevision);
        else if(command.type==='match.answer')this.classic.answer(user,roomId,command.payload);
        else if(match) {
          if(command.type==='room.leave')this.classic.dismiss(user,roomId);
          else if(command.type==='room.heartbeat' && match.phase!=='MATCH_FINISHED')this.online(roomId,user);
          else throw new ApiError(409,'MATCH_IN_PROGRESS');
        } else {
        if(room.state!=='LOBBY')throw new ApiError(410,'ROOM_UNAVAILABLE');
        if(command.type==='room.leave') {
          if(room.owner_id===user) {
            this.db.prepare("UPDATE mp_rooms SET state='CLOSED',terminal_at=?,revision=revision+1 WHERE id=?").run(now,roomId);
            this.db.prepare('UPDATE mp_members SET left_at=?,online=0,ready=0 WHERE room_id=? AND left_at IS NULL').run(now,roomId);
          } else {
            this.db.prepare('UPDATE mp_members SET left_at=?,online=0,ready=0 WHERE room_id=? AND user_id=?').run(now,roomId,user);this.bump(roomId);
          }
        } else if(command.type==='room.heartbeat')this.online(roomId,user);
        else if(command.type==='room.ready') {
          if(member.role!=='CONTESTANT')throw new ApiError(403,'FORBIDDEN');
          if(!member.online)throw new ApiError(409,'HEARTBEAT_REQUIRED');
          this.db.prepare('UPDATE mp_members SET ready=? WHERE room_id=? AND user_id=?').run(command.payload.ready?1:0,roomId,user);this.bump(roomId);
        } else if(command.type==='room.settings') {
          if(room.owner_id!==user)throw new ApiError(403,'FORBIDDEN');
          if(!this.ownerOnline(room))throw new ApiError(409,'HOST_UNAVAILABLE');
          if(room.revision!==command.expectedRevision)throw new ApiError(409,'REVISION_CONFLICT');
          this.db.prepare('UPDATE mp_rooms SET settings=?,revision=revision+1 WHERE id=?').run(JSON.stringify(command.payload),roomId);
          this.db.prepare('UPDATE mp_members SET ready=0 WHERE room_id=?').run(roomId);
        } else throw new ApiError(400,'INVALID_COMMAND');
        }
        result={roomId,participantId:member.participant_id};
      }
      const response={protocolVersion:1,requestId:command.requestId,result};
      this.db.prepare('INSERT INTO mp_requests VALUES (?,?,?,?,?)').run(user,command.requestId,fingerprint,JSON.stringify(response),now);
      return response;
    });
  }
  create(user,payload) {
    if(this.activeRoom(user))throw new ApiError(409,'ALREADY_IN_ROOM');
    const now=this.now(),day=Math.floor(now/86400000)*86400000;
    if(this.db.prepare("SELECT count(*) AS n FROM mp_rooms WHERE state='LOBBY' AND id NOT IN (SELECT room_id FROM mp_matches WHERE phase='MATCH_FINISHED')").get().n>=LIMITS.maxRooms)
      throw new ApiError(429,'ROOM_LIMIT');
    if(this.db.prepare('SELECT count(*) AS n FROM mp_rooms WHERE owner_id=? AND created_at>=?').get(user,day).n>=LIMITS.userCreatesDaily ||
       this.db.prepare('SELECT count(*) AS n FROM mp_rooms WHERE created_at>=?').get(day).n>=LIMITS.globalCreatesDaily)
      throw new ApiError(429,'ROOM_LIMIT');
    let code;
    for(let i=0;i<16;i++) {
      const candidate=this.codeFactory();
      if(typeof candidate!=='string' || candidate.length!==6 || ![...candidate].every(c=>CODE_ALPHABET.includes(c)))throw Error('CODE_FACTORY_FAILED');
      if(!this.db.prepare('SELECT 1 FROM mp_rooms WHERE code=?').get(candidate)){code=candidate;break;}
    }
    if(!code)throw new ApiError(503,'CODE_UNAVAILABLE');
    const room=randomUUID();
    this.db.prepare('INSERT INTO mp_rooms(id,code,mode,owner_id,settings,created_at,expires_at) VALUES (?,?,?,?,?,?,?)')
      .run(room,code,payload.mode,user,JSON.stringify(payload.settings),now,now+LIMITS.roomTtlMs);
    const participant=this.insertMember(room,user,payload.mode==='MODERATOR'?'MODERATOR':'CONTESTANT',payload.displayName);
    this.db.prepare("UPDATE mp_match_players SET dismissed_at=? WHERE user_id=? AND room_id IN (SELECT room_id FROM mp_matches WHERE phase='MATCH_FINISHED')").run(now,user);
    return {roomId:room,participantId:participant,inviteCode:code};
  }
  join(user,payload) {
    const room=this.db.prepare("SELECT * FROM mp_rooms WHERE code=? AND state='LOBBY'").get(payload.code);
    if(!room)throw new ApiError(404,'ROOM_NOT_FOUND');
    const current=this.activeRoom(user);
    if(current && current!==room.id)throw new ApiError(409,'ALREADY_IN_ROOM');
    const member=this.db.prepare('SELECT * FROM mp_members WHERE room_id=? AND user_id=? AND left_at IS NULL').get(room.id,user);
    if(member){this.online(room.id,user);return {roomId:room.id,participantId:member.participant_id};}
    if(this.classic.get(room.id))throw new ApiError(404,'ROOM_NOT_FOUND');
    if(!this.ownerOnline(room))throw new ApiError(409,'HOST_UNAVAILABLE');
    if(this.db.prepare("SELECT count(*) AS n FROM mp_members WHERE room_id=? AND role='CONTESTANT' AND left_at IS NULL").get(room.id).n>=8)
      throw new ApiError(409,'ROOM_FULL');
    const participant=this.insertMember(room.id,user,'CONTESTANT',payload.displayName);this.bump(room.id);
    return {roomId:room.id,participantId:participant};
  }
  snapshot(user,id=null,classic=false) {
    this.transaction(()=>this.maintain());
    const currentId=id || this.activeRoom(user) || this.classic.current(user);
    if(currentId)this.chargeRoom(user,currentId);
    return this.transaction(()=>{
      const roomId=currentId;
      if(!roomId)return {protocolVersion:1,serverTimeMs:this.now(),authority:{network:'ONLINE',instanceId:this.instanceId},room:null};
      if(!UUID.test(roomId))throw new ApiError(404,'ROOM_NOT_FOUND');
      const room=this.room(roomId);
      const match=this.classic.get(roomId);
      if(match && !classic)throw new ApiError(409,'UPDATE_REQUIRED');
      const self=this.db.prepare('SELECT * FROM mp_members WHERE room_id=? AND user_id=?').get(roomId,user);
      if(!self || self.left_at!==null && room.state==='LOBBY' && !match)throw new ApiError(404,'ROOM_NOT_FOUND');
      if(match)this.classic.player(roomId,user);
      const base={id:room.id,mode:room.mode,state:room.state,revision:room.revision,expiresAtMs:room.expires_at};
      const members=match?this.db.prepare('SELECT m.participant_id,m.role,p.display_name,m.ready,m.online FROM mp_members m JOIN mp_match_players p ON p.participant_id=m.participant_id WHERE m.room_id=? ORDER BY m.joined_at,m.participant_id').all(roomId):room.state==='LOBBY'?this.db.prepare('SELECT participant_id,role,display_name,ready,online FROM mp_members WHERE room_id=? AND left_at IS NULL ORDER BY joined_at,participant_id').all(roomId):[];
      return {protocolVersion:1,serverTimeMs:match?this.classic.time():this.now(),authority:{network:'ONLINE',instanceId:this.instanceId},
        room:{...base,...(match?{state:match.phase,match:this.classic.projection(user,roomId)}:{}),...(room.state==='LOBBY' || match?{inviteCode:room.code,settings:JSON.parse(room.settings)}:{}),
          maxContestants:8,minContestants:2,hostOnline:room.state==='LOBBY' && this.ownerOnline(room),
          members:members.map(m=>({participantId:m.participant_id,role:m.role,displayName:m.display_name,ready:Boolean(m.ready),online:Boolean(m.online)}))},
        self:{participantId:self.participant_id,role:self.role,isOwner:room.owner_id===user},
        capabilities:{lobby:true,matches:classic && room.mode==='CLASSIC' && (this.classic.enabled || Boolean(match)),localHost:false}};
    });
  }
}
