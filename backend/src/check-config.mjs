import { loadConfig } from './config.mjs';
try { const c=loadConfig(); console.log(JSON.stringify({geminiKeyConfigured:Boolean(c.apiKey),modelConfigured:Boolean(c.model),freeTierConfirmed:c.freeTierConfirmed})); }
catch {console.error('Configuration could not be loaded.');process.exitCode=1;}
