# JARVIS — Android Voice Assistant

An Android-first personal voice assistant project written in Kotlin.

## Current status

**Stage 1 source code is in place:** Android app screen, microphone permission flow, visible microphone foreground service, and offline Porcupine wake-word detection configured for the built-in **“Jarvis”** keyword.

This source has not yet been built or tested on a physical phone. No APK is available yet.

## Configure the Picovoice AccessKey

Porcupine requires a Picovoice Console account and an AccessKey.

1. Create/sign in to your account at https://console.picovoice.ai/.
2. Copy your AccessKey.
3. In the Android project root, create a local file named `local.properties` (this file is ignored by Git).
4. Add this line, replacing the placeholder with your own key:

```properties
PICOVOICE_ACCESS_KEY=YOUR_PICOVOICE_ACCESS_KEY
```

Do not commit or share your AccessKey. Note that a key packaged into an Android client can be extracted from an APK; production use needs a carefully managed key and service-side controls.

## Build

Open this repository in Android Studio, allow Gradle sync, then run the `app` configuration on a real Android phone.

- Application ID: `com.mdsorbit.jarvis`
- Minimum Android SDK: 23
- Compile / target SDK: 35
- Language: Kotlin
- Wake-word SDK: `ai.picovoice:porcupine-android:4.0.2`

## Current behavior

- The user must open JARVIS and press **START LISTENING**.
- The app requests microphone permission and notification permission where applicable.
- Android displays a persistent foreground-service notification and microphone indicator while listening.
- The wake-word engine listens locally for **“Jarvis”**.
- Press **STOP LISTENING** to stop the microphone service.

## Next milestones

1. Verify Gradle build and wake-word detection on the target phone.
2. Start Hindi speech recognition after “Jarvis” is detected.
3. Add Hindi text-to-speech responses.
4. Add offline commands for basic phone tasks.
5. Add optional online AI through a secure backend.
6. Test background, screen-locked, battery and OEM restrictions.

## Privacy and Android behavior

Microphone use is permission-based and visible through Android's foreground-service notification and microphone indicator. Android versions and phone manufacturers can restrict microphone foreground services and background operation; starting the service from the visible app is intentional. Screen-locked reliability must be tested on the actual device and cannot be guaranteed by source code alone.

Sensitive actions such as calls, messages, and payments must require explicit user confirmation. Online AI credentials must not be embedded in the app.
