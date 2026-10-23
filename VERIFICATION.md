# Verification — 2026-10-23, review blocker fixes

Initial EdgeSpeak 0.1.0 / versionCode 1 remains unpublished and uncommitted.
No git add/commit/reset/clean, push, remote changes, or GitHub API calls performed.
Product changes were made by Codex in this project only; no private text or secrets used.

## Executed checks

Environment sourced from `/opt/data/android-env.sh`: JDK17, Gradle8.13,
AGP8.10.1, Kotlin2.1.21, compile/target36, min26, Build Tools35.0.0.

- `:core:test`: **19 passed, 0 failed/errors/skipped** (10 protocol + 9 review tests).
- Targeted RED runs preceded implementation: missing terminal/timeout/revocation
  seams and transport policy; subsequently input-validation and revocation-completion
  seams. These RED runs failed at Kotlin compilation, not on a device.
  GREEN uses the same helpers that the production service/decoder call.
- Coverage: one error+done before start, repeated completion suppression, fake-clock
  46-unit blocking sink excluded from 30-unit codec budget, finite stall failure,
  repeated codec progress, invalid/oversized/blank/unknown-voice requests,
  revoke blocks PCM and completes error+done, framework stop remains stop,
  302/307 WebSocket handshake rejection with second target requestCount=0.
  Redirect tests inject only localhost fixture requests through the internal transport;
  production endpoint is fixed and has no configurable URL.
- `:app:lintDebug`: **0 errors, 15 warnings** (existing dependency/version,
  exported TTS service and UI/resource warnings).
- `:app:assembleDebug`, `:app:assembleDebugAndroidTest`: passed.
- **9 Android tests compiled, none executed here**: 5 offline engine tests,
  1 screenshot test, 3 opt-in live tests. No installed Kindle required.
- Final APK `apksigner verify --verbose`: passed, v2 signature, one debug signer.
- All four packaged license assets compared byte-for-byte against source assets.

Logs in `/opt/data/profiles/mrs_patmore/cache/scratch/`:
`edgespeak-review-red.log`, `edgespeak-review-red-validation.log`,
`edgespeak-review-red-revoke.log`, `edgespeak-review-final.log`,
`edgespeak-review-live.log`. Build log ends BUILD SUCCESSFUL.

## Actual live Kotlin smoke after client policy change

Executed `:core:run` once using existing fixed public German/English sentences.
Same production EdgeClient/GEC/SSML code as Android; no alternate implementation.

| Sample/chunk | MP3 bytes | elapsed ms | duration s |
|---|---:|---:|---:|
| Katja normal / 0 | 28656 | 1109 | 4.776 |
| Aria +25% / 0 | 20448 | 891 | 3.408 |
| Conrad -10% / 0 | 65664 | 945 | 10.944 |
| Conrad -10% / 1 | 40752 | 888 | 6.792 |

FFprobe confirmed MP3 / 24000 Hz / mono for all four files; FFmpeg decoded
all successfully to signed16 PCM. Audio stays in profile scratch:
`edgespeak-review-live/`; this does not establish Android MediaCodec or Kindle behavior.

## Fixes and bounds

All service-owned validation/network/decoder/output failures complete error(code)
then done() on the synthesis thread, including failures before start. Completion
is guarded against duplication. Callback failure codes abort emission; AOSP STOPPED
(-2) and framework onStop remain cancellation, without a false network error.
Consent revocation has its own cancellation reason, cancels I/O, prevents subsequent
PCM, and completes with ERROR_NOT_INSTALLED_YET rather than a successful truncated
file. Consent is checked in the PCM guard and immediately before success as well
as by the preferences listener. The preview no longer calls framework stop on revoke.

Decoder timeout is 30 seconds without input/output codec progress, excluding
blocking emit/audioAvailable time. A stalled codec still fails. The old 150-second
request wallclock was removed because it charged normal playback. Bounds remain:
20,000 UTF-16 characters/request; 3,000 escaped UTF-8 bytes/chunk; 2 MB MP3/chunk;
16 MB decoded PCM/chunk; connect 10 s, read 20 s, network turn 35 s/chunk.
No speech caps were increased; no retries after partial playback.
Both HTTPS and SSL redirects disabled; TLS and fixed Microsoft origin retained.

Offline license view includes complete COPYING (LGPL), COPYING.GPL and Apache-2.0,
plus dependency attribution, GEC edge-tts provenance, modification scope and intended
source/tag link https://github.com/Zwiebelrostbraten/EdgeSpeak-Android/tree/v0.1.0 .
The tag/source has not been published. CI APK artifact also includes license assets.

## Prepared Android evidence — not executed

`adb devices -l` returned no devices. No emulator, CI, installation, real system TTS
playback or Kindle test was run here. Parent will review/commit before emulator work.
Existing CI emulator script and inputs retained. `emulator=true` builds/runs API29;
`live_speech=true` additionally enables all three `liveEdge=true` tests:
German framework WAV PCM validation, fixed benign English paragraph speak playback
awaiting done for up to 240 s and asserting elapsed >45 s, and synthesizeToFile
consent revocation at onStart (no sleeps), expecting one terminal consent error.
No-consent synthesizeToFile and speak tests require one error and zero audio.
Screenshots and reports are collected by the existing emulator-proof script.
Live tests restore settings and delete test audio. No runtime success is claimed.
Kindle and device-specific Android standard-engine selection remain unverified.

## Files touched in this review

- `core/src/main/kotlin/de/edgespeak/core/Protocol.kt`
- `core/src/main/kotlin/de/edgespeak/core/SynthesisControl.kt` (new)
- `core/src/test/kotlin/de/edgespeak/core/ReviewTest.kt` (new)
- `core/build.gradle.kts` (MockWebServer test dependency)
- `app/src/main/java/de/edgespeak/tts/EdgeTtsService.kt`
- `app/src/main/java/de/edgespeak/tts/PcmDecoder.kt`
- `app/src/main/java/de/edgespeak/tts/MainActivity.kt`
- `app/src/androidTest/java/de/edgespeak/tts/EngineTest.kt`
- `app/src/androidTest/java/de/edgespeak/tts/LiveFrameworkTest.kt`
- `app/src/main/assets/licenses/{COPYING,COPYING.GPL,Apache-2.0.txt,THIRD_PARTY_NOTICES.md}` (new)
- `.github/workflows/android.yml`
- `THIRD_PARTY_NOTICES.md`, `README.md`, `VERIFICATION.md`

## Delivery

Final actual APK: `/opt/data/projects/EdgeSpeak-v0.1.0-debug.apk`
Checksum file: `/opt/data/projects/EdgeSpeak-v0.1.0-debug.apk.sha256`
Supplemental `build/delivery/` copies refreshed to match.

SHA256: `a899bc21f64305b4bda88be5e7ea0abd0d1dafb6546a088c3c43707f4d8ce155`
