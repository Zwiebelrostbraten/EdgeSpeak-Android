# EdgeSpeak für Android

Eine eigenständige Kotlin-TextToSpeechService-Engine für Android 8+ (API 26).
Version 0.1.0, Paket `de.edgespeak.tts`. Online-Sprachausgabe über den
undokumentierten Microsoft Edge Read Aloud Consumer-Dienst, ohne API-Schlüssel.
Unabhängiges Projekt, nicht von Microsoft unterstützt. Der Dienst kann sich
jederzeit ändern oder ausfallen. Kein Offline-Modus und kein Ersatzdienst.

## Installation und Android-Standard

Debug-APK installieren (Installation aus dieser Quelle erlauben), EdgeSpeak öffnen
und die Erklärung zur Textübertragung lesen. Erst nach ausdrücklicher Zustimmung
wird Text an Microsoft übertragen. Die Zustimmung lässt sich jederzeit widerrufen.
Stimme auswählen, Hörprobe testen und „Als Android-Standard auswählen“ öffnen.
In Androids Text-in-Sprache-Einstellungen **EdgeSpeak (Online)** als bevorzugte
Engine wählen. Das erfolgt immer manuell. Menüpfade unterscheiden sich je nach Hersteller.

Die Hörprobe verwendet Android `TextToSpeech` mit explizitem EdgeSpeak-Paket.
Andere Apps können eine Stimme sowie Tempo und Tonhöhe ausdrücklich vorgeben;
diese System-Anfragen haben Vorrang. Die App-Regler gelten für die Hörprobe.
Die gespeicherte Stimme ist die bevorzugte Standardstimme ihrer Sprache.
Deutsch: Katja, Conrad, Amala, Killian; Englisch: Aria, Guy, Sonia.
Alle Stimmen werden als netzwerkabhängig gemeldet.

## Kindle

Bei geeigneten Büchern kann Kindle **Aa → Mehr / More → Assistive Reader** anbieten.
Verfügbarkeit, Menü und Verwendung der Android-Standardstimme hängen von Buch,
Region, App-Version und Gerät ab. Kindle wurde hier nicht auf einem Gerät getestet;
eine Nutzungsgarantie gibt es nicht. Kein Accessibility-Scraping und keine DRM-Umgehung.

## Datenschutz und Grenzen

Vorlesen überträgt den jeweiligen Text und technische Verbindungsdaten (u.a. IP-Adresse,
Stimme, Tempo) direkt an Microsoft. Es gelten deren Dienst- und Datenschutzbedingungen:
https://privacy.microsoft.com/privacystatement . Nur mit Text verwenden, dessen
Übertragung zulässig und gewollt ist. Zustimmung gilt auch für Anfragen anderer Apps.
Keine Werbung, Analyse, Konten, Schlüssel, Proxy-Backend oder Text-/Audiochronik.
MP3 wird kurz im privaten Cache gehalten, in 24-kHz-Mono-PCM dekodiert und im
`finally` gelöscht; ein Prozessabbruch kann temporäre Cachedateien hinterlassen.
Android-Backup und Klartextverkehr sind deaktiviert. Nur INTERNET-Berechtigung.

20.000 UTF-16-Zeichen pro Anfrage, Unicode-/XML-sichere Blöcke bis 3.000 UTF-8-Bytes,
2 MB MP3 pro Block, 16 MB PCM pro Block. Netzwerkverbindung 10 s, Blockdeadline 35 s,
Decoder: 30 s ohne Codec-Fortschritt, ohne blockierende Framework-Audioausgabe.
Keine Gesamt-Wallclock-Deadline über normale Wiedergabe; endliche Text-/Blocklimits
und die Netzwerkdeadline pro Block bleiben bestehen. Kein automatischer Retry
nach Teilausgabe; Fehler wiederholen keinen schon gesprochenen Text.
Netzwerk-/Dienstfehler werden dem Framework gemeldet. Die App zeigt Fehler verständlich.
Die Startlatenz hängt vom Textblock und Netzwerk ab, da MP3 blockweise gepuffert wird.
Stop bricht den aktiven WebSocket ab und sperrt weitere Audio-Callbacks.
Zustimmungswiderruf meldet stattdessen einen terminalen Fehler; Fehler werden
auch vor dem Audiostart mit error + done auf dem Synthesethread abgeschlossen.
HTTPS- und SSL-Redirects sind deaktiviert; das Produktionsziel ist fest eingebaut.

## Bauen und prüfen

JDK 17, Android SDK 36, Build Tools 35.0.0, AGP 8.10.1,
Kotlin 2.1.21 und Gradle Wrapper 8.13:

```sh
# JDK 17 und Android SDK an eigene Installation anpassen:
export JAVA_HOME=/pfad/zum/jdk-17
export ANDROID_HOME=/pfad/zum/android-sdk
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
sdkmanager 'platforms;android-36' 'build-tools;35.0.0'
# Optional nur auf dem bisherigen Entwicklungsrechner: . /opt/data/android-env.sh
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
# Mit angeschlossenem Gerät/Emulator:
./gradlew :app:connectedDebugAndroidTest
# Opt-in Android: sendet ausschließlich feste öffentliche Testtexte
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.liveEdge=true
# Opt-in JVM: echte Übertragung ausschließlich fest eingebauter öffentlicher Testsätze
./gradlew :core:run --args=/pfad/zu/privatem/scratch
ffprobe -v error -show_streams /pfad/zu/privatem/scratch/0-0.mp3
```

Der JVM-Smoke nutzt exakt denselben `EdgeClient`, GEC und SSML-Code wie die App.
Kein Python auf dem Telefon oder für den Kotlin-Netzwerkbeweis.
Die CI baut, testet und lintet mit read-only Berechtigung. Optional manuell gestartete
API-29-x86_64-Instrumentierung (`workflow_dispatch`, `emulator=true`) prüft
MediaCodec, Consent und Abbruch deterministisch ohne Netzwerk und speichert echte
MainActivity-Screenshots in Hell/Dunkel. `live_speech=true` aktiviert zusätzlich
`liveEdge=true` ausschließlich im Emulatorjob: echtes Android-TextToSpeech mit
expliziter Engine `de.edgespeak.tts`, deutscher Stimme und WAV-Prüfung auf
nichtstilles signed16 PCM, 24 kHz, mono. Zusätzlich werden über 45 Sekunden lange speak-Wiedergabe und Consent-Widerruf
am onStart-Checkpoint geprüft (keine Sleeps). Ohne Runnerargument werden alle
drei Live-Tests übersprungen. Zustimmung und Einstellungen werden wiederhergestellt; Audio wird gelöscht.
Screenshots entstehen privat unter `files/test-output` und werden vor dem
Emulator-Shutdown über `run-as`/`adb pull` mit den Instrumentierungsberichten
als `instrumentation-results` hochgeladen. Diese CI-Beweise sind vorbereitet;
ein erfolgreicher lokaler Build ersetzt keinen tatsächlich ausgeführten Emulatorlauf.
Keine Veröffentlichung, Signing-Secrets oder automatischen Releases.

Die Schaltfläche „Open-Source-Lizenzen“ zeigt die mitgelieferten vollständigen
LGPL-/GPL-/Apache-2.0-Lizenzen und Drittanbieterhinweise offline an.
Vorgesehener Release-Quellstand: https://github.com/Zwiebelrostbraten/EdgeSpeak-Android/tree/v0.1.0
(noch nicht veröffentlicht). CI-Artefakte enthalten zusätzlich die Lizenzdateien.

Siehe [VERIFICATION.md](VERIFICATION.md) für echte Messwerte und noch offene
Android-Laufzeitprüfungen. LGPL-3.0-or-later; [Lizenz](COPYING),
[GPL-Bedingungen](COPYING.GPL), [Drittanbieter](THIRD_PARTY_NOTICES.md).
