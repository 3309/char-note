# CharNotes

A tiny Android app for jotting down short notes (200 characters max, by default) that are
stored locally on the device — no network, no accounts. Notes have a title, content, and a
recorded creation date/time, and the app is protected by a password lock screen.

## How it works

- **Storage**: [Room](https://developer.android.com/training/data-storage/room) (SQLite under
  the hood), persisted in the app's private database file. Nothing leaves the device.
- **UI**: Jetpack Compose + Material 3. A list of notes, a "+" button to add a new one, and a
  live `x/200` character counter while typing.
- **Character limit**: controlled by the `CHAR_LIMIT` constant in
  `app/src/main/java/com/example/charnotes/Note.kt`. Change that one number to allow shorter
  or longer notes. `TITLE_CHAR_LIMIT` controls the title length the same way.
- **Created date/time**: recorded automatically (`System.currentTimeMillis()`) the moment a
  note is first saved, and shown on each note card.
- **App lock**: on first launch you're asked to set a password. Every subsequent launch (and
  whenever you tap the lock icon in the top bar) requires that password before the notes are
  shown. The password is hashed (SHA-256) and stored locally in the app's private
  SharedPreferences — it's not sent anywhere, but this is a casual-privacy lock, not
  military-grade security. **There's no "forgot password" flow**: if you forget it, the only
  way back in is clearing the app's storage (Settings → Apps → CharNotes → Storage → Clear
  data), which also deletes all notes.

## Project structure

```
app/src/main/java/com/example/charnotes/
  Note.kt             – Room entity (title, content, timestamp) + CHAR_LIMIT / TITLE_CHAR_LIMIT
  NoteDao.kt           – database queries (insert/update/delete/getAll)
  NoteDatabase.kt      – Room database singleton
  NoteViewModel.kt     – loads notes, enforces character limits, exposes state to the UI
  PasswordManager.kt   – hashes/stores/checks the app-lock password (SharedPreferences)
  MainActivity.kt      – Compose UI: lock/unlock screens, list screen, add/edit dialog
```

## A note on the app lock and existing data

This version changed the notes database schema (added a title column) and bumped the Room
database version, using `fallbackToDestructiveMigration()`. If you had a previous build of
this app installed, updating to this version will wipe existing notes (a fresh empty database
gets created). This only matters once — future updates that don't change the schema won't
affect your data.

## Opening the project

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. Choose **File → Open** and select the `CharNotesApp` folder.
3. Let Gradle sync (Android Studio will download the Gradle wrapper automatically the first
   time — no need to run anything by hand).
4. Click **Run ▶** with an emulator or a plugged-in device (minimum Android 7.0 / API 24).

## Things you might want to change

- **Character limit** — edit `CHAR_LIMIT` in `Note.kt`.
- **App icon / name** — `app/src/main/res/values/strings.xml` and the manifest's
  `android:icon` (currently a placeholder system icon so the project builds without extra
  image assets).
- **Search / tags / pinning** — the DAO and entity are intentionally minimal; extend `Note`
  with extra columns and add queries to `NoteDao` as needed.

## Notes on scope

This is intentionally a single-screen, single-purpose app: add, edit, delete, done. There's
no cloud sync, sharing, or export — everything lives in the local SQLite database via Room,
so notes persist across app restarts but stay on-device.
