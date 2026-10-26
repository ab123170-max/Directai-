# Cameraman AI — Native Android

Native Android project for the Cameraman AI creator studio.

## Stack
- Kotlin + Jetpack Compose (UI)
- CameraX Preview + VideoCapture (real device camera and MP4 recording)
- Android SDK 36 / target SDK 36
- Media3 Transformer dependency reserved for the next editing/export phase
- Gemini-backed endpoints remain server-side; never put API keys in the APK

## Current native milestone
- Studio dashboard and project workflow
- Shot list with style and duration controls
- Camera permission flow, front/back camera switch, HD video capture
- Local app-private MP4 storage
- Editing workspace that clearly indicates editing/export is not yet implemented

## Build
Run `gradle assembleDebug` in an Android SDK environment with Gradle 8.11.1 and JDK 17.
GitHub Actions builds a debug APK and stores it as the workflow artifact for 14 days.

## Important
This is an incremental native rebuild, not a claim that the full CapCut-like editor is complete. Timeline editing, transitions, color grading, export, Gemini live analysis, pose detection and automated tests remain follow-up milestones.
