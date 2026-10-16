import { Store } from './store.mjs';
import { loadConfig } from './config.mjs';
try {
 const id=process.argv[2];if(!/^[a-zA-Z0-9_-]{1,40}$/.test(id||''))throw Error();
 const store=new Store(loadConfig().dbPath);
 try {store.db.prepare('UPDATE users SET enabled=0 WHERE id=?').run(id);}finally{store.close();}
 console.log('User access revoked. Cached generation responses also require an enabled user.');
} catch {console.error('Revocation failed. Check user ID and database access.');process.exitCode=1;}
