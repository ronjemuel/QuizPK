# QuizPK

QuizPK is a native Android quiz application with offline play, an optional AI quiz backend, and online multiplayer work in progress. The project includes Classic Match and Moderator Mode lobby concepts from the existing portfolio placeholder.

## Technologies

Java, Android XML Views, AndroidX, Material Components, SQLite, and Gradle Kotlin DSL. The included backend uses Node.js 24 and SQLite, with a server-side Gemini integration.

## Implemented features and status

- Offline quizzes with category, difficulty, length, timer, feedback, results, history, and resumable progress
- Bundled question data and local quiz persistence
- AI quiz requests through an authenticated HTTPS backend; provider credentials remain on the server
- Online room/lobby flows and a Classic Match review implementation with shared questions, deadlines, scoring, and reconnect handling

The local source identifies its latest Android build as M4.3 (`1.4-M4.3`). Classic Match phone acceptance is a separate validation step. Moderator gameplay and LAN runtime remain deferred. Uploading this source does not deploy or change the running backend.

## Android build

Use Android Studio or Code on the Go with a JDK and SDK compatible with the included Gradle configuration. The project compiles against Android API 36 and supports API 24 or newer. Configure your SDK locally, then run from the repository root:

```sh
bash gradlew :app:assembleDebug
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. Signing material is intentionally excluded; a local debug build uses its own debug identity. Existing installed versions may use a different certificate, so plan any installation and data migration separately.

## Backend setup and checks

The backend is a separate service under `backend/`; it is not an Android Gradle module. With Node.js 24:

```sh
cd backend
npm test
npm run check-config
```

The configuration check reports readiness without printing secret values. Use `backend/.env.example` and `config/backend.properties.example` as references for your own deployment. Supply provider credentials through private server configuration and use HTTPS for Android connections. See `backend/README.md` and the M4.3 contracts in `docs/` for the existing operator procedures and their historical validation limits. Do not treat an old free-tier or model availability note as a current provider guarantee.

## Structure

- `app/`: Android application and existing tests
- `backend/`: server source, fixtures, and tests
- `protocol/`: shared multiplayer contract
- `docs/` and `tools/`: development documentation and existing checks

No generated APKs, live databases, owner tokens, provider keys, or signing files are included.

## Repository preparation

This repository contains the existing local implementation. Application source and build logic are preserved. Generated output, signing keys, credentials, and local service configuration are excluded. Earlier development reports describe their original checks; repository preparation did not repeat an application build or device test.
