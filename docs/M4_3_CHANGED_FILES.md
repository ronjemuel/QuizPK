# M4.3 source inventory

Compared with the approved 222-file M4.2 checkpoint (SHA-256
`c9f6c5c527f13603fb8e3905c0e8bdbce6ff43338316da954734190af7b3b5e0`).
There is no Git repository. No original public source file was removed.
Private configuration, signing, databases, build outputs, APKs and validation
reports are excluded. Machine-readable final inventory is in
`validation/m4.3/source-changes.json`.

The changes add Classic authority, additive schema 2, strict opt-in DTOs, native
match rendering and automated coverage. Existing lobby tests only change the
formerly unsupported Classic capability expectation. Existing backend tests update
the migration fixture and exercise the production scoring vectors. Operator tests
mock their socket probe so synthetic maintenance tests cannot contact a real
listener; production maintenance guards are unchanged. M1–M3 implementation,
Android saved-data schema, credentials, signing and dependencies remain intact.

## Modified baseline files

- `IMPLEMENTATION_LOG.md`
- `README.md`
- `app/build.gradle.kts`
- `app/src/main/java/com/zeke/quizpk/data/remote/RoomBackendClient.java`
- `app/src/main/java/com/zeke/quizpk/multiplayer/RoomProtocol.java`
- `app/src/main/java/com/zeke/quizpk/ui/screens/LobbyViewModel.java`
- `app/src/main/java/com/zeke/quizpk/ui/screens/MultiplayerFragment.java`
- `app/src/main/res/layout/fragment_multiplayer.xml`
- `app/src/main/res/values/multiplayer_strings.xml`
- `app/src/test/java/com/zeke/quizpk/LobbyProtocolTest.java`
- `backend/README.md`
- `backend/src/multiplayer/http.mjs`
- `backend/src/multiplayer/migrations.mjs`
- `backend/src/multiplayer/protocol.mjs`
- `backend/src/multiplayer/rooms.mjs`
- `backend/src/server.mjs`
- `backend/test/protocol.test.mjs`
- `backend/test/rooms.test.mjs`
- `docs/M4_ARCHITECTURE.md`
- `docs/M4_CHANGED_FILES.md`
- `docs/M4_PROTOCOL.md`
- `docs/M4_SETUP.md`
- `docs/TEST_NOTES.md`
- `protocol/multiplayer-v1.json`
- `tools/test_m42_backend_ops.py`

## Added source and review documents

- `app/src/main/java/com/zeke/quizpk/multiplayer/MatchProtocol.java`
- `app/src/main/java/com/zeke/quizpk/ui/screens/ClassicMatchView.java`
- `app/src/main/res/layout/item_match_player.xml`
- `app/src/main/res/layout/panel_classic_match.xml`
- `app/src/main/res/values/classic_strings.xml`
- `app/src/test/java/com/zeke/quizpk/ClassicBackendIntegrationTest.java`
- `app/src/test/java/com/zeke/quizpk/ClassicFixtures.java`
- `app/src/test/java/com/zeke/quizpk/ClassicFlowTest.java`
- `backend/src/multiplayer/classic-bank.json`
- `backend/src/multiplayer/classic.mjs`
- `backend/test/android-classic-server.mjs`
- `backend/test/classic.test.mjs`
- `backend/test/fixtures/m42-migrations.mjs`
- `docs/M4_3_CHANGED_FILES.md`
- `docs/M4_3_CLASSIC.md`
- `docs/M4_3_DEPLOYMENT_PLAN.md`
- `docs/M4_3_DEVICE_TEST_PLAN.md`
