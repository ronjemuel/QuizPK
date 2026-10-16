# M3 changed files

Compared with `../QuizPK_Checkpoints/QuizPK-M2-approved-before-M3.tar.gz`.
Credential files, signing files, generated builds and retained validation outputs
are excluded. MainActivity startup fix, themes, brand assets, bank and reference
images match the checkpoint. No multiplayer implementation.

## Modified (20)

- [.gitignore](../.gitignore)
- [IMPLEMENTATION_LOG.md](../IMPLEMENTATION_LOG.md)
- [PLAN.md](../PLAN.md)
- [README.md](../README.md)
- [app/build.gradle.kts](../app/build.gradle.kts)
- [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml)
- [app/src/main/java/com/zeke/quizpk/OfflineQuizActivity.java](../app/src/main/java/com/zeke/quizpk/OfflineQuizActivity.java)
- [app/src/main/java/com/zeke/quizpk/data/local/QuizDatabase.java](../app/src/main/java/com/zeke/quizpk/data/local/QuizDatabase.java)
- [app/src/main/java/com/zeke/quizpk/domain/QuizConfig.java](../app/src/main/java/com/zeke/quizpk/domain/QuizConfig.java)
- [app/src/main/java/com/zeke/quizpk/ui/screens/AiQuizFragment.java](../app/src/main/java/com/zeke/quizpk/ui/screens/AiQuizFragment.java)
- [app/src/main/java/com/zeke/quizpk/ui/screens/ResultsFragment.java](../app/src/main/java/com/zeke/quizpk/ui/screens/ResultsFragment.java)
- [app/src/main/java/com/zeke/quizpk/ui/screens/ReviewFragment.java](../app/src/main/java/com/zeke/quizpk/ui/screens/ReviewFragment.java)
- [app/src/main/res/layout/fragment_ai_quiz.xml](../app/src/main/res/layout/fragment_ai_quiz.xml)
- [app/src/main/res/values/strings.xml](../app/src/main/res/values/strings.xml)
- [app/src/main/res/xml/backup_rules.xml](../app/src/main/res/xml/backup_rules.xml)
- [app/src/main/res/xml/data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml)
- [app/src/test/java/com/zeke/quizpk/OfflineDataTest.java](../app/src/test/java/com/zeke/quizpk/OfflineDataTest.java)
- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)
- [docs/REFERENCE_AUDIT.md](../docs/REFERENCE_AUDIT.md)
- [docs/TEST_NOTES.md](../docs/TEST_NOTES.md)

## Added (30)

- [app/src/main/java/com/zeke/quizpk/data/remote/AiBackendClient.java](../app/src/main/java/com/zeke/quizpk/data/remote/AiBackendClient.java)
- [app/src/main/java/com/zeke/quizpk/data/remote/BackendCredentials.java](../app/src/main/java/com/zeke/quizpk/data/remote/BackendCredentials.java)
- [app/src/main/java/com/zeke/quizpk/domain/AiQuiz.java](../app/src/main/java/com/zeke/quizpk/domain/AiQuiz.java)
- [app/src/main/java/com/zeke/quizpk/domain/AiQuizRequest.java](../app/src/main/java/com/zeke/quizpk/domain/AiQuizRequest.java)
- [app/src/main/java/com/zeke/quizpk/domain/SavedAiQuiz.java](../app/src/main/java/com/zeke/quizpk/domain/SavedAiQuiz.java)
- [app/src/main/java/com/zeke/quizpk/ui/screens/AiQuizViewModel.java](../app/src/main/java/com/zeke/quizpk/ui/screens/AiQuizViewModel.java)
- [app/src/test/java/com/zeke/quizpk/AiDataTest.java](../app/src/test/java/com/zeke/quizpk/AiDataTest.java)
- [app/src/test/java/com/zeke/quizpk/AiFixtures.java](../app/src/test/java/com/zeke/quizpk/AiFixtures.java)
- [app/src/test/java/com/zeke/quizpk/AiFlowTest.java](../app/src/test/java/com/zeke/quizpk/AiFlowTest.java)
- [backend/.dockerignore](../backend/.dockerignore)
- [backend/.env.example](../backend/.env.example)
- [backend/Caddyfile.example](../backend/Caddyfile.example)
- [backend/README.md](../backend/README.md)
- [backend/package.json](../backend/package.json)
- [backend/quizpk.service.example](../backend/quizpk.service.example)
- [backend/src/check-config.mjs](../backend/src/check-config.mjs)
- [backend/src/config.mjs](../backend/src/config.mjs)
- [backend/src/contract.mjs](../backend/src/contract.mjs)
- [backend/src/create-user.mjs](../backend/src/create-user.mjs)
- [backend/src/gemini.mjs](../backend/src/gemini.mjs)
- [backend/src/revoke-user.mjs](../backend/src/revoke-user.mjs)
- [backend/src/server.mjs](../backend/src/server.mjs)
- [backend/src/smoke.mjs](../backend/src/smoke.mjs)
- [backend/src/store.mjs](../backend/src/store.mjs)
- [backend/test/api.test.mjs](../backend/test/api.test.mjs)
- [backend/test/persistence.test.mjs](../backend/test/persistence.test.mjs)
- [docs/M3_CHANGED_FILES.md](../docs/M3_CHANGED_FILES.md)
- [docs/M3_SETUP.md](../docs/M3_SETUP.md)
- [tools/m3_inventory.py](../tools/m3_inventory.py)
- [tools/verify_m3_security.py](../tools/verify_m3_security.py)

## Removed (0)


## Generated artifacts and evidence

- `artifacts/QuizPK-M3-ai-debug.apk`: delivery artifact; M1/M2 APKs retained.
- `docs/validation/m3/`: actual clean/targeted Gradle logs, first failed checks,
  final test XML, lint reports, backend mock tests, signing/metadata/hash,
  security and preservation audit, seed audit and empty ADB listing.

No Gemini secret was copied to source, APKs, archives, reports or chat.
No actual production URL or paid plan was configured. Real HTTPS/backend/phone
acceptance remains pending; see TEST_NOTES and backend/README.
