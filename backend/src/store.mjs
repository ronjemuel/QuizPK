import { DatabaseSync } from 'node:sqlite';
import { createHash, timingSafeEqual } from 'node:crypto';
import { ApiError } from './contract.mjs';
import { migrateMultiplayer } from './multiplayer/migrations.mjs';
export const hash = s => createHash('sha256').update(s).digest('hex');
export class Store {
  constructor(path, now = () => Date.now()) {
    this.now = now; this.db = new DatabaseSync(path);
    this.db.exec(`PRAGMA journal_mode=WAL; PRAGMA busy_timeout=5000;
      CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, token_hash TEXT NOT NULL UNIQUE, enabled INTEGER NOT NULL DEFAULT 1);
      CREATE TABLE IF NOT EXISTS jobs(user_id TEXT NOT NULL, key TEXT NOT NULL, fingerprint TEXT NOT NULL, created INTEGER NOT NULL, response TEXT, status INTEGER, PRIMARY KEY(user_id,key));
      CREATE TABLE IF NOT EXISTS throttle(key TEXT PRIMARY KEY, window INTEGER NOT NULL, count INTEGER NOT NULL);`);
    try { migrateMultiplayer(this.db); }
    catch(e) { this.db.close(); throw e; }
  }
  addUser(id, token) { this.db.prepare('INSERT INTO users(id,token_hash) VALUES (?,?)').run(id,hash(token)); }
  authenticate(header) {
    const token = /^Bearer ([A-Za-z0-9_-]{43,128})$/.exec(header || '')?.[1];
    if (!token) throw new ApiError(401,'AUTH_REQUIRED');
    const digest = hash(token);
    const row = this.db.prepare('SELECT id,token_hash FROM users WHERE token_hash=? AND enabled=1').get(digest);
    if (!row || !timingSafeEqual(Buffer.from(digest),Buffer.from(row.token_hash))) throw new ApiError(401,'AUTH_REQUIRED');
    return row.id;
  }
  throttle(ip) {
    const now=this.now(), key=hash(ip), window=Math.floor(now/60000);
    this.db.prepare('DELETE FROM throttle WHERE window < ?').run(window-1);
    this.db.prepare('INSERT INTO throttle VALUES (?,?,1) ON CONFLICT(key) DO UPDATE SET count=CASE WHEN window=excluded.window THEN count+1 ELSE 1 END, window=excluded.window').run(key,window);
    if (this.db.prepare('SELECT count FROM throttle WHERE key=?').get(key).count > 30) throw new ApiError(429,'RATE_LIMITED');
  }
  reserve(user, key, fingerprint) {
    this.db.exec('BEGIN IMMEDIATE');
    try {
      const now=this.now(), old=this.db.prepare('SELECT * FROM jobs WHERE user_id=? AND key=?').get(user,key);
      if (old) {
        if (old.fingerprint !== fingerprint) throw new ApiError(409,'IDEMPOTENCY_CONFLICT');
        if (old.response) return {status:old.status,body:JSON.parse(old.response)};
        // An interrupted process never repeats an uncertain provider charge.
        throw new ApiError(old.created < now-90000 ? 409 : 425,old.created < now-90000 ? 'REQUEST_INTERRUPTED' : 'IN_PROGRESS');
      }
      const day=Math.floor(now/86400000)*86400000;
      const count=(since,uid) => uid ? this.db.prepare('SELECT count(*) AS n FROM jobs WHERE created>=? AND user_id=?').get(since,uid).n : this.db.prepare('SELECT count(*) AS n FROM jobs WHERE created>=?').get(since).n;
      if (count(day,user)>=5 || count(day)>=20 || count(now-60000,user)>=2) throw new ApiError(429,'QUOTA_EXCEEDED');
      if (this.db.prepare('SELECT count(*) AS n FROM jobs WHERE response IS NULL AND created>?').get(now-90000).n>=2 || this.db.prepare('SELECT count(*) AS n FROM jobs WHERE user_id=? AND response IS NULL AND created>?').get(user,now-90000).n>=1) throw new ApiError(429,'BUSY');
      this.db.prepare('INSERT INTO jobs(user_id,key,fingerprint,created) VALUES (?,?,?,?)').run(user,key,fingerprint,now);
      return null;
    } finally { this.db.exec('COMMIT'); }
  }
  complete(user,key,status,body) { this.db.prepare('UPDATE jobs SET response=?,status=? WHERE user_id=? AND key=?').run(JSON.stringify(body),status,user,key); }
  close() { this.db.close(); }
}
