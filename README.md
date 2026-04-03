# Impostor Game 🕵️‍♂️

A modern, fast-paced social deduction game built with **Kotlin Multiplatform** and **Compose Multiplatform**. Play with your friends across Android, iOS, Web, and Desktop (Windows/macOS) in real-time.

## 🎮 What is Impostor Game?

Impostor is a social game of deception and intuition. Players are given a secret word, but one or more players (the **Impostors**) receive a slightly different word or no word at all (the **Mr. White**).

The goal of the citizens is to identify the Impostor through discussion, while the Impostor tries to blend in and guess the secret word of the majority.

### Game Rules:
1. **Secret Words:** Everyone is assigned a role. Citizens get the "Main Word," while the Impostor gets a "Secret Word" from the same category.
2. **The Discussion:** Players take turns describing their word without being too obvious.
3. **The Vote:** After the discussion, players vote on who they think the Impostor is.
4. **Winning:**
   - **Citizens win** if they correctly identify and eject all Impostors.
   - **Impostors win** if they remain undetected or correctly guess the Citizens' word.

---

## 🚀 Features

- **Cross-Platform Play:** Seamless real-time gameplay between mobile, web, and desktop.
- **Huge Vocabulary:** Over 1,000 hand-picked Croatian word pairs across various categories.
- **Real-Time Interaction:** Powered by Firebase for instant synchronization of chat and game states.
- **Smart Admin System:** Dynamic transfer of room ownership if the host leaves.
- **QR Code Entry:** Quickly join rooms by scanning a QR code from the host's screen.
- **Animated UI:** Smooth transitions and modern design for an engaging experience.

---

## 🛠 Tech Stack

- **Language:** Kotlin
- **UI Framework:** Compose Multiplatform
- **Backend:** Firebase Realtime Database
- **Networking:** Ktor
- **Web Hosting:** Cloudflare Pages

---

## 📦 Building and Running

### Desktop (Windows/macOS)
To run the native desktop application:
```bash
./gradlew :impostor-cross-platform:run
```

### Web (Cloudflare Pages)
To build the production version for web deployment:
```bash
./gradlew :impostor-cross-platform:jsBrowserDistribution
```
The output will be in `impostor-cross-platform/build/dist/js/productionExecutable`.

### Android
Build the debug APK:
```bash
./gradlew :impostor-cross-platform:assembleDebug
```

### iOS
Open the `iosApp` folder in **Xcode** and run it on a simulator or a physical device.

---

## 🔧 Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Luksaaa/impostor-crossplatform.git
   ```
2. **Firebase Setup:**
   - Add your `google-services.json` to the `impostor-cross-platform` module.
   - Update the Firebase config in `index.html` for the web version.
3. **Sync Gradle** and you are ready to go!

---

## 📄 License
This project is for educational and entertainment purposes. Feel free to use and modify!

---
*Created with ❤️ using Kotlin Multiplatform.*
