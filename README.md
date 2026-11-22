# JARVIS — Android Voice Assistant

Kotlin Android starter for a personal Hindi voice assistant.

## Current version: 0.3.0

Picovoice / Porcupine has been removed. No Picovoice signup, company email, AccessKey, or API key is needed for this starter.

## Current behavior

- Open the app and tap **START VOICE ASSISTANT**.
- Grant microphone permission.
- Speak in Hindi; recognized words appear on screen.
- JARVIS gives a basic spoken Hindi response using Android Text-to-Speech.
- Tap **STOP LISTENING** to stop the recognition loop.
- On Android 12+ the app prefers Android's on-device speech recognizer when one is available. Availability and Hindi language-pack support depend on the phone's installed speech services. If on-device recognition is unavailable, Android's configured speech service may require internet.

## Important limitations

This is a no-key voice-command starter, **not an always-on “Jarvis” wake-word detector**. Android's public SpeechRecognizer API is not a replacement for a dedicated low-power hotword engine and is not designed to keep a third-party app continuously listening with the screen locked. Recognition is started by the user while the app is open. Screen-locked wake-word support needs a separately tested, Android-compatible hotword implementation and must follow Android microphone/foreground-service rules.

This source has not yet been built or tested on a physical phone. No APK is available yet.

## Build

Open the repository in Android Studio, allow Gradle sync, then run the `app` configuration on a real Android phone.

- Application ID: `com.mdsorbit.jarvis`
- Minimum Android SDK: 23
- Compile / target SDK: 35
- Language: Kotlin
- Speech recognition: Android SpeechRecognizer
- Spoken responses: Android TextToSpeech

## Privacy and safety

Microphone permission is required. Use the visible app controls to start and stop listening. Android may route speech recognition through an installed service; check the phone's speech-service and offline language settings if offline use is important. Sensitive actions such as calls, messages, and payments must require explicit user confirmation.