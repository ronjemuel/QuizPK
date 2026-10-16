import { ApiError } from '../contract.mjs';
import { validateCommand, UUID } from './protocol.mjs';

async function body(req) {
  if (!/^application\/json(?:;|$)/i.test(req.headers['content-type']||''))
    throw new ApiError(415,'INVALID_COMMAND');
  const chunks=[];let size=0;
  for await(const chunk of req) {
    size+=chunk.length;
    if(size>4096)throw new ApiError(413,'INVALID_COMMAND');
    chunks.push(chunk);
  }
  try{return JSON.parse(Buffer.concat(chunks).toString('utf8'));}
  catch {throw new ApiError(400,'INVALID_COMMAND');}
}
// Authentication is transport-specific; RoomService receives a resolved principal only.
export async function roomHttp(req,send,store,rooms) {
  try {
    rooms.ingress(req.socket.remoteAddress || 'unknown');
    const user=store.authenticate(req.headers.authorization);rooms.userRate(user);
    const classic=req.headers['x-quizpk-features']==='classic-v1';
    if(req.method==='GET' && req.url==='/api/rooms/current')return send(200,rooms.snapshot(user,null,classic));
    const path=/^\/api\/rooms\/([0-9a-f-]{36})(?:\/(heartbeat|leave|ready|settings))?$/.exec(req.url);
    if(req.method==='GET' && path && !path[2] && UUID.test(path[1]))
      return send(200,rooms.snapshot(user,path[1],classic));
    const matchPath=/^\/api\/rooms\/([0-9a-f-]{36})\/match\/(start|answer)$/.exec(req.url);
    let type,id=null;
    if(req.method==='POST' && req.url==='/api/rooms')type='room.create';
    else if(req.method==='POST' && req.url==='/api/rooms/join')type='room.join';
    else if(req.method==='POST' && matchPath && UUID.test(matchPath[1])) {
      type='match.'+matchPath[2];id=matchPath[1];
    } else if(req.method==='POST' && path?.[2] && UUID.test(path[1])) {
      type='room.'+path[2];id=path[1];
    } else throw new ApiError(404,'NOT_FOUND');
    rooms.writeRate(user);
    if(type==='room.join')rooms.guessRate(user);
    const command=validateCommand(await body(req),type);
    return send(200,rooms.command(user,command,id));
  } catch(e) {
    return send(e instanceof ApiError?e.status:503,{protocolVersion:1,error:{code:e instanceof ApiError?e.code:'SERVER_UNAVAILABLE'}});
  }
}
