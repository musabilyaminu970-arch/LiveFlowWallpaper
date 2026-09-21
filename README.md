# Live Flow Wallpaper

A complete Android Studio project for a functional animated live wallpaper.

## Features

- Kotlin
- Android SDK 35
- minSdk 23
- No external runtime dependencies
- Uses Android's `WallpaperService` API
- Launcher activity opens Android's live-wallpaper system
- Lightweight Canvas animation with moving gradients, ribbons and particles
- Phone-only cloud build workflow included

## Build without a laptop

See **PHONE_BUILD.md**.

The included GitHub Actions workflow builds `app-debug.apk` on GitHub's servers, so Android Studio does not need to be installed on your phone.

## Build locally

Open the project in Android Studio and build the `app` module.
