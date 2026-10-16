import { randomBytes } from 'node:crypto';
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { loadConfig, backendRoot } from './config.mjs';
import { Store } from './store.mjs';
try {
  const id=process.argv[2]; if(!/^[a-zA-Z0-9_-]{1,40}$/.test(id||'')) throw Error();
  const token=randomBytes(32).toString('base64url'), folder=resolve(backendRoot,'private'); mkdirSync(folder,{recursive:true,mode:0o700});
  const path=resolve(folder,`${id}.token`);writeFileSync(path,token+'\n',{mode:0o600,flag:'wx'});
  const store=new Store(loadConfig().dbPath); try{store.addUser(id,token);} finally{store.close();}
  console.log('Individual access token saved to backend/private/<user-id>.token; transfer privately. No token is printed.');
} catch {console.error('User creation failed. Choose a new unique user ID and check permissions.');process.exitCode=1;}
