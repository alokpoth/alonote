# AloNote (আলোনোট) — Minimal Offline Notes App

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-black?style=flat-square&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.3-black?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-black?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20SQLite%20(100%25%20Offline)-black?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![Website](https://img.shields.io/badge/Website-alonote.zihan.xyz-black?style=flat-square)](https://alonote.zihan.xyz)

> **AloNote (আলোনোট)** is an extremely simple, lightweight, 100% offline note-taking native Android application by **Alokpath (আলোকপথ)**, inspired by Apple Notes and Google Keep. Built with Jetpack Compose, Material 3, and Room Database.

🌐 **Website & Live Demo:** [alonote.zihan.xyz](https://alonote.zihan.xyz) (or [alonote.zihan.uk](https://alonote.zihan.uk))  
📦 **Direct APK Download:** [Download AloNote v2.2.0.1 (Alokpath Edition)](https://alonote.zihan.xyz/downloads/AloNote-v2.2.0.1-Alokpath.apk)  
🏢 **Ecosystem:** An [Alokpath (আলোকপথ)](https://alokpoth.zihan.xyz) Sovereign Privacy Application

---

## 1. Core Principles

- **Zero Cloud & 100% Offline:** No account, no login, no server backend, no network permissions (`android.permission.INTERNET` is completely omitted).
- **Absolute Privacy:** Notes remain stored exclusively on your device in an encrypted app-private SQLite database.
- **Apple-Inspired Monochrome Aesthetic:** Pure black and white design with subtle gray accents, rounded corners, clean typography, and a floating translucent glass navigation bar.
- **Instant Autosave:** Writes are persisted automatically as you type with a debounced pipeline. Leaving or backgrounding the app flushes pending changes immediately with zero data loss.
- **Sensible Empty Draft Policy:** Opening a new note and backing out without typing leaves no clutter or blank database records.
- **Low-End Device Optimization:** Built for smooth 60fps scrolling and responsive typing on devices with 2–4 GB RAM.

---

## 2. Technology Stack

- **Language:** Kotlin 2.3
- **UI Toolkit:** Jetpack Compose with Material 3
- **Architecture:** Clean MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence:** AndroidX Room Database 2.8 with KSP code generation
- **Navigation:** AndroidX Navigation 3 with type-safe `NavKey` backstack
- **Asynchronous Flow:** Kotlin Coroutines (`Dispatchers.IO`) & StateFlow
- **Minimum SDK:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35/36)
- **Build System:** Gradle 9.1 with Kotlin DSL

---

## 3. Architecture & Package Structure

```
com.noto.app
├── data
│   ├── local
│   │   ├── dao
│   │   │   └── NoteDao.kt           # Room DAO queries for notes, search, & favorites
│   │   ├── entity
│   │   │   └── Note.kt              # Room Entity with display title & preview helpers
│   │   └── NotoDatabase.kt          # Room SQLite Database singleton
│   └── repository
│       ├── NoteRepository.kt        # Repository abstraction interface
│       └── NoteRepositoryImpl.kt    # Implementation with Dispatchers.IO isolation
├── ui
│   ├── components
│   │   ├── DeleteConfirmationDialog.kt  # Apple-style confirmation dialog
│   │   ├── EmptyStateView.kt            # Minimalist monochrome empty states
│   │   ├── NoteCard.kt                  # Note list card with quick star toggle
│   │   ├── NotoBottomBar.kt             # Floating translucent glass bottom bar
│   │   └── NotoSearchBar.kt             # Minimalist search field with clear action
│   ├── editor
│   │   ├── NoteEditorScreen.kt      # Distraction-free borderless editor
│   │   └── NoteEditorViewModel.kt   # Debounced autosave & draft policy engine
│   ├── main
│   │   ├── MainScreen.kt            # Note list screen with tabs & search
│   │   └── MainViewModel.kt         # StateFlow search, tabs, & undo deletion
│   ├── navigation
│   │   ├── Navigation.kt            # Navigation 3 NavDisplay & backstack wiring
│   │   └── NavigationKeys.kt        # MainNavKey and NoteEditorNavKey
│   └── theme
│       ├── Color.kt                 # Apple monochrome palette (light & dark)
│       ├── Theme.kt                 # MaterialTheme configuration & bar insets
│       └── Type.kt                  # Clean typography hierarchy
├── util
│   └── DateFormatter.kt             # Relative/concise Apple-style date formatting
├── MainActivity.kt                  # Single-activity edge-to-edge entry point
└── NotoApplication.kt               # Application class managing DB & repo singletons
```

---

## 4. Key Features

### 🗂️ Note Management & Organization
- **All Notes & Pinned Section:** Notes can be pinned to the top of the list for immediate access.
- **Favorites & Star:** Dedicated Favorites tab filters your most essential thoughts.
- **Recycle Bin (Trash):** Soft-delete architecture with safety confirmation, restore notes anytime, or permanently empty trash.
- **Apple-Style Long-Press Actions:** Long-press any note card to trigger a bottom action sheet with haptic feedback: Pin/Unpin, Star/Unstar, Share, Copy Text, or Move to Trash.
- **Order Adjustment:** Move notes up or down with tactile feedback.
- **Instant Search:** Debounced real-time search across titles and body contents with match highlighting.

### ✍️ Intelligent Markdown Editor
- **Live Markdown Syntax Highlighting:** Headings (`#`), bold (`**bold**`), italics (`*italic*`), strikethrough (`~~strike~~`), quotes (`>`), and checkboxes (`- [ ]`, `- [x]`) styled in real time without cursor jumping.
- **Reader / Focus Mode:** One-tap toggle into distraction-free reading mode with interactive checkboxes that toggle directly in the note body.
- **Smart Undo / Redo:** Discrete history stack with debounced character snapshots and discrete formatting action pushes.
- **Smart List Continuation:** Pressing Enter on an active bullet or checklist item automatically continues the list; pressing Enter on an empty checklist cleanly terminates it.
- **Note Statistics Sheet:** Tap the live word pill to inspect word count, character count, lines, paragraphs, estimated reading/speaking time, and checklist completion percentage.
- **Formatting Toolbar:** Docked Apple-style formatting toolbar with Undo, Redo, Bold, Italic, Strikethrough, Heading, Checklist, Bullet List, Code, and Quote.
- **Automatic Saving:** Debounced 500ms background saving ensures you never lose a word.

### 🎨 Design & Experience
- **Apple-Inspired Monochrome Aesthetic:** Pure black and white design with subtle gray accents, rounded corners, clean typography, and a floating translucent glass navigation bar.
- **Theme Switcher:** Seamless switching between System, Light, and Dark mode.
- **Haptic Feedback:** Tactile vibrations on actions, long-presses, and toolbar formatting.
- **120 FPS Fluid Animations:** Smooth spring physics and enter/exit transitions optimized for thermal efficiency.

---

## 5. Security & Signing

- **100% Offline & No Permissions:** Omission of `android.permission.INTERNET` guarantees zero data leakage or telemetry.
- **Developer Signed:** Official release APKs are signed with a custom 2048-bit RSA key:
  - **Developer:** Zihan Fakir (`@zihanfakir`)
  - **Organization:** Zihan Fakir (github.com/zihanfakir)
  - **Validity:** 2026–2054 (10,000+ days)
- **Play Protect Trusted:** Eliminates untrusted developer warnings during sideloading.

---

## 6. Building & Running the Project

### Prerequisites
- JDK 17 or higher
- Android SDK Platform 35/36 with Build-Tools
- Android device or emulator running Android 8.0+

### Compile & Build Release APK
```bash
./gradlew assembleRelease
```
The compiled, signed APK will be located at:
`docs/downloads/Noto-v2.1.0-Alokpath.apk`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```
Runs the complete 26-test suite verifying CRUD operations, debounced autosave, empty draft policy, undo/redo history, smart list continuation, and Markdown visual transformation.

---

## 7. Privacy Policy & Backup
- **100% Offline:** Noto does not request or possess `android.permission.INTERNET`.
- **Local Storage:** All data is stored in the app's private SQLite database on the internal file system.
- **Cloud Backup Disabled:** Cloud auto-backup is explicitly disabled in `data_extraction_rules.xml` and `backup_rules.xml` to prevent note contents from leaking into third-party cloud backups. Direct device-to-device transfers remain permitted for switching phones.

---

## 8. License
Open-source under the [MIT License](LICENSE).  
Authored by [Zihan Fakir](https://zihan.xyz) ([@zihanfakir](https://github.com/zihanfakir)).
