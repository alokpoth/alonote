# Noto — Minimal Offline Notes App

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-black?style=flat-square&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin%202.3-black?style=flat-square&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%2F%20Material%203-black?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Storage](https://img.shields.io/badge/Storage-Room%20SQLite%20(100%25%20Offline)-black?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![Website](https://img.shields.io/badge/Website-note.zihan.xyz-black?style=flat-square)](https://note.zihan.xyz)

> **Noto** is an extremely simple, lightweight, 100% offline note-taking native Android application inspired by Apple Notes and Google Keep. Built with Jetpack Compose, Material 3, and Room Database.

🌐 **Website & Live Demo:** [note.zihan.xyz](https://note.zihan.xyz)  
📦 **Direct APK Download:** [Download Noto v1.0 APK](https://note.zihan.xyz/downloads/Noto-v1.0.apk)

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

### 🗂️ Note Management
- **All Notes:** Displays saved notes sorted by last edited date.
- **Favorites:** Star important notes to access them instantly from the Favorites tab.
- **Instant Search:** Debounced real-time search across titles and body contents.
- **Delete with Confirmation:** Delete notes with a native dialog or instant undo support.

### ✍️ Note Editor
- **Borderless Typography:** Clean Apple Notes inspired layout with large title and comfortable line height.
- **Automatic Saving:** Debounced 500ms background saving ensures you never lose a word.
- **Smart Draft Cleanup:** Completely empty drafts are never saved, keeping your database clean.
- **Keyboard Handling:** Keyboard opens automatically on new note creation, preserving cursor positions without jarring recomposition.

### 🧭 Floating Translucent Navigation Bar
- **Favorites (Star):** Toggles between all notes and starred favorites.
- **New Note (Plus):** Centered primary action button that immediately opens a fresh editor.
- **Notes (Document):** Default tab displaying all notes.
- **Translucent Glass Surface:** Subtle border and light blur fallback with complete navigation bar inset awareness.

---

## 5. Building & Running the Project

### Prerequisites
- JDK 17 or higher
- Android SDK Platform 35/36 with Build-Tools
- Android device or emulator running Android 8.0+

### Compile & Build Debug APK
```bash
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```
Runs the complete unit test suite verifying CRUD operations, debounced autosave, empty draft policy, and date formatting.

---

## 6. Privacy Policy & Backup
- **100% Offline:** Noto does not request or possess `android.permission.INTERNET`.
- **Local Storage:** All data is stored in the app's private SQLite database on the internal file system.
- **Cloud Backup:** Cloud auto-backup is explicitly disabled in `data_extraction_rules.xml` and `backup_rules.xml` to prevent note contents from leaking into third-party cloud backups. Direct device-to-device transfers remain permitted for switching phones.

---

## 7. License
Open-source under the [MIT License](LICENSE).
Developed by [Zihan Fakir](https://zihan.xyz).
