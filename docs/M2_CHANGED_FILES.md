# M2 changed files

Compared by content with `../QuizPK_Checkpoints/QuizPK-M1-approved-before-M2.tar.gz`.
Generated build/cache outputs and private signing files are excluded from this
inventory. Existing signing configuration, theme preference implementation, color/
theme/style resources, QuizPK brand vectors and all 19 reference PNGs are unchanged.

## Modified (18)

- [`IMPLEMENTATION_LOG.md`](../IMPLEMENTATION_LOG.md)
- [`PLAN.md`](../PLAN.md)
- [`README.md`](../README.md)
- [`app/build.gradle.kts`](../app/build.gradle.kts)
- [`app/src/main/AndroidManifest.xml`](../app/src/main/AndroidManifest.xml)
- [`app/src/main/java/com/zeke/quizpk/MainActivity.java`](../app/src/main/java/com/zeke/quizpk/MainActivity.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/ExploreFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/ExploreFragment.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/HomeFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/HomeFragment.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/ProfileFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/ProfileFragment.java)
- [`app/src/main/res/layout/component_category.xml`](../app/src/main/res/layout/component_category.xml)
- [`app/src/main/res/layout/fragment_explore.xml`](../app/src/main/res/layout/fragment_explore.xml)
- [`app/src/main/res/layout/fragment_home.xml`](../app/src/main/res/layout/fragment_home.xml)
- [`app/src/main/res/layout/fragment_profile.xml`](../app/src/main/res/layout/fragment_profile.xml)
- [`app/src/main/res/values/strings.xml`](../app/src/main/res/values/strings.xml)
- [`app/src/test/java/com/zeke/quizpk/StartupTest.java`](../app/src/test/java/com/zeke/quizpk/StartupTest.java)
- [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md)
- [`docs/QUESTION_ATTRIBUTION.md`](../docs/QUESTION_ATTRIBUTION.md)
- [`docs/TEST_NOTES.md`](../docs/TEST_NOTES.md)

## Added (25)

- [`app/src/main/assets/questions.json`](../app/src/main/assets/questions.json)
- [`app/src/main/java/com/zeke/quizpk/OfflineQuizActivity.java`](../app/src/main/java/com/zeke/quizpk/OfflineQuizActivity.java)
- [`app/src/main/java/com/zeke/quizpk/data/local/QuestionRepository.java`](../app/src/main/java/com/zeke/quizpk/data/local/QuestionRepository.java)
- [`app/src/main/java/com/zeke/quizpk/data/local/QuizDatabase.java`](../app/src/main/java/com/zeke/quizpk/data/local/QuizDatabase.java)
- [`app/src/main/java/com/zeke/quizpk/domain/Question.java`](../app/src/main/java/com/zeke/quizpk/domain/Question.java)
- [`app/src/main/java/com/zeke/quizpk/domain/QuizConfig.java`](../app/src/main/java/com/zeke/quizpk/domain/QuizConfig.java)
- [`app/src/main/java/com/zeke/quizpk/domain/QuizScoring.java`](../app/src/main/java/com/zeke/quizpk/domain/QuizScoring.java)
- [`app/src/main/java/com/zeke/quizpk/domain/QuizSession.java`](../app/src/main/java/com/zeke/quizpk/domain/QuizSession.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/GameplayFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/GameplayFragment.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/QuizConfigFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/QuizConfigFragment.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/ResultsFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/ResultsFragment.java)
- [`app/src/main/java/com/zeke/quizpk/ui/screens/ReviewFragment.java`](../app/src/main/java/com/zeke/quizpk/ui/screens/ReviewFragment.java)
- [`app/src/main/res/drawable/bg_answer.xml`](../app/src/main/res/drawable/bg_answer.xml)
- [`app/src/main/res/drawable/ic_difficulty.xml`](../app/src/main/res/drawable/ic_difficulty.xml)
- [`app/src/main/res/layout/activity_offline.xml`](../app/src/main/res/layout/activity_offline.xml)
- [`app/src/main/res/layout/fragment_gameplay.xml`](../app/src/main/res/layout/fragment_gameplay.xml)
- [`app/src/main/res/layout/fragment_quiz_config.xml`](../app/src/main/res/layout/fragment_quiz_config.xml)
- [`app/src/main/res/layout/fragment_results.xml`](../app/src/main/res/layout/fragment_results.xml)
- [`app/src/main/res/layout/fragment_review.xml`](../app/src/main/res/layout/fragment_review.xml)
- [`app/src/test/java/com/zeke/quizpk/HostSqliteSupport.java`](../app/src/test/java/com/zeke/quizpk/HostSqliteSupport.java)
- [`app/src/test/java/com/zeke/quizpk/OfflineDataTest.java`](../app/src/test/java/com/zeke/quizpk/OfflineDataTest.java)
- [`app/src/test/java/com/zeke/quizpk/OfflineFlowTest.java`](../app/src/test/java/com/zeke/quizpk/OfflineFlowTest.java)
- [`docs/M2_CHANGED_FILES.md`](../docs/M2_CHANGED_FILES.md)
- [`tools/prepare_arm_sqlite_tests.sh`](../tools/prepare_arm_sqlite_tests.sh)
- [`tools/validate_question_bank.py`](../tools/validate_question_bank.py)

## Delivery and retained evidence

- `artifacts/QuizPK-M2-offline-debug.apk`: rebuilt debug APK; M1 artifact retained.
- `docs/validation/m2/`: final clean Gradle log, test XML, full lint reports,
  APK metadata/signature/hash, M1 resource preservation, question-bank audit,
  factual-source access results/review notes and earlier failed/green attempts.

The StartupTest change enables the optional real-SQLite ARM64 test loader; its
seven existing startup/navigation/theme regression cases remain intact.
App build changes increment debug version to `1.1-M2` / code 2 and pass an
optional test-only JNI path. No production dependency or signing change was made.
MainActivity adds the results-to-Home intent handler while retaining the P0 fix.
Multiplayer/remote code and private-room/separate-moderator planning remain reserved.
No AI generation, live rooms, matchmaking, ads or payment implementation was added.
