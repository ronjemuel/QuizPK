# M3 setup and device handoff

M3 source implementation is available; **acceptance is pending** a real configured
HTTPS backend and physical-phone validation. Mock tests are not live Gemini tests.
No paid API billing/hosting plan has been enabled.

Delivery: [QuizPK-M3-ai-debug.apk](../artifacts/QuizPK-M3-ai-debug.apk). Clean build,
56 Android tests and 20 backend mock tests passed; lint has 0 errors/39 warnings.
The verified APK signature matches M2. Install as an update to retain existing data.

- Existing server-only credential: `GEMINI_API_KEY/GEMINI_API_KEY.env`.
  Format validated, loader checked through boolean status only; no value displayed.
- Recoverable source backup: `../QuizPK_Checkpoints/QuizPK-M2-approved-before-M3.tar.gz`.
  Extract into a separate empty folder to review/restore; signing and secret files
  were excluded and remain unchanged in the workspace.
- Backend architecture: Node.js 24 + durable SQLite, one process behind Caddy HTTPS.
  Individual random user tokens, hashed allowlist, quotas and idempotency are enforced.
- Recommended current model: `gemini-3.5-flash-lite`, official docs checked 2026-10-12.
  `GEMINI_MODEL` remains a blank template setting until the operator configures it.
- Exact deployment/private configuration: [backend README](../backend/README.md).
- Android configuration: **AI Quiz → Connect to QuizPK**, real HTTPS origin plus a
  provisioned personal access token. No APK rebuild is needed to configure the host.
  Never put the provider API key in this dialog; it accepts only individual tokens.
- Rotate the key previously shared in chat before public release and replace it in
  every deployed environment. Keep Gemini on an operator-confirmed free-tier project;
  consumer Gemini subscriptions do not supply API quota.

After setup, install the M3 APK as an update without uninstalling M2. The device
checklist and actual automated/build results are in [TEST_NOTES](TEST_NOTES.md).
Saved quizzes, completed history and resumable gameplay use local SQLite only.
Generated questions need review; local question reports are not transmitted.
