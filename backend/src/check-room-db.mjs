// Operator-only, read-only check. Never dumps identities, token hashes or credentials.
import {existsSync,readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import {DatabaseSync} from 'node:sqlite';
import {loadConfig} from './config.mjs';
let db;
try {
  const config=loadConfig(),args=process.argv.slice(2);
  if(args.length && (args.length!==2 || args[0]!=='--token-file'))throw Error();
  const result={databasePresent:existsSync(config.dbPath),identitySchemaPresent:false,roomSchemaPresent:false};
  if(result.databasePresent) {
    db=new DatabaseSync(config.dbPath,{readOnly:true});
    result.identitySchemaPresent=Boolean(db.prepare("SELECT 1 FROM sqlite_master WHERE type='table' AND name='users'").get());
    result.roomSchemaPresent=Boolean(db.prepare("SELECT 1 FROM sqlite_master WHERE type='table' AND name='mp_schema'").get());
    if(args.length) {
      const token=readFileSync(args[1],'utf8').trim();
      if(!/^[A-Za-z0-9_-]{43,128}$/.test(token) || !result.identitySchemaPresent)throw Error();
      result.tokenMatchesEnabledIdentity=Boolean(db.prepare('SELECT 1 FROM users WHERE token_hash=? AND enabled=1')
        .get(createHash('sha256').update(token).digest('hex')));
    }
  }
  console.log(JSON.stringify(result));
} catch {console.error('Private database check failed. No credentials or database contents printed.');process.exitCode=1;}
finally {db?.close();}
