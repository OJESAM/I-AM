# Project Plan

App Name: Kairos Living Stewards
Description: A daily devotional app for Kairos Living Glory Church. 
Features:
- Daily devotional notes.
- Search for fellowship places (cells) near the user.
- View fellowship leader (cell leader) profiles: name, email, phone, address, meeting date, and time.
- Watch livestream sermons on YouTube.
- Share devotional notes on social media.
- Material Design 3 (M3) with a vibrant, energetic color scheme.
- Full Edge-to-Edge display.
- Adaptive app icon.

## Project Brief

# Kairos Living Stewards - Project Brief

**Kairos Living Stewards** is a vibrant, modern Android application designed for the Kairos Living Glory Church community. The app serves as a spiritual companion, providing daily devotionals, connection to local fellowship groups, and access to live worship services.

## Features
1.  **Daily Devotionals & Sharing:** A curated feed of daily spiritual notes and reflections with built-in social sharing to spread the message across platforms.
2.  **Fellowship Locator:** A location-based search tool to help users find the nearest church "cells" or fellowship places.
3.  **Leader Directory:** Detailed profiles for fellowship leaders, including contact information (phone, email) and specific meeting schedules/addresses.
4.  **YouTube Livestreaming:** Integration with YouTube to allow users to watch live sermons and church services directly within the app.

## High-Level Tech Stack
*   **Kotlin:** The primary language for modern Android development.
*   **Jetpack Compose:** For building a declarative UI following **Material Design 3** standards, including support for Edge-to-Edge display.
*   **Coroutines & Flow:** To manage asynchronous tasks and reactive data streams smoothly.
*   **Retrofit & OkHttp:** For handling network requests to fetch devotionals and interact with the YouTube API.
*   **Play Services Location:** For detecting user proximity to fellowship groups.
*   **KSP (Kotlin Symbol Processing):** Used for efficient code generation (e.g., for Moshi or Room if added later).
*   **Coil:** For high-performance image loading of leader profiles and devotional media.

## Implementation Steps

### Task_1_Base_and_Theming: Set up the Material Design 3 theme with a vibrant color scheme, enable full Edge-to-Edge display, and establish the main Navigation structure using Jetpack Compose.
- **Status:** COMPLETED
- **Updates:** The Material Design 3 theme and base navigation structure for "Kairos Living Stewards" have been successfully implemented.

### Task_2_Devotionals_and_Livestream: Implement the Daily Devotionals screen with sharing capabilities and integrate the YouTube player for livestream sermons.
- **Status:** COMPLETED
- **Updates:** The "Daily Devotionals" and "Livestream" features have been successfully implemented.

### Task_3_Fellowship_Locator_and_Profiles: Develop the location-based Fellowship (cell) locator and the Leader Directory with contact and meeting details.
- **Status:** COMPLETED
- **Updates:** The "Fellowship Locator" and "Leader Profiles" features for the Kairos Living Stewards app have been successfully implemented.

### Task_4_App_Assets_and_Verification: Create an adaptive app icon, refine the UI for M3 consistency, and perform a final run and verify to ensure stability and requirement alignment.
- **Status:** IN_PROGRESS
- **Updates:** The "Kairos Living Stewards" application failed the final quality check due to a **critical stability issue**.
 The application crashes immediately upon launch on the test device.

### **Priority #1: Stability - CRITICAL FAILURE**
- **Observation**: The application crashes on launch with a `java.lang.ClassNotFoundException` for `com.example.kairoslivingstewards.MainActivity`.
- **Crash Log
 Summary**:
  ```
  java.lang.RuntimeException: Unable to instantiate activity ComponentInfo{com.example.kairoslivingstewards/com.example.kairoslivingstewards.MainActivity}: java.lang.ClassNotFoundException: Didn't find class "com.example.kairoslivingst
ewards.MainActivity" on path: DexPathList[...]
  ```
- **Rationale**: This is a showstopper. The application is completely unusable in its current state.
- **Root Cause Analysis**: Upon inspecting `app/build.gradle.kts` and `libs.versions.toml`, it appears that
 the **Kotlin Android plugin** (`org.jetbrains.kotlin.android`) is missing from the `plugins` block. While the `kotlin-compose` plugin (Compose Compiler) is present, the base Kotlin plugin required to compile Kotlin source code for Android is not applied. Consequently, `MainActivity.kt` and other Kotlin files
 are likely not being compiled into the DEX file, leading to the `ClassNotFoundException`.
- **Suggestion**: 
    1. Add the Kotlin Android plugin to `libs.versions.toml`:
       ```toml
       [plugins]
       kotlin-android = { id = "org.jetbrains.kotlin.android
", version.ref = "kotlin" }
       ```
    2. Apply the plugin in `app/build.gradle.kts`:
       ```kotlin
       plugins {
           alias(libs.plugins.android.application)
           alias(libs.plugins.kotlin.android) // Add this

           alias(libs.plugins.kotlin.compose)
           alias(libs.plugins.google.devtools.ksp)
       }
       ```

### **Priority #2: Core Features - UNVERIFIED**
Due to the crash on launch, none of the interactive features (Devotionals, Fellowship Locator,
 Leader Profiles, Livestream) could be verified. However, a code review of the source files indicates:
- **Daily Devotionals**: Implemented in `DevotionalsScreen.kt` with a `LazyColumn` and detail view.
- **Fellowship Locator**: Implemented in `FellowshipScreen
.kt` with distance calculations and permission handling.
- **Livestream**: Implemented using the `android-youtube-player` library in `LivestreamScreen.kt`.

### **Priority #3: UI/UX - PARTIAL VERIFICATION**
- **Edge-to-Edge**: `enableEdge
ToEdge()` is called in `MainActivity.kt`, and `Scaffold` is used correctly with `innerPadding`.
- **Theming**: The code shows usage of Material 3 components and a custom `KairosLivingStewardsTheme`.
- **App Icon**: **SUCCESS**. The adaptive icon is correctly
 implemented in `res/mipmap-anydpi-v26/ic_launcher.xml` and `res/drawable/ic_launcher_foreground.xml`. The vector drawable features a centered white cross and symbolic green leaves, matching the church/steward theme perfectly.

### **Overall Status**: **FAILED
**
The application requires an immediate fix for the build configuration (missing Kotlin plugin) before any further feature testing can proceed.
- **Acceptance Criteria:**
  - Adaptive app icon matches the app's core function
  - Vibrant, energetic aesthetic verified across all screens
  - App does not crash during navigation or feature use
  - All existing tests pass and build is successful
- **StartTime:** 2026-03-27 17:31:30 EAT

