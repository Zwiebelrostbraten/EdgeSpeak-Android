# Third-party notices

EdgeSpeak is licensed under LGPL-3.0-or-later. Copyright 2026 EdgeSpeak contributors.
See COPYING and COPYING.GPL. Corresponding source is this repository; retain license
and notices when distributing modifications. No Microsoft affiliation or trademark license.

The GEC v2 algorithm in `core/.../Gec.kt` is a Kotlin adaptation of
[rany2/edge-tts](https://github.com/rany2/edge-tts), `src/edge_tts/drm.py`
(reference fetched 2026-10-23), Copyright rany2 and contributors, LGPL-3.0.
Protocol constants and message structure were researched from its constants.py and
communicate.py. Python is neither included nor required at runtime.

Runtime dependencies: OkHttp 4.12.0 and Okio (Square, Apache-2.0), Kotlin standard
library (JetBrains, Apache-2.0). Build/test: Android Gradle Plugin/AndroidX (Apache-2.0),
Gradle wrapper (Apache-2.0), JUnit 4 (EPL-1.0). Android platform APIs supplied by device.
Dependency sources and licenses: https://github.com/square/okhttp,
https://github.com/square/okio, https://github.com/JetBrains/kotlin,
https://github.com/junit-team/junit4, https://github.com/gradle/gradle.
The instrumentation tone fixture was generated locally with FFmpeg's sine source;
it contains no speech, music, user text, or third-party recording.

Corresponding release source: https://github.com/Zwiebelrostbraten/EdgeSpeak-Android/tree/v0.1.0
Unpublished initial 0.1.0 source; this tag link is the intended release source location.
EdgeSpeak modifications/adaptations: Kotlin GEC port, bounded WebSocket/SSML client,
Android system TTS service, PCM decoding, consent/cancellation, setup UI and tests.
OkHttp/Okio Copyright Square, Inc.; Kotlin Copyright JetBrains s.r.o. and contributors.
Their Apache-2.0 license is bundled as licenses/Apache-2.0.txt.
