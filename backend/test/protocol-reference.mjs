// Test-only executable interpretation of the future rules. Not a shipped match engine.
export function score({correct,elapsedMs,durationMs,cancelled}) {
  if(cancelled || !correct || elapsedMs<0 || elapsedMs>=durationMs)return 0;
  return 100+Math.floor(50*(durationMs-elapsedMs)/durationMs);
}
export function order(submissions) {
  const sorted=[...submissions].sort((a,b)=>a.sequence-b.sequence);
  return {first:sorted[0]?.id ?? null,fastestCorrect:sorted.find(s=>s.correct)?.id ?? null};
}
export function transition(v) {
  if(v.action==='host.disconnect')return v.actor==='SERVER'?v.state:'FORBIDDEN';
  if(v.action==='server.restart')return v.actor==='SERVER'?(v.state==='ROUND_LIVE'?'ROUND_CLOSED':v.state):'FORBIDDEN';
  if(v.action==='round.deadline')return v.actor!=='SERVER'?'FORBIDDEN':v.state==='ROUND_LIVE'?'ROUND_CLOSED':'INVALID_STATE';
  const actor=v.action==='match.start'?(v.mode==='MODERATOR'?'MODERATOR':'OWNER'):(v.mode==='MODERATOR'?'MODERATOR':'SERVER');
  if(v.actor!==actor)return 'FORBIDDEN';
  const allowed={
    'match.start':['LOBBY',v.mode==='MODERATOR'?'READY':'ROUND_PREPARED'],
    'round.prepare':['READY','ROUND_PREPARED'],
    'round.start':['ROUND_PREPARED','ROUND_LIVE'],
    'round.reveal':['ROUND_CLOSED','ROUND_REVEALED'],
    'round.next':['ROUND_REVEALED','ROUND_PREPARED']
  }[v.action];
  if(!allowed || v.state!==allowed[0])return 'INVALID_STATE';
  if(v.action==='match.start' && v.contestants<2)return 'TOO_FEW_CONTESTANTS';
  return allowed[1];
}
