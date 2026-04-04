# Impostor Game: Cross-Platform Social Deduction Framework

This repository contains the source code for Impostor Game, a real-time social deduction platform built using Kotlin Multiplatform and Compose Multiplatform. The project enables synchronous multiplayer gameplay across Android, iOS, Web (WebAssembly/JS), and Desktop (Windows/macOS) systems from a single shared codebase.

## Overview

Impostor Game is a strategic social deduction experience designed for group interaction. The system utilizes a centralized real-time database to synchronize game states among participants. During a session, players are assigned specific roles and secret words based on a sophisticated dictionary pairing system.

### Core Objectives

- **Citizens:** Facilitate strategic discussion to identify and eliminate the hidden Impostor.
- **Impostors:** Obfuscate their identity and attempt to deduce the Citizens' secret word through linguistic patterns and contextual clues.
- **Mr. White:** A specialized role receiving no word, requiring high-level social engineering to remain undetected.

## Technical Features

- **Multiplatform Architecture:** Utilizes Kotlin Multiplatform (KMP) to share 95% of business logic and UI components across five target environments.
- **Real-Time Data Synchronization:** Integrated with Firebase Realtime Database for low-latency state updates, messaging, and player presence management.
- **Dynamic Content Management:** Implements an automated word pair loader utilizing the Compose Resources API, featuring a comprehensive Croatian dictionary with over 1,000 entries.
- **Advanced Session Handling:**
    - **Automated Ownership Transfer:** Intelligent algorithm for persistent room administrator handover upon host disconnection.
    - **Database Optimization:** Self-cleaning logic that prunes inactive sessions to maintain database performance and integrity.
- **Discovery and Onboarding:** Efficient room access via high-speed QR code generation and 6-digit alphanumeric session identifiers.
- **Responsive Interface:** Adaptive layout design utilizing Material 3 components, optimized for both mobile aspect ratios and widescreen desktop environments.

## Technical Architecture

- **UI Framework:** Compose Multiplatform (Material 3).
- **Core Logic:** Kotlin Multiplatform (Common).
- **Backend Services:** Firebase Realtime Database (via Platform Interop and REST).
- **Web Runtime:** WebAssembly (Wasm) and JavaScript targets.
- **Deployment:** Continuous delivery via Cloudflare Pages.

## Building and Deployment

### Development Requirements
- Java Development Kit (JDK) 17.
- Android Studio Koala or IntelliJ IDEA 2024.1 or later.
- Xcode (required for iOS compilation on macOS).

### Build Commands

#### Web Distribution
To generate the production-ready distribution for web environments:
```bash
./gradlew :impostor-cross-platform:jsBrowserDistribution
```
Output location: `impostor-cross-platform/build/dist/js/productionExecutable`

#### Native Desktop Application
To execute the native desktop build:
```bash
./gradlew :impostor-cross-platform:run
```

#### Android Application
To install the debug build on a connected device or emulator:
```bash
./gradlew :impostor-cross-platform:installDebug
```

## Configuration

To establish a private backend instance:
1. Provide a valid `google-services.json` within the `:impostor-cross-platform` module.
2. Configure the Firebase JS SDK parameters within `src/jsMain/resources/index.html`.
3. Apply appropriate Firebase Security Rules to secure the `rooms/` node against unauthorized access.
