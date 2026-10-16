# M4.2 Online lobby setup and review

M4.1 was approved. M4.2 adds native Android Online connection, create/join and
Classic/Moderator lobbies using that backend and common protocol. Match play,
moderator authoring and LAN runtime remain deferred. Review the
[two-device plan](M4_2_DEVICE_TEST_PLAN.md), [deployment/migration plan](M4_2_DEPLOYMENT_PLAN.md),
[test evidence](TEST_NOTES.md) and [file inventory](M4_CHANGED_FILES.md).

Install `artifacts/QuizPK-M4.2-online-lobby-debug.apk` as an update, preserving
`com.zeke.quizpk` data. Multiplayer → Online uses the existing encrypted M3
connection. Connect/Update connection saves the same HTTPS origin and individual
token used by AI Quiz. Each friend needs a distinct login for the same backend;
codes confer no role or identity. Create chooses Classic (playing host) or
Moderator (separate nonplaying owner), name and settings. Join needs a name and
six-character code. Lobby offers ready state, owner settings, copy/share and
confirmed leave/close. Back can keep your reserved seat and return Home.

Foreground snapshots poll every 5s and heartbeat every 15s. After network failure,
read recovery backs off to 60s; 429 honors a 60–300s cooldown. A write with an
uncertain response requires explicit Retry pending action with the exact original
request ID/body. Configuration recreation retains it in memory; process restart
recovers membership through authenticated current-room lookup. No lobby state,
tokens or pending bodies are added to the solo SQLite DB or disk logs. Seats are
reserved through disconnection until leave/expiry; readiness is reset by the server.

**The live Termux backend/DB was not upgraded.** Its M3-only room responses show
the unsupported-room message. Lobby testing against that host needs separate
authorization of the deployment plan; installing this APK does not deploy a server.
The remaining server setup/checkpoint guidance below originated in M4.1 and is an
operator reference, not a record of a live deployment in M4.2.

## Recover the approved M4.1 source

Pre-edit checkpoint: `../QuizPK_Checkpoints/QuizPK-M4.1-approved-before-M4.2.tar.gz`,
192 source files, companion `.manifest.json`, verified archive SHA-256
`c76afd06599d4e9b0d551b2229ecc8925091aa22130e2c9fb66de6eedcf5f958`.
Restore into a new empty directory and compare manifest hashes before selecting
source to recover. Excludes private state, signing, APKs, builds and validation.
The earlier M3 checkpoint and APK remain retained unchanged. Do not extract over
the host or uninstall the Android app. See the deployment plan for DB recovery.

## Recover M3 without touching private state

Verified source archive: `../QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz`.
Its companion `.manifest.json` records source file checksums. Archive SHA-256:
`8fe1a54a07768aaec255b67a1c54923b55127e2005e673d7939386d57f690d28`.
All 176 files were restored into an isolated temporary directory and compared;
the restored M3 backend passed its original 20 tests. This source-only archive
was derived exclusively from the verified pre-edit checkpoint, omitting historical
validation reports. The original 275-file archive is also retained unchanged as
`QuizPK-M3-approved-before-M4.1.tar.gz`; no post-edit source entered either baseline.

Extract into a **new empty directory**, inspect there, and use its source to
recover. Do not extract over the live project or server. Credentials, signing
files, private databases, generated builds and APKs are deliberately excluded.
The existing `artifacts/QuizPK-M3-ai-debug.apk` is retained separately. Never
uninstall the app to roll back: that deletes Android saved data. No Android
schema/signature change occurred in M4.1. M4.2 uses Android SQLite v3 unchanged,
version code 4 / name `1.3-M4.2`, and the preserved M3 debug signer.

After installing version 4, the ordinary installer rejects the older version 3
APK. For a reviewed debug rollback on an authorized ADB connection,
`adb install -r -d artifacts/QuizPK-M3-ai-debug.apk` retains app data and permits
the debug version downgrade; this command was not executed here. Alternatively
build the recovered M3 source with a higher version code and the same existing
debug signer. Never uninstall/clear storage to make a rollback install succeed.

The completed M4.2 source is also retained separately as
`../QuizPK_Checkpoints/QuizPK-M4.2-online-lobby-review.tar.gz`, with its source
hash manifest. Its verification is under `validation/m4.2/review-checkpoint.json`.
Like the earlier checkpoints, restore it into a new empty directory, then inspect;
private configuration, databases, signing and APKs are excluded.

## Local automated verification

Node.js 24, one persistent process and durable SQLite remain required. There are
no new dependencies and no `npm install` step. From `backend/`:

```sh
npm test
node src/check-room-db.mjs
```

Tests use isolated temporary databases, distinct fake identities, real loopback
HTTP connections and injected time. They never call Gemini or use the user's DB.
The read-only checker prints booleans and opens no missing database. The workspace
has no private backend DB. The existing host was subsequently located at
`/data/data/com.termux/files/home/quizpk-server`; its `data/quizpk.sqlite` was checked
read-only against both existing distinct token files. Both match enabled identities;
the live DB still has M3-only schema. Its DB/WAL, token files and provider credential
are byte-for-byte preserved. SQLite updated only transient shared-memory reader
coordination in `-shm`; that live coordination file was never restored or overwritten.
Evidence: `validation/m4.1/existing-termux-db-readonly.json`.

On the actual host, validate the **existing** identity and DB privately, with the
same absolute `QUIZPK_DB_PATH` used by the working M3 process:

```sh
QUIZPK_DB_PATH=/private/absolute/path/quizpk.sqlite \
  node src/check-room-db.mjs --token-file /private/absolute/path/your-user.token
```

Replace only the example paths. Output is boolean `databasePresent`,
`identitySchemaPresent`, `roomSchemaPresent`, and, when available,
`tokenMatchesEnabledIdentity`. It prints no IDs, tokens, hashes or SQL rows. Before
upgrade, roomSchemaPresent should be false; after the migration it should be true.
A missing DB or false identity result must be resolved against the existing
service configuration; do not create/reset a replacement DB. This host's read-only
identity check passed; a post-upgrade room-schema check remains a later operator step.

The existing `create-user.mjs` was tested against a populated M3-compatible
temporary DB. For additional classmates, use the existing CLI with that same live
DB path and a new unique user ID, per [backend instructions](../backend/README.md).
It saves a distinct token in a private file and prints no token. Existing M3 users
already have lobby permission; generation quotas/permissions remain unchanged.
Do not create a universal classroom or moderator secret. Moderator status comes
only from creating that room, not token naming or a client role flag.

## Upgrade one existing Termux server after review

No live server, tunnel or durable private DB contents changed during this stage. The following
is an operator procedure for the existing host, not an action already executed:

1. Retain the current working backend directory for rollback. Stop its **one**
   Node process cleanly and make a private SQLite backup, including any WAL state
   (use SQLite backup facilities or copy after a clean stop). Keep this DB backup
   outside source/checkpoints/shared storage; it contains private auth and quizzes.
2. Copy `backend/src/` including the new `multiplayer/` directory and the unchanged
   `package.json` into the server directory. Preserve its `private/`, DB path,
   provider `.env`, non-secret model/free-tier settings and existing tunnel/proxy.
   Do not copy the whole Android workspace, private files or reference images.
3. Run the read-only identity check above, then start the upgraded service with
   **exactly the existing private environment and DB path**, `NODE_ENV=production`
   and `HOST=127.0.0.1`. `npm start` applies the additive multiplayer migration.
   Do not start a second replica or put this SQLite service in ephemeral hosting.
4. Recheck the same token file and DB read-only. Existing users/jobs/throttle and
   Android SQLite v3 data remain intact. Keep port 8787 on loopback and the public
   edge HTTPS-only. Preserve existing Gemini settings; room commands use no AI.
5. For rollback, stop Node and restore M3 **source** from the retained directory
   or verified source checkpoint; M3 ignores the additive `mp_*` tables. Do not
   replace the DB with an older backup after new generation/room activity; that
   would discard saved jobs and identities. Keep any DB recovery manual/private.

The existing Caddy deployment stays supported. For a temporary Termux development
tunnel, in a separate terminal with cloudflared already installed:

```sh
cloudflared tunnel --url http://127.0.0.1:8787
```

Use the actual HTTPS origin it supplies. Confirm `/health` over that origin and
verify authenticated room requests through the same edge before relying on it.
The proxy must supply `X-Forwarded-Proto: https`; if it does not, fix the local proxy
instead of disabling production TLS checks. Never put tokens in curl arguments,
query strings, tunnel logs, screenshots or shared output. Tunnel hostnames change,
SSE is unsupported, and uptime is not guaranteed; short polling is used. Termux
background survival and two-phone connectivity still need physical validation.
See [Cloudflare's Quick Tunnel limits](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

## Manual backend smoke without printing credentials

Use two distinct private user token files for the **same** DB and a real origin.
This script creates a short-lived test lobby, joins the second identity, checks
the snapshot and closes the room; it consumes room creation/write quota, not AI
quota. Neither identity may already occupy another lobby. Run in a private operator
terminal, with shell tracing off. Environment values below are paths/origin only:

```sh
QUIZPK_ORIGIN=https://your-real-host.example \
QUIZPK_OWNER_TOKEN_FILE=/private/path/owner.token \
QUIZPK_PLAYER_TOKEN_FILE=/private/path/player.token node --input-type=module <<'JS'
import {readFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
const origin=new URL(process.env.QUIZPK_ORIGIN);
if(origin.protocol!=='https:' || origin.pathname!=='/' || origin.search || origin.hash || origin.username || origin.password)throw Error('Use an HTTPS origin');
const tokens=[process.env.QUIZPK_OWNER_TOKEN_FILE,process.env.QUIZPK_PLAYER_TOKEN_FILE]
  .map(path=>readFileSync(path,'utf8').trim());
let roomId;
async function api(who,path,type,payload={}) {
  const headers={Authorization:'Bearer '+tokens[who]};
  const options={headers,signal:AbortSignal.timeout(15000)};
  if(type) {
    const clock=await api(who,'/api/rooms/current');
    options.method='POST';headers['Content-Type']='application/json';
    options.body=JSON.stringify({protocolVersion:1,requestId:randomUUID(),issuedAtMs:clock.serverTimeMs,type,payload});
  }
  const response=await fetch(new URL(path,origin),options);
  if(!response.ok)throw Error('Request failed');
  return await response.json();
}
try {
  const created=await api(0,'/api/rooms','room.create',{mode:'MODERATOR',displayName:'Host',settings:{category:'All Categories',difficulty:'Easy',questionCount:5,timerSeconds:20}});
  roomId=created.result.roomId;
  await api(1,'/api/rooms/join','room.join',{code:created.result.inviteCode,displayName:'Player'});
  const snapshot=await api(1,'/api/rooms/'+roomId);
  console.log(JSON.stringify({twoIdentitiesJoined:snapshot.room.members.length===2,moderatorSeparate:snapshot.room.members.filter(m=>m.role==='CONTESTANT').length===1}));
} catch {console.error('Room smoke failed. No credentials or payloads printed.');process.exitCode=1;}
finally {
  if(roomId)try {await api(0,'/api/rooms/'+roomId+'/leave','room.leave');}
  catch {console.error('Cleanup pending: owner should leave the test room, or let its two-hour TTL expire.');process.exitCode=1;}
}
JS
```

No real-origin smoke was run here. Two identities in this script are simulated
clients, **not two physical Android phones**. Fetch transport failures or an
unreceived creation response can leave a room until its TTL; recover through the
authenticated current-room endpoint. Do not print private response objects.

## Physical acceptance deferred to the client stages

After M4.2, install its same-signature APK **as an update**, on two actual phones.
Use distinct individual tokens; test Classic and Moderator lobbies, capacity,
readiness, settings conflicts, background/reconnect, revoked access, full/expired
codes and both themes. Test different internet networks as well as shared Wi-Fi.
Rounds/results/moderator control require their later stages. M4.1 cannot certify
phone lobby UX because those screens are not implemented.

After M4.8–M4.10, test the embedded Android host with router internet disconnected
and cellular data off, then with a password-protected hotspot and mobile data off.
Test manual LAN IP:port when NSD fails, and a separate host phone if tethering
hardware prevents hosting. LOCAL must use ephemeral local sessions, validated
saved questions and no Termux/cloud/Gemini; host death must fail visibly. A hotspot
sharing mobile internet only proves Online connectivity. Node/Java equivalence
and at least moderator + two physical contestants remain future acceptance gates.

**Stop here. M4.2 requires user review and explicit authorization.**
# M4.3 setup and review gate

M4.2 passed user-reported physical two-device acceptance, including the refresh
correction. M4.3 implements Online Classic only. Use the signed version 6 APK
`artifacts/QuizPK-M4.3-classic-debug.apk` as an update after build validation; keep
the same package/signer and storage. Individual HTTPS login is unchanged.

The updated APK retains ordinary lobbies on the older backend; Classic Start
requires a separately reviewed M4.3 deployment. Server schema ledger becomes 2,
Android SQLite stays v3. Deploy all of `backend/src/`, including the validated
Classic bank JSON, with the existing package/config/token/DB paths and one Node 24
process. No Gemini provider request supplies match questions.

Review [Classic rules/security](M4_3_CLASSIC.md),
[deployment/fallback](M4_3_DEPLOYMENT_PLAN.md),
[phone acceptance](M4_3_DEVICE_TEST_PLAN.md) and [test evidence](TEST_NOTES.md).
The old M4.2 deployment receipts/launchers do not authorize schema 2. No live
deployment/start/migration or installed-app update is performed by implementation.
Moderator gameplay, LAN, authoring and monetization remain deferred.

Earlier setup instructions follow as historical stage records.

---

