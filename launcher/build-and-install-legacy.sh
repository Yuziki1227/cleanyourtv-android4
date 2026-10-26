#!/usr/bin/env bash
# Install the Android 4.0 (API 14+) build of the launcher on a TV over ADB.
#
# The modern build-and-install.sh uses `cmd package compile` / `cmd package set-home-activity` and the
# `settings` command, which do not exist on Android 4.0-6. This script uses only commands those
# releases have: pm install/enable/disable-user, am start and dumpsys.
#
#   TV_HOST=192.168.1.50 ./build-and-install-legacy.sh
#   TV_HOST=192.168.1.50 STOCK_LAUNCHER=com.google.android.tvlauncher ./build-and-install-legacy.sh
#
# STOCK_LAUNCHER is optional. When it is set, the stock launcher is disabled after the new one starts,
# so Home goes straight to CleanYourTV. When it is not set, the new launcher is installed and started
# and the TV will ask which Home to use (or keep the old one) until you choose on the TV.
#
# This is a fork-only path: upstream's launcher requires API 26. See docs/legacy-android-4.md.
set -euo pipefail
cd "$(dirname "$0")"
: "${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 $0}"
TOOLS="$(cd ../tools && pwd)"
PY=$TOOLS/venv/bin/python
[ -x "$PY" ] || PY=$TOOLS/venv/Scripts/python.exe
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }
export MSYS_NO_PATHCONV=1

TV_SDK=$("$PY" -I "$TOOLS/tv.py" 'getprop ro.build.version.sdk' 2>/dev/null | tr -dc '0-9' || true)
if [ -z "$TV_SDK" ]; then
  echo "ERROR: the TV did not answer. Check TV_HOST and the debugging prompt on the TV." >&2
  exit 1
fi
MIN_SDK=$(sed -n 's/^[[:space:]]*minSdk[[:space:]][[:space:]]*\([0-9]\{1,3\}\)[[:space:]]*$/\1/p' app/build.gradle | head -n 1)
if [ -n "$MIN_SDK" ] && [ "$TV_SDK" -lt "$MIN_SDK" ]; then
  echo "ERROR: this TV runs SDK $TV_SDK but the launcher needs minSdk $MIN_SDK." >&2
  exit 1
fi
echo "TV SDK: $TV_SDK (launcher minSdk ${MIN_SDK:-unknown})"

# JDK 17 and an Android SDK, like build-and-install.sh. On Windows set JAVA_HOME and ANDROID_HOME.
if [ -z "${JAVA_HOME:-}" ]; then
  if command -v java >/dev/null 2>&1; then
    JAVA_HOME="$(java -XshowSettings:properties -version 2>&1 | awk '/java.home/ {print $3}')"
  fi
fi
[ -n "${JAVA_HOME:-}" ] || { echo "ERROR: set JAVA_HOME to a JDK 17." >&2; exit 1; }
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"
if [ -z "${ANDROID_HOME:-}" ]; then
  for d in "$HOME/AppData/Local/Android/Sdk" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk" /opt/android-sdk; do
    [ -d "$d" ] && ANDROID_HOME="$d" && break
  done
fi
[ -n "${ANDROID_HOME:-}" ] || { echo "ERROR: set ANDROID_HOME to an Android SDK with platform 34 and build-tools 34." >&2; exit 1; }
export ANDROID_HOME
[ -f local.properties ] || echo "sdk.dir=$(printf '%s' "$ANDROID_HOME" | tr '\\' '/')" > local.properties

./gradlew assembleRelease --console=plain
APK=app/build/outputs/apk/release/app-release.apk
[ -f "$APK" ] || { echo "ERROR: $APK was not built." >&2; exit 1; }
echo "APK: $(du -h "$APK" | cut -f1)"

"$PY" -I ../tools/b64push.py "$APK" /data/local/tmp/tvhome.apk
OUT=$("$PY" -I ../tools/tv.py 'pm install -r /data/local/tmp/tvhome.apk; rm -f /data/local/tmp/tvhome.apk')
echo "$OUT"
case "$OUT" in
  *Success*) ;;
  *) echo "ERROR: pm install failed; the stock launcher was left alone." >&2; exit 1 ;;
esac

# A fresh instance after an update; the start below creates one.
"$PY" -I ../tools/tv.py 'am force-stop com.gathin.tvhome' >/dev/null 2>&1 || true
echo "Starting the launcher..."
"$PY" -I ../tools/tv.py 'am start -a android.intent.action.MAIN -c android.intent.category.HOME -n com.gathin.tvhome/.MainActivity'
sleep 2

if [ -n "${STOCK_LAUNCHER:-}" ] && [ "$STOCK_LAUNCHER" != none ]; then
  echo "Disabling the stock launcher: $STOCK_LAUNCHER"
  if ! "$PY" -I ../tools/tv.py "pm disable-user $STOCK_LAUNCHER"; then
    echo "WARNING: pm disable-user refused; Home will keep the old launcher until you choose one on the TV." >&2
  fi
fi

# Start Home with no component so the TV resolves it now (our launcher, or a chooser).
"$PY" -I ../tools/tv.py 'am start -a android.intent.action.MAIN -c android.intent.category.HOME' >/dev/null 2>&1 || true
sleep 2

FOCUSED=$("$PY" -I ../tools/tv.py 'dumpsys activity' 2>/dev/null \
  | tr -d '\r' | grep -m1 -E 'mFocusedActivity|mResumedActivity' || true)
case "$FOCUSED" in
  *com.gathin.tvhome*)
    echo "Home is running the CleanYourTV launcher."
    echo "If the TV still shows its old home, press Home and choose CleanYourTV once."
    exit 0 ;;
esac

echo "WARNING: the launcher did not come to the front (dumpsys: ${FOCUSED:-nothing})." >&2
if [ -n "${STOCK_LAUNCHER:-}" ] && [ "$STOCK_LAUNCHER" != none ]; then
  echo "Putting the stock launcher back..." >&2
  "$PY" -I ../tools/tv.py "pm enable $STOCK_LAUNCHER" >/dev/null 2>&1 || true
  "$PY" -I ../tools/tv.py 'am start -a android.intent.action.MAIN -c android.intent.category.HOME' >/dev/null 2>&1 || true
fi
echo "Read docs/legacy-android-4.md and the TV's logcat: adb logcat | grep -i tvhome" >&2
exit 1
