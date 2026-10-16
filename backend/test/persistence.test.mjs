import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync,rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { Store } from '../src/store.mjs';
test('on-disk restart keeps auth, cached response, quota and uncertain reservations',()=>{
  const folder=mkdtempSync(join(tmpdir(),'quizpk-test-')),path=join(folder,'db.sqlite');
  let now=Date.now(),store=new Store(path,()=>now);
  try {
    store.addUser('u','A'.repeat(43));store.reserve('u','first','fp');store.complete('u','first',200,{safe:true});
    now+=61000;store.reserve('u','pending','fp');store.close();store=new Store(path,()=>now);
    assert.equal(store.authenticate('Bearer '+'A'.repeat(43)),'u');
    assert.deepEqual(store.reserve('u','first','fp'),{status:200,body:{safe:true}});
    assert.throws(()=>store.reserve('u','pending','fp'),/IN_PROGRESS/);
    now+=100000;assert.throws(()=>store.reserve('u','pending','fp'),/REQUEST_INTERRUPTED/);
    for(let i=0;i<3;i++){store.reserve('u',String(i),'fp');store.complete('u',String(i),200,{});now+=61000;}
    assert.throws(()=>store.reserve('u','extra','fp'),/QUOTA_EXCEEDED/);
    store.db.prepare('UPDATE users SET enabled=0 WHERE id=?').run('u');
    assert.throws(()=>store.authenticate('Bearer '+'A'.repeat(43)),/AUTH_REQUIRED/);
  } finally {store.close();rmSync(folder,{recursive:true,force:true});}
});
