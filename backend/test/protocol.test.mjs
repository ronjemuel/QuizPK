import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {validateCommand,COMMANDS,LIMITS} from '../src/multiplayer/protocol.mjs';
import {score,order,transition} from './protocol-reference.mjs';
import {score as classicScore} from '../src/multiplayer/classic.mjs';
const spec=JSON.parse(readFileSync(new URL('../../protocol/multiplayer-v1.json',import.meta.url)));
test('shared fixture is accepted by online codec; protocol is independent of cloud auth',()=>{
  assert.deepEqual(validateCommand(spec.createFixture,'room.create'),spec.createFixture);
  assert.deepEqual(spec.implementedCommands,COMMANDS);assert.equal(spec.presenceTimeoutMs,LIMITS.presenceMs);
  assert.deepEqual(spec.authorityNetworks,['ONLINE','LOCAL']);
  assert.ok(!JSON.stringify(spec.createFixture).includes('token'));
});
test('protocol rejects unknown versions, forged roles/ownership/scores and coercions',()=>{
  for(const patch of [{protocolVersion:2},{requestId:'bad'},{issuedAtMs:'123'},{actor:'MODERATOR'},
    {payload:{...spec.createFixture.payload,role:'MODERATOR'}},{payload:{...spec.createFixture.payload,ownerId:'other'}},
    {payload:{...spec.createFixture.payload,score:150}},{payload:{...spec.createFixture.payload,displayName:'x\u0000'}}])
    assert.throws(()=>validateCommand({...spec.createFixture,...patch},'room.create'));
});
test('production Classic scoring and shared reference agree on bounds, rounding, late, wrong and cancellation',()=>{
  assert.equal(spec.scoringDesignOnly,false);
  for(const v of spec.scoreVectors){assert.equal(score(v),v.expected);assert.equal(classicScore(v.correct,v.elapsedMs,v.durationMs,v.cancelled),v.expected);}
});
test('future first-submit/fastest-correct vectors distinguish wrong-first, ties and zero correct',()=>{
  for(const v of spec.orderingVectors)assert.deepEqual(order(v.submissions),{first:v.first,fastestCorrect:v.fastestCorrect});
});
test('future transition vectors require manual moderator starts and forbid contestant controls',()=>{
  assert.equal(spec.transitionDesignOnly,true);
  for(const v of spec.transitionVectors)assert.equal(transition(v),v.expected);
});
