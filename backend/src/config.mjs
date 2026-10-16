import { existsSync, mkdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
export const backendRoot = fileURLToPath(new URL('../', import.meta.url));
// Explicit custom filename. Existing workspace location stays server-only.
export function loadConfig() {
  const paths = [resolve(backendRoot, '../GEMINI_API_KEY/GEMINI_API_KEY.env'), resolve(backendRoot, 'GEMINI_API_KEY.env')];
  const inherited = { ...process.env };
  for (const path of paths) if (existsSync(path)) process.loadEnvFile(path);
  Object.assign(process.env, inherited); // Hosting secrets take precedence over local files.
  const dbPath = resolve(backendRoot, process.env.QUIZPK_DB_PATH || './data/quizpk.sqlite');
  mkdirSync(dirname(dbPath), { recursive: true, mode: 0o700 });
  return { apiKey: process.env.GEMINI_API_KEY || '', model: process.env.GEMINI_MODEL || '',
    freeTierConfirmed: process.env.GEMINI_FREE_TIER_CONFIRMED === 'true', dbPath,
    host: process.env.HOST || '127.0.0.1', port: Number(process.env.PORT || 8787),
    production: process.env.NODE_ENV === 'production' };
}
