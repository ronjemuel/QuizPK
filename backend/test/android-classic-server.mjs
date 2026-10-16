// Isolated Java integration fixture. No config loader, provider, live DB or real credentials.
import {mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {createInterface} from 'node:readline';
import {Store} from '../src/store.mjs';
import {createApi} from '../src/server.mjs';
const dir=mkdtempSync(join(tmpdir(),'quizpk-android-classic-')),path=join(dir,'synthetic.sqlite');
let clock=1791849600000,store=new Store(path,()=>clock),server,port=0;
store.addUser('synthetic-owner','a'.repeat(64));store.addUser('synthetic-player','b'.repeat(64));
async function start(){
 server=createApi({store,roomOptions:{monotonic:()=>clock},provider:async()=>{throw Error('PROVIDER_FORBIDDEN');},model:'fixture'});
 await new Promise(r=>server.listen(port,'127.0.0.1',r));port=server.address().port;
}
await start();console.log('PORT='+port);
const input=createInterface({input:process.stdin});
for await(const line of input){
 const message=JSON.parse(line);
 if(message.type==='advance'){clock+=message.ms;console.log('ACK');}
 else if(message.type==='restart'){await new Promise(r=>server.close(r));store.close();store=new Store(path,()=>clock);await start();console.log('ACK');}
 else if(message.type==='close')break;
}
input.close();process.stdin.pause();
await new Promise(r=>server.close(r));store.close();rmSync(dir,{recursive:true,force:true});
