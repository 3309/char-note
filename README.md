# CharNotes

A tiny Android app for jotting down short notes (200 characters max, by default) that are
stored locally on the device — no network, no accounts.

## How it works

- **Storage**: [Room](https://developer.android.com/training/data-storage/room) (SQLite under
  the hood), persisted in the app's private database file. Nothing leaves the device.
- **UI**: Jetpack Compose + Material 3. One screen: a list of notes, a "+" button to add a
  new one, and a live `x/200` character counter while typing.
- **Character limit**: controlled by the `CHAR_LIMIT` constant in
  `app/src/main/java/com/example/charnotes/Note.kt`. Change that one number to allow shorter
  or longer notes.

## Project structure

```
app/src/main/java/com/example/charnotes/
  Note.kt            – Room entity + CHAR_LIMIT constant
  NoteDao.kt          – database queries (insert/update/delete/getAll)
  NoteDatabase.kt     – Room database singleton
  NoteViewModel.kt    – loads notes, enforces the character limit, exposes state to the UI
  MainActivity.kt     – Compose UI (list screen + add/edit dialog)
```

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
