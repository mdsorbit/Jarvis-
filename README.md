# JARVIS — Android Voice Assistant

An Android-first personal voice assistant project.

## Current status

**Foundation / starter screen added.** This repository is not yet a working always-listening assistant and no APK has been built or tested.

## Planned milestones

1. Android app foundation and project structure
2. Microphone permission and clear foreground-service notification
3. Wake-word detection
4. Hindi speech recognition and Hindi text-to-speech
5. Offline commands for basic phone tasks
6. Optional online AI through a secure backend
7. Battery/background behavior testing on real devices

## Android project

Open this repository in Android Studio and sync the Gradle project.

- Application ID: `com.mdsorbit.jarvis`
- Minimum Android SDK: 23
- Target Android SDK: 35
- Language: Kotlin

## Privacy and platform behavior

JARVIS will use microphone access only with user permission and visible Android microphone/foreground-service indicators. Background microphone operation is subject to Android version restrictions. Sensitive actions such as calls, messages, and payments must require user confirmation.

Online AI credentials must not be embedded in the Android app. A secure backend proxy is planned for any production integration.

## Build status

Source files are being prepared in this repository. Build and device testing are still pending.
