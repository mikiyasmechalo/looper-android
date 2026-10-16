<p align="center">
  <img src="assets/main_logo.svg" width="240" height="96" alt="Looper Logo" />
</p>

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Lexend+Giga&size=28&pause=1200&color=B4BEFE&center=true&vCenter=true&width=900&lines=Native+Android+Music+Player;Material+3+Expressive+%2B+Neo-Blur+Aesthetics;High-Fidelity+Playback+%2B+Glance+Widgets" />
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024--36)-3DDC84?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Audio-Media3%20%2F%20ExoPlayer-FF6F00?style=for-the-badge&logo=googleplay&logoColor=white" />
  <img src="https://img.shields.io/badge/Widgets-AndroidX%20Glance-7B1FA2?style=for-the-badge&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Architecture-Hilt%20%2B%20Room%20%2B%20MVI-00C853?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Version-1.0.0--beta1-blueviolet?style=for-the-badge" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-green?style=for-the-badge" />
</p>

---

## 🔮 Experience Local Audio in Cyber-Glass Aesthetics

**Looper for Android** is a native, offline-first, high-fidelity local music player engineered completely from the ground up using **Kotlin**, **Jetpack Compose**, and **AndroidX Media3 (ExoPlayer)**.

It pairs an audio engine with a **Glassmorphic / Neo-Blur** interface that extracts dynamic color palettes from your album artwork in real time. Designed with a strict **offline-first, zero-telemetry** philosophy, your audio files, playlists, lyrics, and configurations stay exclusively on your device.

---

## 🚀 Key Features

```
┌────────────────────────────────────────────────────────────────────────┐
│                        LOOPER ANDROID FEATURES                         │
├───────────────────────────────────────┬────────────────────────────────┤
│ 🔮 GLASSMORPHIC CYBER-UI              │ ⚡ MEDIA3 / EXOPLAYER AUDIO   │
│   Real-time blur, glow borders, and   │   Bit-perfect gapless playback,│
│   dynamic palette extraction.         │   audio focus & lockscreen sync│
├───────────────────────────────────────┼────────────────────────────────┤
│ 🧩 INTERACTIVE GLANCE WIDGET SUITE    │ 🎤 SYNCHRONIZED LRC LYRICS     │
│   5 dynamic home screen widgets:      │   Real-time smooth scrolling,  │
│   Mini, Classic, Art, Square & Lyrics!│   tap-to-seek & LRCLIB sync.   │
├───────────────────────────────────────┼────────────────────────────────┤
│ 🎚️ 18-BAND GRAPHIC EQUALIZER         │ 🔒 100% PRIVATE & OFFLINE      │
│   Custom curve shaping, dB grids,     │   Zero telemetry, no trackers, │
│   bass boost & zero-snapping.         │   no cloud accounts required.  │
└───────────────────────────────────────┴────────────────────────────────┘
```

### 🎨 Material 3 Expressive & Neo-Blur Aesthetics
- **Dynamic Palette Morphing**: Ambient background glows, button highlights, progress sliders, and frosted glass cards adapt to match the album artwork of the playing track with smooth crossfade animations.
- **Glassmorphic Surfaces**: Custom blur backdrops and translucent borders provide depth and modern visual hierarchy across standard, tall, and foldable screens.

### 🧩 Interactive AndroidX Glance Widget Suite
Looper features a collection of home screen widgets built using AndroidX Glance (Compose for AppWidgets):
- **Mini Capsule Widget**: A compact pill widget featuring essential controls (Play/Pause, Previous, Next), title/artist marquee, dynamic color adaptation, optional border outlines, and matching album corner radii.
- **Classic Widget**: An expanded media card showing high-resolution album artwork, seek progress bar, and full transport controls.
- **Square Widget**: A balanced layout that intelligently scales album art as the widget is resized on your home screen grid.
- **Synchronized Lyrics Widget**: Live lyrics directly on your Android launcher! Dynamically expands from **3 lines to 5 lines** when stretched vertically, featuring centered active lyric lines and tap-to-open actions.
- **In-App Widget Customizer**: Real-time interactive preview screen to fine-tune widget transparency (0% to 100%), border outlines, corner radii, and favorite button toggles.

### 🎤 Synchronized LRC Lyrics
- **Real-time Karaoke Scrolling**: Fluid line-by-line animations synchronized with playback progress.
- **Tap-to-Seek**: Jump straight to any section of the song simply by tapping a lyric line.
- **Offline & Online Sources**: Automatically loads embedded and local `.lrc` files, or fetches synchronized lyrics on-demand via [LRCLIB](https://lrclib.net/).
- **Customizable Experience**: Supports font scaling, translation toggle, and display keep-awake modes.

### ⚡ Media3 & ExoPlayer Engine
- **Gapless Audio & Fade Transitions**: Smooth playback transitions without audio pops or stuttering.
- **Intelligent Audio Focus**: Seamlessly handles phone calls, navigation prompts, and transient audio ducking with customizable resume behaviors.
- **Background Persistence**: Native Android `MediaSessionService` ensures stable background playback that persists even when the app is swiped away from Recents.
- **Timeline & Queue Recovery**: Remembers your active queue and exact playback timestamp down to the millisecond across app restarts.

### 🎚️ 18-Band Equalizer & Sound Effects
- Multi-band graphical equalizer with curve manipulation, decibel scale guidelines (+/- 20 dB), frequency adjustment bands (65Hz to 20kHz), zero-snapping, and instant preset selection.

---

## 📸 Screenshots

### 📱 Now Playing & Synchronized Lyrics
<p align="center">
  <img src="assets/screenshots/01_now_playing_player.jpg" width="24%" alt="Now Playing Screen" />
  <img src="assets/screenshots/02_synchronized_lyrics.jpg" width="24%" alt="Live Synced Lyrics" />
  <img src="assets/screenshots/03_lyrics_scroll.jpg" width="24%" alt="Smooth Lyrics Scrolling" />
  <img src="assets/screenshots/04_player_paused.jpg" width="24%" alt="Glassmorphic Controls" />
</p>

### 🎨 Real-time Dynamic Theming & Neo-Blur
<p align="center">
  <img src="assets/screenshots/06_dynamic_theming_player.jpg" width="31%" alt="Dynamic Theming Player" />
  <img src="assets/screenshots/07_dynamic_theming_lyrics.jpg" width="31%" alt="Dynamic Theming Lyrics" />
  <img src="assets/screenshots/11_dashboard_rockstar.jpg" width="31%" alt="Dynamic Ambient Dashboard" />
</p>

### 📚 Home Dashboard, Library & Settings
<p align="center">
  <img src="assets/screenshots/05_home_dashboard.jpg" width="31%" alt="Home Dashboard" />
  <img src="assets/screenshots/08_music_library.jpg" width="31%" alt="Music Library" />
  <img src="assets/screenshots/10_all_songs_tracklist.jpg" width="31%" alt="All Songs Tracklist" />
</p>

### 🧩 Interactive AndroidX Glance Widgets
<p align="center">
  <img src="assets/screenshots/13_widgets_square_fullart_compact.jpg" width="32%" alt="Square, Full Art, and Classic Widgets" />
  <img src="assets/screenshots/14_widgets_lyrics_miniplayer.jpg" width="32%" alt="Synchronized Lyrics & Mini Player Widgets" />
  <img src="assets/screenshots/09_settings_overview.jpg" width="32%" alt="Settings Overview" />
</p>

---

## 🛠️ Tech Stack & Architecture

```
┌────────────────────────────────────────────────────────┐
│               LOOPER ANDROID ARCHITECTURE              │
├────────────────────────────────────────────────────────┤
│  Language           │  Kotlin 2.0+                     │
│  UI Toolkit         │  Jetpack Compose + Material 3    │
│  AppWidgets         │  AndroidX Glance                 │
│  Audio Engine       │  AndroidX Media3 (ExoPlayer)     │
│  Dependency Inject. │  Dagger Hilt                     │
│  Local Database     │  Room SQLite + Flow              │
│  Image Loading      │  Coil 3 + ContentResolver Thumb  │
│  Target SDK         │  Android 15 (API 36)             │
│  Minimum SDK        │  Android 7.0 (API 24)            │
└────────────────────────────────────────────────────────┘
```

- **MVI / Clean Architecture**: Reactive unidirectional data flow using Kotlin `StateFlow`, `SharedFlow`, and coroutines.
- **Scoped Storage & MediaStore**: Compliant with Android 10+ (`Q`) through Android 15 (`VanillaIceCream`) storage standards with high-speed thumbnail caching.
- **ProGuard / R8 Optimization**: Release builds include bytecode shrinking, resource stripping, and log de-bloating for maximum performance (~8 MB standalone APK).

---

## 📦 Download & Installation

Grab the latest Beta build from the [Releases](https://github.com/mikiyasmechalo/looper-android/releases) tab.

1. Download the APK compatible with your device.
2. Open the downloaded APK on your Android device and confirm installation.
   > *Note: If prompted, enable "Install unknown apps" for your browser or file manager.*
3. Enjoy your music offline with zero setup required!

---

## 💻 Building from Source

### Prerequisites
- **JDK 17** (Temurin or OpenJDK recommended)
- **Android Studio** (Ladybug | 2024.2.1+ recommended)
- **Android SDK** API 36 with Build-Tools 36.0.0

### Build Steps
```bash
# Clone the repository
git clone https://github.com/mikiyasmechalo/looper-android.git
cd looper-android

# Build Debug APK
./gradlew assembleDebug

# Build Release APK and Android App Bundle (.aab)
./gradlew assembleRelease bundleRelease
```

The output artifacts will be located in:
- APK: `app/build/outputs/apk/release/app-release.apk`
- AAB: `app/build/outputs/bundle/release/app-release.aab`

---

## 👥 Contributors & Open Source

Looper for Android is brought to you by the collaborative open-source community:

### Native Android Port
* **Mikiyas Mechalo** — *Android Lead Developer & Jetpack Compose Architect*  
  [![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/mikiyasmechalo)
  [![Telegram](https://img.shields.io/badge/Telegram-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/mikeme2)

### Original Looper Team
* **Nilesh Suthar** — *Original Creator, Architect & Maintainer*  
  [![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/SthrNilshaaa)
  [![Telegram](https://img.shields.io/badge/Telegram-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/neelshy)
* **Karan Suthar (Krn.)** — *Creative UI/UX & Interaction Designer*  
  [![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/sthrkaran)
  [![Telegram](https://img.shields.io/badge/Telegram-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/karanwhy)
* **Madan Suthar** — *Active Contributor*  
  [![Telegram](https://img.shields.io/badge/Telegram-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/madansthr)

---

## 📄 License

This project is licensed under the **GPL-3.0 License**. See [LICENSE](LICENSE) for details.
