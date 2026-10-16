// One tightly limited live request. Never prints output questions, headers, or credentials.
import { loadConfig } from './config.mjs';
import { geminiProvider } from './gemini.mjs';
import { validateRequest, validateOutput, ApiError } from './contract.mjs';
try {
  const config=loadConfig();
  if(!config.apiKey||!config.model||!config.freeTierConfirmed)throw new ApiError(503,'SETUP_REQUIRED');
  const request=validateRequest({topic:'Java Programming',difficulty:'Easy',questionCount:5,timerSeconds:15});
  const questions=validateOutput(await geminiProvider(config)(request),request);
  console.log(JSON.stringify({liveGeminiVerified:true,validatedQuestionCount:questions.length}));
} catch(e) {console.error(JSON.stringify({liveGeminiVerified:false,code:e instanceof ApiError?e.code:'UNVERIFIED'}));process.exitCode=1;}
