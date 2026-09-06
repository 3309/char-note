# CharNotes

A tiny Android app for jotting down short notes that are stored locally on the device — no network, no accounts. Notes have a title, content, and a
recorded creation date/time, and the app is protected by a password lock screen.

## How it works

- **Storage**: [Room](https://developer.android.com/training/data-storage/room) (SQLite under
  the hood), persisted in the app's private database file. Nothing leaves the device.
- **UI**: Jetpack Compose + Material 3. A list of notes, a "+" button to add a new one, and an
  unlimited-length note body (titles are capped short, see below).
- **Title length**: capped via the `TITLE_CHAR_LIMIT` constant in
  `app/src/main/java/com/example/charnotes/Note.kt`. There's no limit on note content —
  if you want to reintroduce one, add a `.take(N)` where content is trimmed in
  `NoteViewModel.kt`.
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
  Note.kt             – Room entity (title, content, timestamp) + TITLE_CHAR_LIMIT
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

## Attaching a photo or file to a note

Each note can optionally carry one attachment. In the add/edit dialog, tap **"Attach a photo
or file"** to open the system file/photo picker. Whatever you pick is **copied into the app's
private storage** (under `filesDir/attachments/`), so the note keeps its attachment even if
the original photo or file is later deleted or moved. Images show as a thumbnail directly on
the note card; any other file type shows as a tappable filename chip that opens it with
whatever app on your phone handles that file type. Tap the **X** next to an attachment in the
edit dialog to remove it from the note (this also deletes the copied file).

## Moving your notes to a new phone (Backup / Restore)

Since this app is sideloaded rather than installed from the Play Store, Android's automatic
phone-to-phone data transfer isn't reliable for it. Instead, use the built-in backup feature:

1. On your **old phone**, open the overflow menu (⋮ in the top bar) → **"Backup notes…"**.
   Choose where to save the file (Downloads, Google Drive, etc.) — it creates a single
   `.zip` containing every note and its attachments.
2. Move that `.zip` file to your new phone any way you like (email it, upload to Drive and
   download it there, USB transfer, etc.).
3. Install CharNotes on the **new phone** (same build, so it can be a fresh build from the
   same GitHub Actions pipeline).
4. Open the overflow menu → **"Restore from backup…"** and select the `.zip` file. Your
   notes and attachments are added back in.

A few notes on this:
- Restoring **adds** the backed-up notes to whatever's already in the app rather than
  replacing them, so it's safe to restore into an app that already has some notes.
- The backup file isn't encrypted — if your notes are sensitive, keep the `.zip` somewhere
  private (not, say, in a shared Drive folder) between export and import.
- This is a manual step you do once when switching phones, not an ongoing sync — there's no
  automatic cloud backup here.


## Opening the project

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. Choose **File → Open** and select the `CharNotesApp` folder.
3. Let Gradle sync (Android Studio will download the Gradle wrapper automatically the first
   time — no need to run anything by hand).
4. Click **Run ▶** with an emulator or a plugged-in device (minimum Android 7.0 / API 24).

## Things you might want to change

- **Title length limit** — edit `TITLE_CHAR_LIMIT` in `Note.kt`. Note content itself is
  currently unlimited.
- **App icon / name** — `app/src/main/res/values/strings.xml` and the manifest's
  `android:icon` (currently a placeholder system icon so the project builds without extra
  image assets).
- **Search / tags / pinning** — the DAO and entity are intentionally minimal; extend `Note`
  with extra columns and add queries to `NoteDao` as needed.

## Notes on scope

This is intentionally a single-screen, single-purpose app: add, edit, delete, done. There's
no cloud sync, sharing, or export — everything lives in the local SQLite database via Room,
so notes persist across app restarts but stay on-device.
