import { readFileSync } from 'node:fs';
import { randomInt, randomUUID } from 'node:crypto';
import { performance } from 'node:perf_hooks';
import { ApiError } from '../contract.mjs';
import { CATEGORIES } from './protocol.mjs';

export const CLASSIC = Object.freeze({prepareMs:3000, revealMs:5000, pollMs:1000, retentionMs:30*86400000});
export function score(correct, elapsed, duration, cancelled=false) {
  if (cancelled || !correct || elapsed < 0 || elapsed >= duration || duration <= 0) return 0;
  return 100 + Math.floor(50 * (duration-elapsed) / duration);
}
export function validateBank(bank) {
  if (!Array.isArray(bank) || bank.length < 5 || bank.length > 10000) throw Error('INVALID_CLASSIC_BANK');
  const ids=new Set(), prompts=new Set();
  for (const q of bank) {
    const norm=s=>s.normalize('NFKC').trim().toLowerCase();
    if (!q || typeof q.id!=='string' || ids.has(q.id) || typeof q.text!=='string' || q.text.length<5 || q.text.length>600 ||
        prompts.has(norm(q.text)) || !CATEGORIES.slice(1).includes(q.category) ||
        !['Easy','Normal','Hard','Expert'].includes(q.difficulty) || !Array.isArray(q.options) || q.options.length!==4 ||
        q.options.some(o=>typeof o!=='string' || !o.trim() || o.length>240) ||
        new Set(q.options.map(norm)).size!==4 || !Number.isInteger(q.correct) || q.correct<0 || q.correct>3 ||
        typeof q.explanation!=='string' || !q.explanation.trim() || q.explanation.length>1000)
      throw Error('INVALID_CLASSIC_BANK');
    ids.add(q.id);prompts.add(norm(q.text));
  }
  return bank;
}
function shuffle(values) {
  const a=[...values];
  for(let i=a.length-1;i>0;i--){const j=randomInt(i+1);[a[i],a[j]]=[a[j],a[i]];}
  return a;
}

// Called only inside the room authority's BEGIN IMMEDIATE transaction.
export class ClassicService {
  constructor(rooms,{bank,monotonic=()=>performance.now(),classicEnabled=true}={}) {
    this.rooms=rooms;this.db=rooms.db;this.enabled=classicEnabled;
    this.bank=validateBank(bank ?? JSON.parse(readFileSync(new URL('./classic-bank.json',import.meta.url),'utf8')));
    const epoch=rooms.now(), start=monotonic();let last=epoch;
    this.time=()=>last=Math.max(last,Math.floor(epoch+monotonic()-start));
  }
  get(id) {return this.db.prepare('SELECT * FROM mp_matches WHERE room_id=?').get(id);}
  player(id,user) {
    const p=this.db.prepare('SELECT * FROM mp_match_players WHERE room_id=? AND user_id=?').get(id,user);
    if(!p)throw new ApiError(404,'ROOM_NOT_FOUND');return p;
  }
  current(user) {
    return this.db.prepare(`SELECT p.room_id FROM mp_match_players p JOIN mp_matches m ON m.room_id=p.room_id
      WHERE p.user_id=? AND p.dismissed_at IS NULL ORDER BY m.started_at DESC LIMIT 1`).get(user)?.room_id;
  }
  dismiss(user,id) {
    if(this.get(id)?.phase!=='MATCH_FINISHED')throw new ApiError(409,'MATCH_IN_PROGRESS');
    this.player(id,user);
    this.db.prepare('UPDATE mp_match_players SET dismissed_at=? WHERE room_id=? AND user_id=?').run(this.time(),id,user);
    this.db.prepare('UPDATE mp_members SET left_at=?,online=0,ready=0 WHERE room_id=? AND user_id=?').run(this.rooms.now(),id,user);
    this.rooms.bump(id);
  }
  launch(user,room,revision) {
    if(room.owner_id!==user)throw new ApiError(403,'FORBIDDEN');
    if(room.mode!=='CLASSIC' || !this.enabled)throw new ApiError(409,'CLASSIC_UNAVAILABLE');
    if(this.get(room.id))throw new ApiError(409,'MATCH_IN_PROGRESS');
    if(room.state!=='LOBBY')throw new ApiError(410,'ROOM_UNAVAILABLE');
    if(room.revision!==revision)throw new ApiError(409,'REVISION_CONFLICT');
    const players=this.db.prepare('SELECT * FROM mp_members WHERE room_id=? AND left_at IS NULL').all(room.id);
    if(players.length<2 || players.length>8)throw new ApiError(409,'TOO_FEW_CONTESTANTS');
    if(players.some(p=>!p.online || !p.ready))throw new ApiError(409,'PLAYERS_NOT_READY');
    const settings=JSON.parse(room.settings), pool=this.bank.filter(q=>q.difficulty===settings.difficulty &&
      (settings.category==='All Categories' || settings.category===q.category));
    if(pool.length<settings.questionCount)throw new ApiError(409,'INSUFFICIENT_QUESTIONS');
    const now=this.time();
    this.db.prepare('INSERT INTO mp_matches(room_id,phase,round_index,question_count,started_at) VALUES (?,\'ROUND_PREPARED\',1,?,?)')
      .run(room.id,settings.questionCount,now);
    for(const p of players)this.db.prepare('INSERT INTO mp_match_players(room_id,user_id,participant_id,display_name) VALUES (?,?,?,?)')
      .run(room.id,p.user_id,p.participant_id,p.display_name);
    shuffle(pool).slice(0,settings.questionCount).forEach((q,i)=>{
      const id=randomUUID(), order=shuffle([0,1,2,3]);
      this.db.prepare('INSERT INTO mp_rounds(id,room_id,position,state,question_json) VALUES (?,?,?,\'QUEUED\',?)')
        .run(id,room.id,i+1,JSON.stringify({text:q.text,options:order.map(n=>q.options[n])}));
      this.db.prepare('INSERT INTO mp_answer_keys VALUES (?,?,?)').run(id,order.indexOf(q.correct),q.explanation);
    });
    this.prepare(room.id,1,now,settings.timerSeconds*1000);
  }
  prepare(id,position,now,duration) {
    this.db.prepare("UPDATE mp_rounds SET state='ROUND_PREPARED',started_at=?,deadline=? WHERE room_id=? AND position=?")
      .run(now+CLASSIC.prepareMs,now+CLASSIC.prepareMs+duration,id,position);
    this.db.prepare("UPDATE mp_matches SET phase='ROUND_PREPARED',round_index=? WHERE room_id=?").run(position,id);
    this.rooms.bump(id);
  }
  answer(user,id,payload) {
    const match=this.get(id), player=this.player(id,user);
    if(!match || match.phase!=='ROUND_LIVE')throw new ApiError(409,'ROUND_NOT_LIVE');
    const round=this.db.prepare('SELECT * FROM mp_rounds WHERE room_id=? AND position=?').get(id,match.round_index);
    if(round.id!==payload.roundId)throw new ApiError(409,'STALE_ROUND');
    const now=this.time();
    if(now>=round.deadline)throw new ApiError(409,'ANSWER_TOO_LATE');
    const old=this.db.prepare('SELECT selected_option FROM mp_answers WHERE round_id=? AND participant_id=?').get(round.id,player.participant_id);
    if(old)throw new ApiError(409,'ANSWER_ALREADY_ACCEPTED');
    const seq=this.db.prepare('SELECT count(*) AS n FROM mp_answers WHERE round_id=?').get(round.id).n+1;
    this.db.prepare('INSERT INTO mp_answers(round_id,participant_id,selected_option,accepted_at,elapsed_ms,sequence) VALUES (?,?,?,?,?,?)')
      .run(round.id,player.participant_id,payload.option,now,now-round.started_at,seq);
    this.rooms.bump(id);
  }
  reveal(match,round,now) {
    const key=this.db.prepare('SELECT correct_index FROM mp_answer_keys WHERE round_id=?').get(round.id);
    const answers=this.db.prepare('SELECT * FROM mp_answers WHERE round_id=? ORDER BY sequence').all(round.id);
    let fastest=null;
    for(const a of answers){
      const correct=a.selected_option===key.correct_index, points=score(correct,a.elapsed_ms,round.deadline-round.started_at);
      this.db.prepare('UPDATE mp_answers SET correct=?,points=? WHERE round_id=? AND participant_id=?')
        .run(correct?1:0,points,round.id,a.participant_id);
      if(correct && fastest===null)fastest=a.participant_id;
      this.db.prepare('UPDATE mp_match_players SET score=score+?,correct_count=correct_count+?,correct_elapsed=correct_elapsed+? WHERE participant_id=?')
        .run(points,correct?1:0,correct?a.elapsed_ms:0,a.participant_id);
    }
    if(fastest)this.db.prepare('UPDATE mp_match_players SET fastest_wins=fastest_wins+1 WHERE participant_id=?').run(fastest);
    this.db.prepare("UPDATE mp_rounds SET state='ROUND_REVEALED',reveal_until=? WHERE id=?").run(now+CLASSIC.revealMs,round.id);
    this.db.prepare("UPDATE mp_matches SET phase='ROUND_REVEALED' WHERE room_id=?").run(match.room_id);
    this.rooms.bump(match.room_id);
  }
  finish(id,reason) {
    const m=this.get(id);if(!m || m.phase==='MATCH_FINISHED')return;
    this.db.prepare("UPDATE mp_rounds SET state='CANCELLED' WHERE room_id=? AND state IN ('QUEUED','ROUND_PREPARED','ROUND_LIVE')").run(id);
    this.db.prepare("UPDATE mp_matches SET phase='MATCH_FINISHED',finished_at=?,finish_reason=? WHERE room_id=?")
      .run(Math.max(this.time(),m.started_at),reason,id);
    this.rooms.bump(id);
  }
  restart() {
    for(const m of this.db.prepare("SELECT room_id FROM mp_matches WHERE phase<>'MATCH_FINISHED'").all())this.finish(m.room_id,'INTERRUPTED');
  }
  advance() {
    for(const m of this.db.prepare("SELECT * FROM mp_matches WHERE phase<>'MATCH_FINISHED'").all()){
      const room=this.rooms.room(m.room_id);
      if(room.expires_at<=this.rooms.now()){this.finish(m.room_id,'ROOM_EXPIRED');continue;}
      const r=this.db.prepare('SELECT * FROM mp_rounds WHERE room_id=? AND position=?').get(m.room_id,m.round_index),now=this.time();
      if(m.phase==='ROUND_PREPARED' && now>=r.started_at){
        // A suspended process must not release a question with an already expired clock.
        if(now>=r.deadline){this.finish(m.room_id,'INTERRUPTED');continue;}
        this.db.prepare("UPDATE mp_rounds SET state='ROUND_LIVE' WHERE id=?").run(r.id);
        this.db.prepare("UPDATE mp_matches SET phase='ROUND_LIVE' WHERE room_id=?").run(m.room_id);this.rooms.bump(m.room_id);
      }else if(m.phase==='ROUND_LIVE' && now>=r.deadline)this.reveal(m,r,now);
      else if(m.phase==='ROUND_REVEALED' && now>=r.reveal_until){
        if(m.round_index===m.question_count)this.finish(m.room_id,'COMPLETED');
        else this.prepare(m.room_id,m.round_index+1,now,JSON.parse(room.settings).timerSeconds*1000);
      }
    }
  }
  projection(user,id) {
    const m=this.get(id);this.player(id,user);
    const players=this.db.prepare('SELECT * FROM mp_match_players WHERE room_id=? ORDER BY score DESC,correct_count DESC,fastest_wins DESC,correct_elapsed,participant_id').all(id);
    let previous=null,rank=0;
    const standings=players.map((p,i)=>{
      const sporting=[p.score,p.correct_count,p.fastest_wins,p.correct_elapsed].join(':');
      if(sporting!==previous)rank=i+1;previous=sporting;
      return {participantId:p.participant_id,displayName:p.display_name,rank,score:p.score,correctCount:p.correct_count,
        fastestWins:p.fastest_wins,accuracy:Math.floor(100*p.correct_count/m.question_count)};
    });
    const r=this.db.prepare('SELECT * FROM mp_rounds WHERE room_id=? AND position=?').get(id,m.round_index);
    const round={id:r.id,number:r.position,state:r.state,startedAtMs:r.started_at,deadlineMs:r.deadline,
      revealUntilMs:r.reveal_until,question:null,submitted:[],ownOption:null,reveal:null};
    if(r.state==='ROUND_LIVE' || r.state==='ROUND_REVEALED'){
      round.question=JSON.parse(r.question_json);
      const a=this.db.prepare('SELECT * FROM mp_answers WHERE round_id=? ORDER BY sequence').all(r.id);
      round.submitted=a.map(x=>x.participant_id);
      round.ownOption=a.find(x=>x.participant_id===this.player(id,user).participant_id)?.selected_option??null;
      if(r.state==='ROUND_REVEALED'){
        const k=this.db.prepare('SELECT * FROM mp_answer_keys WHERE round_id=?').get(r.id);
        round.reveal={correctIndex:k.correct_index,explanation:k.explanation,
          firstSubmission:a[0]?.participant_id??null,fastestCorrect:a.find(x=>x.correct)?.participant_id??null,
          answers:players.map(p=>{const x=a.find(v=>v.participant_id===p.participant_id);return {
            participantId:p.participant_id,option:x?.selected_option??null,elapsedMs:x?.elapsed_ms??null,
            sequence:x?.sequence??null,correct:Boolean(x?.correct),points:x?.points??0};})};
      }
    }
    return {phase:m.phase,questionCount:m.question_count,startedAtMs:m.started_at,finishedAtMs:m.finished_at,
      finishReason:m.finish_reason,round,standings};
  }
}
