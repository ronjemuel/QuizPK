# P0: M1 startup crash investigation

The reported TECNO KL7 / Android 14 crash is not available through ADB in this
session. `adb devices -l` lists no target and no emulator is available. The host's
native properties identify TECNO KL7 / Android 14, but direct crash-buffer reading
returned no lines; that does not establish permission to read QuizPK's logs or
prove the app is crash-free. No device FATAL EXCEPTION/Caused by trace was captured.
The user requested that source investigation continue without a device log.

## Confirmed defect and supporting evidence

The original MainActivity was tested with Robolectric 4.14.1 at SDK 34 (Android
14 model). Its cold-start test **failed**, with this actual JVM test stack:

```text
android.content.res.Resources$NotFoundException: Resource ID #0x0
    at android.content.res.ResourcesImpl.getValue(ResourcesImpl.java:234)
    at android.content.res.Resources.getValue(Resources.java:1439)
    at androidx.appcompat.widget.ResourceManagerInternal.createDrawableIfNeeded(ResourceManagerInternal.java:181)
    at androidx.appcompat.widget.ResourceManagerInternal.getDrawable(ResourceManagerInternal.java:146)
    at androidx.appcompat.widget.ResourceManagerInternal.getDrawable(ResourceManagerInternal.java:137)
    at androidx.appcompat.content.res.AppCompatResources.getDrawable(AppCompatResources.java:66)
    at androidx.appcompat.widget.Toolbar.setNavigationIcon(Toolbar.java:1046)
    at com.zeke.quizpk.MainActivity.updateChrome(MainActivity.java:151)
    at com.zeke.quizpk.MainActivity.showDestination(MainActivity.java:130)
    at com.zeke.quizpk.MainActivity.onCreate(MainActivity.java:80)
```

The line numbers above refer to the original M1 activity. This is **not** a TECNO
logcat trace. Complete captured evidence is under `docs/validation/p0-startup/`:
`before-fix-test.xml`, `before-fix-test-build.txt` and `before-fix-stacktrace.txt`.
AppCompat dependency bytecode and the packaged original manifest were also saved.

The original `updateChrome()` passed zero to the integer overload when Home
needed no back icon:

```java
binding.toolbar.setNavigationIcon(settings ? R.drawable.ic_back : 0);
```

AppCompat 1.6.1's integer overload calls `AppCompatResources.getDrawable(context,
resId)` even when `resId` is zero. ResourceManagerInternal then performs a resource
lookup; zero is not a valid drawable ID. This runs synchronously from onCreate,
after Home is created. The second call used the same invalid pattern to remove
the logo when opening Settings:

```java
binding.toolbar.setLogo(settings ? 0 : R.drawable.ic_quizpk);
```

Both are confirmed unsafe calls. The first was reproduced in the SDK-34 JVM
startup test. They explain a post-splash startup failure in the tested model;
without the device trace, do not assert this is the only cause on the TECNO.

## Small production fix

Use the Drawable overloads with `null` to remove an icon, and resolve actual
resource IDs with AppCompatResources. No exception swallowing, fallback generic
screen, manifest changes, splash replacement, new Application class or dependency
upgrade. Existing branded layouts, theme storage and private/moderator design
remain intact.

Only `MainActivity.java` changes production behavior. `app/build.gradle.kts`
adds test-only JUnit/Robolectric dependencies and Android-resource test support;
`app/src/test/java/com/zeke/quizpk/StartupTest.java` supplies regression coverage.
A byte-for-byte comparison of all 820 packaged `res/` entries in the rebuilt
APK against the original M1 APK found no changes, additions or removals.
The test setup follows the resource-enabled workflow in the
[Robolectric documentation](https://robolectric.org/getting-started/), retaining
the available cached 4.14.1 version rather than changing runtime dependencies.

## Other startup checks

- The packaged manifest declares exported MAIN/LAUNCHER
  `com.zeke.quizpk.MainActivity`, matching the Java class/application ID.
- There is no custom Application or custom splash activity/theme. The OS supplies
  the launch splash from the existing branding. OS splash rendering requires a
  device test and is not verified by a JVM activity test.
- The Application theme inherits the existing Material3 DayNight NoActionBar
  hierarchy, with day/night semantic colors and the saved explicit theme choice.
- Home layout includes reusable native cards; app vectors/layouts are preserved.
- AndroidX Startup declares EmojiCompat/ProcessLifecycle initializers; no new
  startup provider was added as part of the fix.
- Full activity creation, Home view inflation, navigation, Settings and persistence
  are exercised by the added tests when those tests pass; see final results in
  `docs/TEST_NOTES.md`. Linking/lint alone do not verify layout inflation.

## Final local verification

The clean build, debug assembly, lint and all seven SDK-34 JVM startup/navigation/
theme tests passed. Final Gradle result: `BUILD SUCCESSFUL in 5m 27s`; lint has
0 errors and 34 warnings. APK signature verification passed. The deliverable is
`artifacts/QuizPK-M1-P0-startup-fix-debug.apk`. Full commands, test scope, checksum
and retained evidence are in `docs/TEST_NOTES.md` and `IMPLEMENTATION_LOG.md`.

## Remaining gate

M2 must not begin until the startup gate is accepted. The new APK still requires
installation on the TECNO KL7 (or an Android 14 emulator), repeated cold/warm/
force-stop launches, Settings/navigation/theme checks and real crash-log review.
A passing JVM test is a regression check, not a device or emulator pass.
