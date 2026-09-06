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

## Keeping your data across rebuilds

Every debug APK needs to be signed, and by default Android auto-generates a fresh, random
debug signing key on whatever machine builds it. Since GitHub Actions runs on a brand-new
temporary machine each time, that meant every build used a *different* signature — and
Android refuses to install an "update" whose signature doesn't match what's already on your
phone, forcing an uninstall (which wipes the local database) every time.

To fix this, a **fixed debug keystore** (`app/debug.keystore`) is checked into the repo and
wired up in `app/build.gradle.kts`, so every build — from Android Studio or from GitHub
Actions — is signed identically. From now on, installing a new build over an existing one
works as a normal in-place update, and your notes persist.

**One-time catch:** the very first time you install a build made with this fixed keystore,
you'll still need to uninstall whatever's currently on your phone (it was signed with a
different, randomly-generated key). After that one uninstall, every future update will
install cleanly without wiping your data.

> Note: `debug.keystore` here is intentionally a throwaway, well-known development key (not
> a secret) — this is normal practice for debug builds and is never used for a real Play
> Store release. Don't reuse it for a signed release build.

## Adding notes from Tasker (or other automation apps)

CharNotes exposes a broadcast receiver so automation apps can create a note without opening
the UI at all. In Tasker, create a task with:

- **Action**: System → Send Intent
- **Action**: `com.example.charnotes.ADD_NOTE`
- **Cat**: Default
- **Target**: Broadcast Receiver
- **Package**: `com.example.charnotes`
- **Class**: `com.example.charnotes.NoteReceiver`
- **Extra**: `content:Your note text here` (required — this becomes the note body)
- **Extra**: `title:Your title here` (optional)

Running that Tasker action saves a new note immediately, timestamped like any other note.

**Two things worth knowing:**
- Notes added this way **skip the password lock screen** — there's no UI involved, so
  nothing to unlock. That's expected for automation, but keep it in mind if you're relying
  on the lock for privacy.
- The receiver is exported with no permission requirement, meaning *any* app on your device
  that knows the action name (`com.example.charnotes.ADD_NOTE`) could add a note this way —
  not just Tasker. For a personal, low-stakes notes app this is a reasonable trade-off for
  simplicity, but it's not locked down against other apps on your phone.


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
