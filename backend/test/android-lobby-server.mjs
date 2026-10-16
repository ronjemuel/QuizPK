// Isolated native-client integration fixture. Never reads config or the live DB.
import {mkdtempSync,rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {Store} from '../src/store.mjs';
import {createApi} from '../src/server.mjs';
const folder=mkdtempSync(join(tmpdir(),'quizpk-android-lobby-'));
const store=new Store(join(folder,'fixture.sqlite'));
store.addUser('synthetic-owner','a'.repeat(64));store.addUser('synthetic-player','b'.repeat(64));
const server=createApi({store,provider:async()=>{throw Error('PROVIDER_NOT_ALLOWED_IN_LOBBY_TEST');},model:'fixture-only'});
server.listen(0,'127.0.0.1',()=>console.log('PORT='+server.address().port));
function close(){server.close(()=>{store.close();rmSync(folder,{recursive:true,force:true});process.exit(0);});}
process.on('SIGTERM',close);process.on('SIGINT',close);
