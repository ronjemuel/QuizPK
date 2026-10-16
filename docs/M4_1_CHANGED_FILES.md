# M4.1 changed files

Compared with `../QuizPK_Checkpoints/QuizPK-M3-source-before-M4.1.tar.gz`.
The replacement Online + Local brief is user-supplied, not an implementation
change. Secret/signing files, private DBs, builds and validation outputs are
excluded. No Git repository exists. No Android source/resource/build change.

## Scope and rationale

- `store.mjs`: additive multiplayer migration hook; existing M3 methods unchanged.
- `server.mjs`: authenticated room dispatch and protocol-specific errors; M3 generation unchanged.
- `multiplayer/`: isolated schema ledger, strict commands, transactional lobby authority, HTTP adapter.
- `check-room-db.mjs`: operator-only read-only identity/schema check, boolean output.
- Room/protocol tests and JSON fixtures: real loopback integration plus future-rule design vectors.
- `.gitignore`: exclude access-token files wherever they are placed.
- Docs/tools: architecture, contract, private setup/recovery, inventory and preservation evidence.

## SQLite migration

Adds mp_schema version 1, mp_rooms, mp_members, mp_requests, mp_limits and indexes.
Does not alter/drop users/jobs/throttle or change their contents/user_version.
Android SQLite remains v3. Migration/reopen of M3 fixtures,
auth/provisioning and on-disk restart are tested; actual Termux identities checked
read-only with both existing tokens, with durable DB/WAL unchanged and M3 schema retained.

## Modified (8)

- [.gitignore](../.gitignore)
- [IMPLEMENTATION_LOG.md](../IMPLEMENTATION_LOG.md)
- [PLAN.md](../PLAN.md)
- [README.md](../README.md)
- [backend/README.md](../backend/README.md)
- [backend/src/server.mjs](../backend/src/server.mjs)
- [backend/src/store.mjs](../backend/src/store.mjs)
- [docs/TEST_NOTES.md](../docs/TEST_NOTES.md)

## Added (15)

- [backend/src/check-room-db.mjs](../backend/src/check-room-db.mjs)
- [backend/src/multiplayer/http.mjs](../backend/src/multiplayer/http.mjs)
- [backend/src/multiplayer/migrations.mjs](../backend/src/multiplayer/migrations.mjs)
- [backend/src/multiplayer/protocol.mjs](../backend/src/multiplayer/protocol.mjs)
- [backend/src/multiplayer/rooms.mjs](../backend/src/multiplayer/rooms.mjs)
- [backend/test/protocol-reference.mjs](../backend/test/protocol-reference.mjs)
- [backend/test/protocol.test.mjs](../backend/test/protocol.test.mjs)
- [backend/test/rooms.test.mjs](../backend/test/rooms.test.mjs)
- [docs/M4_ARCHITECTURE.md](../docs/M4_ARCHITECTURE.md)
- [docs/M4_CHANGED_FILES.md](../docs/M4_CHANGED_FILES.md)
- [docs/M4_PROTOCOL.md](../docs/M4_PROTOCOL.md)
- [docs/M4_SETUP.md](../docs/M4_SETUP.md)
- [protocol/multiplayer-v1.json](../protocol/multiplayer-v1.json)
- [tools/m4_inventory.py](../tools/m4_inventory.py)
- [tools/verify_m4_security.py](../tools/verify_m4_security.py)

## Removed (0)


## Generated evidence and retained delivery

- `docs/validation/m4.1/`: restored M3 tests, all backend/Android regressions,
  first failed error-shape check and corrected passes, test XML, signature,
  checkpoint, read-only DB status, preservation/security and inventory evidence.
- `artifacts/QuizPK-M3-ai-debug.apk`: unchanged M3 delivery; no M4 APK/UI exists yet.
- No live server/tunnel/provider call or durable private DB modification was performed.
- M4.2 UI, actual rounds, authoring/results/history and embedded LAN runtime are deferred.
- Stop for review under the replacement brief before M4.2.
