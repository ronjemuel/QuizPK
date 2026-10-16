import { createServer } from 'node:http';
import { pathToFileURL } from 'node:url';
import { ApiError, validateRequest, validateOutput } from './contract.mjs';
import { Store, hash } from './store.mjs';
import { loadConfig } from './config.mjs';
import { geminiProvider } from './gemini.mjs';
import { RoomService } from './multiplayer/rooms.mjs';
import { roomHttp } from './multiplayer/http.mjs';
const errorBody=code=>({schemaVersion:1,error:{code}});
export function createApi({store,provider,model,production=false,roomOptions={}}) {
  const rooms=new RoomService(store,roomOptions);rooms.start();
  const server=createServer(async(req,res)=>{
    const send=(status,body)=>{res.writeHead(status,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff',...(status===429||status===425?{'Retry-After':'60'}:{})});res.end(JSON.stringify(body));};
    try {
      // Production binds loopback and trusts only its local TLS reverse proxy.
      if (production && req.headers['x-forwarded-proto']!=='https') throw new ApiError(400,'HTTPS_REQUIRED');
      if (req.method==='GET' && req.url==='/health') return send(200,{status:'ok'});
      if (req.url==='/api/rooms' || req.url.startsWith('/api/rooms/')) return await roomHttp(req,send,store,rooms);
      if (req.method!=='POST' || req.url!=='/api/quizzes/generate') throw new ApiError(404,'NOT_FOUND');
      store.throttle(req.socket.remoteAddress || 'unknown');
      const user=store.authenticate(req.headers.authorization);
      const key=req.headers['idempotency-key'];
      if (typeof key!=='string'||!/^[a-f0-9-]{36}$/.test(key)) throw new ApiError(400,'INVALID_IDEMPOTENCY_KEY');
      if (!/^application\/json(?:;|$)/i.test(req.headers['content-type']||'')) throw new ApiError(415,'INVALID_REQUEST');
      let size=0; const chunks=[];
      for await (const chunk of req) {size+=chunk.length; if (size>8192) throw new ApiError(413,'INVALID_REQUEST'); chunks.push(chunk);}
      let parsed; try { parsed=JSON.parse(Buffer.concat(chunks).toString('utf8')); } catch { throw new ApiError(400,'INVALID_REQUEST'); }
      const request=validateRequest(parsed), cached=store.reserve(user,key,hash(JSON.stringify(request)));
      if (cached) return send(cached.status,cached.body);
      let status=200,body;
      try {
        const questions=validateOutput(await provider(request),request);
        body={schemaVersion:1,generationId:key,quiz:{...request,additionalInstructions:undefined,createdAt:Date.now(),origin:'AI_GENERATED',model,questions}};
      } catch (e) { status=e instanceof ApiError?e.status:503;body=errorBody(e instanceof ApiError?e.code:'SERVER_UNAVAILABLE'); }
      store.complete(user,key,status,body); send(status,body);
    } catch (e) {
      const code=e instanceof ApiError?e.code:'SERVER_UNAVAILABLE';
      send(e instanceof ApiError?e.status:503,req.url.startsWith('/api/rooms')?{protocolVersion:1,error:{code}}:errorBody(code));
    }
  });
  server.requestTimeout=15000; server.headersTimeout=10000; server.maxHeadersCount=32;
  const timer=setInterval(()=>{try{rooms.tick();}catch{/* Requests fail closed; never log private state. */}},250);
  timer.unref();server.once('close',()=>clearInterval(timer));
  server.setTimeout(70000); return server;
}
if (process.argv[1] && import.meta.url===pathToFileURL(process.argv[1]).href) {
  try {
    const config=loadConfig();
    if (config.production && config.host!=='127.0.0.1') throw Error();
    const store=new Store(config.dbPath), server=createApi({store,provider:geminiProvider(config),model:config.model,production:config.production,
      roomOptions:{classicEnabled:process.env.QUIZPK_CLASSIC_ENABLED!=='false'}});
    server.listen(config.port,config.host,()=>console.log('QuizPK backend listening; request bodies and credentials are never logged.'));
    process.on('SIGTERM',()=>server.close(()=>{store.close();process.exit(0);}));
  } catch { console.error('Backend setup failed. Check private environment and database permissions.'); process.exitCode=1; }
}
