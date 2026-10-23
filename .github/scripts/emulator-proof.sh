#!/usr/bin/env bash
set -u
result=0
if [ "${1:-false}" = "true" ]; then
  ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveEdge=true || result=$?
else
  ./gradlew :app:connectedDebugAndroidTest || result=$?
fi
mkdir -p app/build/emulator-screenshots
for mode in light dark; do
  adb shell "run-as de.edgespeak.tts cat files/test-output/setup-$mode.png > /data/local/tmp/setup-$mode.png" && adb pull "/data/local/tmp/setup-$mode.png" app/build/emulator-screenshots/ || result=1
  adb shell "rm -f /data/local/tmp/setup-$mode.png"
done
exit "$result"
