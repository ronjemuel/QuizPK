import { ApiError } from '../contract.mjs';
export const PROTOCOL_VERSION = 1;
export const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
export const CODE_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
export const LIMITS = Object.freeze({
  roomTtlMs: 7200000, presenceMs: 45000, retentionMs: 86400000,
  commandAgeMs: 300000, futureSkewMs: 30000, maxRooms: 20,
  userCreatesDaily: 5, globalCreatesDaily: 100,
  userRequestsMinute: 120, roomRequestsMinute: 300, ingressMinute: 3000,
  userCommandsMinute: 20, userCommandsHour: 600, globalCommandsHour: 10000
});
export const COMMANDS = ['room.create','room.join','room.leave','room.heartbeat','room.ready','room.settings','match.start','match.answer'];
export const CATEGORIES = ['All Categories','General','Science','History','Geography','Technology','Sports','Movies','Music','Literature','Animals','Food','Space'];
function object(value, keys, required=keys) {
  if (!value || Array.isArray(value) || typeof value !== 'object' ||
      Object.keys(value).some(k=>!keys.includes(k)) || required.some(k=>!Object.hasOwn(value,k)))
    throw new ApiError(400,'INVALID_COMMAND');
}
function text(value, min, max) {
  if (typeof value !== 'string') throw new ApiError(400,'INVALID_COMMAND');
  const clean=value.normalize('NFKC').trim();
  if (clean.length<min || clean.length>max || /[\p{Cc}\p{Cf}]/u.test(clean))
    throw new ApiError(400,'INVALID_COMMAND');
  return clean;
}
export function settings(input) {
  object(input,['category','difficulty','questionCount','timerSeconds']);
  if (!CATEGORIES.includes(input.category) || !['Easy','Normal','Hard','Expert'].includes(input.difficulty) ||
      ![5,10,15,20].includes(input.questionCount) || ![10,15,20,30].includes(input.timerSeconds))
    throw new ApiError(400,'INVALID_SETTINGS');
  return {category:input.category,difficulty:input.difficulty,questionCount:input.questionCount,timerSeconds:input.timerSeconds};
}
export function validateCommand(input, type) {
  object(input,['protocolVersion','requestId','issuedAtMs','type','payload','expectedRevision'],
    ['protocolVersion','requestId','issuedAtMs','type','payload']);
  if (input.protocolVersion!==1) throw new ApiError(400,'PROTOCOL_UNSUPPORTED');
  if (!UUID.test(input.requestId) || input.type!==type || !COMMANDS.includes(type) ||
      !Number.isSafeInteger(input.issuedAtMs) || input.issuedAtMs<=0)
    throw new ApiError(400,'INVALID_COMMAND');
  if (type==='room.settings' || type==='match.start') {
    if (!Number.isSafeInteger(input.expectedRevision) || input.expectedRevision<1) throw new ApiError(400,'INVALID_REVISION');
  } else if (Object.hasOwn(input,'expectedRevision')) throw new ApiError(400,'INVALID_COMMAND');
  let payload;
  if (type==='room.create') {
    object(input.payload,['mode','displayName','settings']);
    if (!['CLASSIC','MODERATOR'].includes(input.payload.mode)) throw new ApiError(400,'INVALID_MODE');
    payload={mode:input.payload.mode,displayName:text(input.payload.displayName,1,40),settings:settings(input.payload.settings)};
  } else if (type==='room.join') {
    object(input.payload,['code','displayName']);
    const code=typeof input.payload.code==='string'?input.payload.code.normalize('NFKC').trim().toUpperCase():'';
    if (code.length!==6 || ![...code].every(c=>CODE_ALPHABET.includes(c))) throw new ApiError(404,'ROOM_NOT_FOUND');
    payload={code,displayName:text(input.payload.displayName,1,40)};
  } else if (type==='room.ready') {
    object(input.payload,['ready']);
    if (typeof input.payload.ready!=='boolean') throw new ApiError(400,'INVALID_COMMAND');
    payload={ready:input.payload.ready};
  } else if (type==='room.settings') payload=settings(input.payload);
  else if(type==='match.answer') {
    object(input.payload,['roundId','option']);
    if(!UUID.test(input.payload.roundId) || !Number.isInteger(input.payload.option) || input.payload.option<0 || input.payload.option>3)
      throw new ApiError(400,'INVALID_COMMAND');
    payload={roundId:input.payload.roundId,option:input.payload.option};
  }
  else { object(input.payload,[]); payload={}; }
  return {protocolVersion:1,requestId:input.requestId,issuedAtMs:input.issuedAtMs,type,payload,
    ...(['room.settings','match.start'].includes(type)?{expectedRevision:input.expectedRevision}:{})};
}
