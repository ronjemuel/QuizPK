#!/usr/bin/env bash
# Optional Termux/PRoot ARM64 host only. Nothing here enters the application APK.
# Requires git, swig, Termux clang, sqlite development files and the host JDK.
set -euo pipefail
quizpk_native_work="${1:-/tmp/quizpk-sqlite4java-test-source}"
quizpk_native_out="${2:-/data/data/com.termux/files/usr/tmp/quizpk-sqlite-host}"
quizpk_jdk="${QUIZPK_TEST_JDK:-/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk}"
quizpk_termux="${QUIZPK_TEST_PREFIX:-/data/data/com.termux/files/usr}"
if [ ! -d "$quizpk_native_work/.git" ]; then
  git clone https://github.com/GrapheneOS/sqlite4java.git "$quizpk_native_work"
fi
git -C "$quizpk_native_work" checkout --detach 789dab5f1d816bfe7cc01963ed157a637cf00fe9
mkdir -p "$quizpk_native_out"
swig -java -package com.almworks.sqlite4java -outdir "$quizpk_native_out" \
  -o "$quizpk_native_out/sqlite_wrap.c" "$quizpk_native_work/swig/sqlite.i"
"$quizpk_termux/bin/clang" -shared -fPIC -O2 -fno-strict-aliasing \
  -I"$quizpk_termux/include" -I"$quizpk_jdk/include" -I"$quizpk_jdk/include/linux" \
  -I"$quizpk_native_work/native" "$quizpk_native_out/sqlite_wrap.c" \
  "$quizpk_native_work/native/sqlite3_wrap_manual.c" "$quizpk_native_work/native/intarray.c" \
  -L"$quizpk_termux/lib" -lsqlite3 -o "$quizpk_native_out/libsqlite4java.so"
printf 'Use Gradle property: -PquizpkTestSqliteDir=%s\n' "$quizpk_native_out"
