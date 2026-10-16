# Current milestone: M4.3

See [M4.3 source inventory](M4_3_CHANGED_FILES.md), compared against the approved
222-file M4.2 checkpoint. The retained M4.2 inventory below is historical.

---

# M4.2 changed files

Compared with the approved pre-edit M4.1 checkpoint:
`../QuizPK_Checkpoints/QuizPK-M4.1-approved-before-M4.2.tar.gz` (192 files).
No Git repository exists. Private files, signing, databases, generated builds,
APKs and validation reports are excluded from this source inventory.
The [M4.1 inventory](M4_1_CHANGED_FILES.md) is retained as history.

## Scope and rationale

- `MultiplayerFragment` and native XML: network choice, Online create/join,
  separate nonplaying Moderator, eight-contestant roster, ready/settings,
  invite copy/share, connection/errors and confirmed leave/close.
- `LobbyViewModel`: serial lifecycle-aware polling/heartbeat, authenticated
  recovery, exact-envelope explicit uncertain retries and credential isolation.
- `RoomProtocol` and `RoomBackendClient`: strict common-v1 lobby projection
  and bounded HTTPS individual-token adapter. No gameplay or LAN engine.
- `app/build.gradle.kts`: version 4 / 1.3-M4.2, canonical protocol as test-only
  resource. Existing dependencies, debug signer and SDK requirements unchanged.
- Lobby tests and isolated Node fixture: protocol, native UI/lifecycle/errors
  and actual Java-to-M4.1 Node/SQLite integration. No real provider or live DB.
- Docs/tools: deployment plan (not executed), two-device acceptance plan,
  checkpoint/preservation/security verification and reproducible inventory.

## Preservation and migration

No production backend/protocol change, new SQL migration, Android permission
or dependency was introduced. Android SQLite remains v3. M1 startup/navigation,
themes/brand, M2 bank/history/drafts, M3 AI/saved quiz/credentials implementations
and their original tests are byte-preserved apart from the authorized multiplayer
placeholder replacement and app version/test-resource configuration.
The live Termux source, private env/tokens and durable DB/WAL were not edited.
The M3 checkpoint and M3 APK remain unchanged. See TEST_NOTES for audit evidence.

## Modified (11)

- [IMPLEMENTATION_LOG.md](../IMPLEMENTATION_LOG.md)
- [PLAN.md](../PLAN.md)
- [README.md](../README.md)
- [app/build.gradle.kts](../app/build.gradle.kts)
- [app/src/main/java/com/zeke/quizpk/ui/screens/MultiplayerFragment.java](../app/src/main/java/com/zeke/quizpk/ui/screens/MultiplayerFragment.java)
- [app/src/main/res/layout/fragment_multiplayer.xml](../app/src/main/res/layout/fragment_multiplayer.xml)
- [docs/M4_ARCHITECTURE.md](../docs/M4_ARCHITECTURE.md)
- [docs/M4_CHANGED_FILES.md](../docs/M4_CHANGED_FILES.md)
- [docs/M4_PROTOCOL.md](../docs/M4_PROTOCOL.md)
- [docs/M4_SETUP.md](../docs/M4_SETUP.md)
- [docs/TEST_NOTES.md](../docs/TEST_NOTES.md)

## Added (16)

- [app/src/main/java/com/zeke/quizpk/data/remote/RoomBackendClient.java](../app/src/main/java/com/zeke/quizpk/data/remote/RoomBackendClient.java)
- [app/src/main/java/com/zeke/quizpk/multiplayer/RoomProtocol.java](../app/src/main/java/com/zeke/quizpk/multiplayer/RoomProtocol.java)
- [app/src/main/java/com/zeke/quizpk/ui/screens/LobbyViewModel.java](../app/src/main/java/com/zeke/quizpk/ui/screens/LobbyViewModel.java)
- [app/src/main/res/layout/item_lobby_member.xml](../app/src/main/res/layout/item_lobby_member.xml)
- [app/src/main/res/layout/lobby_settings_fields.xml](../app/src/main/res/layout/lobby_settings_fields.xml)
- [app/src/main/res/values/multiplayer_strings.xml](../app/src/main/res/values/multiplayer_strings.xml)
- [app/src/test/java/com/zeke/quizpk/LobbyBackendIntegrationTest.java](../app/src/test/java/com/zeke/quizpk/LobbyBackendIntegrationTest.java)
- [app/src/test/java/com/zeke/quizpk/LobbyFixtures.java](../app/src/test/java/com/zeke/quizpk/LobbyFixtures.java)
- [app/src/test/java/com/zeke/quizpk/LobbyFlowTest.java](../app/src/test/java/com/zeke/quizpk/LobbyFlowTest.java)
- [app/src/test/java/com/zeke/quizpk/LobbyProtocolTest.java](../app/src/test/java/com/zeke/quizpk/LobbyProtocolTest.java)
- [backend/test/android-lobby-server.mjs](../backend/test/android-lobby-server.mjs)
- [docs/M4_1_CHANGED_FILES.md](../docs/M4_1_CHANGED_FILES.md)
- [docs/M4_2_DEPLOYMENT_PLAN.md](../docs/M4_2_DEPLOYMENT_PLAN.md)
- [docs/M4_2_DEVICE_TEST_PLAN.md](../docs/M4_2_DEVICE_TEST_PLAN.md)
- [tools/m42_inventory.py](../tools/m42_inventory.py)
- [tools/verify_m42_security.py](../tools/verify_m42_security.py)

## Removed (0)


## Generated delivery and evidence

- `artifacts/QuizPK-M4.2-online-lobby-debug.apk`: same-signer Android update.
- `docs/validation/m4.2/`: checkpoint, backend/full Android logs, test XML,
  lint, APK metadata/signature/hash, preservation and source inventory evidence.
- Physical two-device acceptance remains pending; no attached ADB device.
- Live matches, question authoring, rankings and LAN runtime remain deferred.
- **STOP for M4.2 review.** Live backend deployment needs separate approval.
