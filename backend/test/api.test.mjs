import { test } from 'node:test';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { Store } from '../src/store.mjs';
import { createApi } from '../src/server.mjs';
import { ApiError, validateRequest, validateOutput } from '../src/contract.mjs';
import { geminiProvider } from '../src/gemini.mjs';
const token='A'.repeat(43);
const request={topic:'Java Programming',difficulty:'Hard',questionCount:5,timerSeconds:20,additionalInstructions:''};
const valid=()=>({questions:Array.from({length:5},(_,i)=>({text:`Which Java fact is number ${i}?`,options:['One','Two','Three','Four'],correctIndex:0,explanation:'One is the verified answer for this mock question.'}))});
async function fixture(run,provider=async()=>valid(),production=false) {
  let now=Date.now(),calls=0;const store=new Store(':memory:',()=>now);store.addUser('classmate',token);
  const server=createApi({store,model:'test-model',production,provider:async r=>{calls++;return provider(r);}});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const post=async(body=request,key=randomUUID(),auth=token,extra={})=>{
    const r=await fetch(`http://127.0.0.1:${server.address().port}/api/quizzes/generate`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:`Bearer ${auth}`,'Idempotency-Key':key,...extra},body:JSON.stringify(body)});
    return {status:r.status,body:await r.json()};};
  try{await run({post,store,calls:()=>calls,advance:ms=>now+=ms});}finally{await new Promise(resolve=>server.close(resolve));store.close();}
}
test('request validation trims input and rejects invalid types/settings/unknown controls',()=>{
  assert.equal(validateRequest({...request,topic:'  Java Programming  '}).topic,request.topic);
  for(const patch of [{topic:''},{topic:'..'},{topic:'x'.repeat(121)},{difficulty:'Very hard'},{questionCount:6},{questionCount:'5'},{timerSeconds:45},{additionalInstructions:'x'.repeat(501)},{role:'admin'},{score:9000}]) assert.throws(()=>validateRequest({...request,...patch}),ApiError);
});
test('output boundary rejects empty/duplicate/partial/range/coercion/extra-field batches',()=>{
  const patches=[q=>q.questions.pop(),q=>q.questions=[],q=>q.questions[1].text=q.questions[0].text.toUpperCase(),q=>q.questions[0].options[1]=' one ',q=>q.questions[0].correctIndex=4,q=>q.questions[0].correctIndex='0',q=>q.questions[0].text='',q=>q.questions[0].explanation='',q=>q.questions[0].options.push('Five'),q=>q.questions[0].extra=true];
  for(const patch of patches){const q=valid();patch(q);assert.throws(()=>validateOutput(q,request),/MALFORMED_QUESTIONS/);}
  assert.equal(validateOutput(valid(),request).length,5);
});
test('HTTP success is versioned; retry returns same quiz and one provider call',()=>fixture(async({post,calls})=>{
  const key=randomUUID(), first=await post(request,key),second=await post(request,key);
  assert.equal(first.status,200);assert.equal(first.body.schemaVersion,1);assert.equal(first.body.generationId,key);
  assert.equal(first.body.quiz.questions.length,5);assert.deepEqual(first,second);assert.equal(calls(),1);
  assert.equal((await post({...request,topic:'Geography'},key)).status,409);
}));
test('auth and strict HTTPS fail before provider; embedded package/id gives no access',()=>fixture(async({post,calls})=>{
  assert.equal((await post(request,randomUUID(),'package.com')).status,401);assert.equal(calls(),0);
}));
test('production requires local TLS proxy marker',()=>fixture(async({post,calls})=>{
  assert.equal((await post()).status,400);assert.equal(calls(),0);
  assert.equal((await post(request,randomUUID(),token,{'x-forwarded-proto':'https'})).status,200);
},undefined,true));
test('per-user quota persists in SQL and cache retrieval does not spend quota',()=>fixture(async({post,calls,advance})=>{
  let first;for(let i=0;i<5;i++){const key=randomUUID();if(!first)first=key;assert.equal((await post(request,key)).status,200);advance(61000);}
  assert.equal((await post()).status,429);assert.equal((await post(request,first)).status,200);assert.equal(calls(),5);
}));
test('global daily budget protects provider across users',()=>{
  let now=Date.now();const store=new Store(':memory:',()=>now);
  try{for(let i=0;i<20;i++){const user=`u${i}`,key=randomUUID();store.reserve(user,key,'fp');store.complete(user,key,200,{});}
    assert.throws(()=>store.reserve('another',randomUUID(),'fp'),/QUOTA_EXCEEDED/);
  }finally{store.close();}
});
test('IP throttle expires and concurrency rejects another request',()=>{
  let now=Date.now();const store=new Store(':memory:',()=>now);
  try{for(let i=0;i<30;i++)store.throttle('ip');assert.throws(()=>store.throttle('ip'),/RATE_LIMITED/);
    now+=61000;store.throttle('ip');store.reserve('one',randomUUID(),'fp');
    assert.throws(()=>store.reserve('one',randomUUID(),'fp'),/BUSY/);
    store.reserve('two',randomUUID(),'fp');assert.throws(()=>store.reserve('three',randomUUID(),'fp'),/BUSY/);
  }finally{store.close();}
});
test('double tap while generation pending has one provider call',()=>fixture(async({post,calls})=>{
  const key=randomUUID(),first=post(request,key);await new Promise(r=>setTimeout(r,30));
  assert.equal((await post(request,key)).status,425);assert.equal((await first).status,200);assert.equal(calls(),1);
},async()=>{await new Promise(r=>setTimeout(r,100));return valid();}));
test('errors are sanitized and cached; provider secrets never appear',()=>fixture(async({post,calls})=>{
  const key=randomUUID(),r=await post(request,key);assert.equal(r.status,503);assert.equal(r.body.error.code,'SERVER_UNAVAILABLE');
  assert.equal(JSON.stringify(r).includes('private-secret'),false);await post(request,key);assert.equal(calls(),1);
},async()=>{throw Error('private-secret');}));
for(const [status,code] of [[504,'TIMEOUT'],[429,'PROVIDER_LIMIT'],[503,'PROVIDER_UNAVAILABLE'],[502,'MALFORMED_QUESTIONS'],[422,'TOPIC_REJECTED']]) test(`recoverable ${code}`,()=>fixture(async({post})=>{
  const r=await post();assert.equal(r.status,status);assert.equal(r.body.error.code,code);
},async()=>{throw new ApiError(status,code);}));
test('restart with uncertain pending provider charge cannot call provider twice',()=>{
  let now=Date.now();const store=new Store(':memory:',()=>now),key=randomUUID();
  try{store.reserve('u',key,'fp');now+=100000;assert.throws(()=>store.reserve('u',key,'fp'),/REQUEST_INTERRUPTED/);}finally{store.close();}
});
test('Gemini structured output uses header secret, schema, bounded tokens and no automatic retries',async()=>{
  let calls=0;const provider=geminiProvider({apiKey:'private-secret',model:'gemini-2.5-flash-lite',freeTierConfirmed:true},async(url,opts)=>{
    calls++;assert.equal(url.includes('private-secret'),false);assert.equal(opts.headers['x-goog-api-key'],'private-secret');
    const body=JSON.parse(opts.body);assert.equal(body.generationConfig.responseMimeType,'application/json');
    assert.equal(body.generationConfig.responseJsonSchema.properties.questions.minItems,5);
    assert.ok(body.generationConfig.maxOutputTokens<=12000);
    return new Response(JSON.stringify({candidates:[{finishReason:'STOP',content:{parts:[{text:JSON.stringify(valid())}]}}]}));
  });assert.deepEqual(await provider(request),valid());assert.equal(calls,1);
});
test('provider limits/outage/malformed/safety/timeouts are mapped without provider bodies',async()=>{
  const config={apiKey:'private-secret',model:'gemini-2.5-flash-lite',freeTierConfirmed:true};
  for(const [response,code] of [[new Response('private-secret',{status:429}),'PROVIDER_LIMIT'],[new Response('private-secret',{status:500}),'PROVIDER_UNAVAILABLE'],[new Response('invalid-json'),'MALFORMED_QUESTIONS'],[Response.json({promptFeedback:{blockReason:'SAFETY'}}),'TOPIC_REJECTED']]){
    await assert.rejects(geminiProvider(config,async()=>response)(request),e=>e.code===code&&!e.message.includes('private-secret'));
  }
  await assert.rejects(geminiProvider(config,async()=>{throw new DOMException('secret','TimeoutError');})(request),/TIMEOUT/);
  await assert.rejects(geminiProvider({...config,freeTierConfirmed:false})(request),/SETUP_REQUIRED/);
});
test('selected Gemini 3.5 model uses a bounded low-thinking structured request',async()=>{
  const config={apiKey:'private-secret',model:'gemini-3.5-flash-lite',freeTierConfirmed:true};
  await geminiProvider(config,async(url,opts)=>{
    assert.match(url,/models\/gemini-3\.5-flash-lite:generateContent$/);
    const body=JSON.parse(opts.body);assert.deepEqual(body.generationConfig.thinkingConfig,{thinkingLevel:'LOW'});
    assert.equal(body.generationConfig.responseJsonSchema.properties.questions.maxItems,5);
    return Response.json({candidates:[{finishReason:'STOP',content:{parts:[{text:JSON.stringify(valid())}]}}]});
  })(request);
});
