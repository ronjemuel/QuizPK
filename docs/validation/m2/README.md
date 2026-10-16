# M2 retained verification evidence

Current accepted local results: 37 tests passed, zero failures/errors/skips;
clean build passed; lint zero errors and 34 warnings. Device acceptance was not run.

- `clean-build-lint-tests.txt`: final clean command output, successful 4m 46s.
- `final-verification-summary.json`: exact suite totals and lint warning IDs/counts.
- `final-TEST-*.xml`: final SDK-34 JVM test results.
- `final-lint-results-debug.*`: complete txt/HTML/XML/SARIF lint reports.
- `apk-metadata.json`, `apk-signature.txt`, `apk-badging.txt`, `apk-sha256.txt`:
  delivered M2 APK; M1 certificate comparison in `m1-signing-certificate.txt`.
- `bank-audit.txt`, `final-source-url-check.json`, `final-source-access-summary.txt`,
  `source-review-notes.md`: content structure, factual references and access limits.
- `m1-preservation.txt`, `color-contrast.txt`, `build-source-sha256.json`:
  static preservation/contrast/source checks.
- `adb-devices.txt`: empty attached-target list; no physical runtime result.

Earlier attempts are retained rather than hidden: initial host JNI architecture/
namespace failures, real-SQLite standalone probe, first green 34-test suite and new
exit-test timing assertions. `sqlite-host-failed-probe.xml` is a single load probe,
not the full first 26-test failure. `clean-first-dialog-test-failure.txt` records
36/37 passing before processing the queued dialog action; `dialog-test-after-idle.txt`
then records the remaining 1 ms resumed-timer assertion corrected in the final run.
None of these logs is a physical TECNO crash trace.
