package com.example.charnotes

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Exports/imports the whole notes database (plus attachment files) as a single .zip:
 *   notes.json          – a JSON array with one object per note
 *   attachments/<name>  – a copy of every attachment file referenced by notes.json
 *
 * This is meant for moving your data to a new phone: export on the old phone, copy the
 * resulting .zip over any way you like (email, Drive, USB, etc.), then import it on the
 * new install of the app.
 */
object BackupManager {

    private const val NOTES_ENTRY = "notes.json"
    private const val ATTACHMENTS_DIR_IN_ZIP = "attachments/"

    /** Core zip-writing logic, shared by "save to a picked location" and "write to a temp file to share". */
    private fun writeZip(out: OutputStream, notes: List<Note>) {
        ZipOutputStream(out).use { zip ->
            val jsonArray = JSONArray()

            for (note in notes) {
                val obj = JSONObject()
                obj.put("title", note.title)
                obj.put("content", note.content)
                obj.put("timestamp", note.timestamp)

                val path = note.attachmentPath
                if (path != null && File(path).exists()) {
                    val zipEntryName = File(path).name
                    obj.put("attachmentFile", zipEntryName)
                    obj.put("attachmentName", note.attachmentName ?: zipEntryName)
                    obj.put("attachmentMimeType", note.attachmentMimeType ?: "")

                    zip.putNextEntry(ZipEntry(ATTACHMENTS_DIR_IN_ZIP + zipEntryName))
                    File(path).inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
                jsonArray.put(obj)
            }

            zip.putNextEntry(ZipEntry(NOTES_ENTRY))
            zip.write(jsonArray.toString().toByteArray())
            zip.closeEntry()
        }
    }

    /** Saves the backup to a location the user picked via the system file/Drive picker. */
    suspend fun exportBackup(context: Context, destination: Uri, notes: List<Note>): Boolean {
        return try {
            context.contentResolver.openOutputStream(destination)?.use { out ->
                writeZip(out, notes)
            } ?: return false
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Writes the backup to a temp file under the app's cache directory, suitable for
     * handing to Android's share sheet (so the user can send it to Google Drive, Gmail,
     * WhatsApp, Bluetooth, or anything else installed). Returns the file, or null on failure.
     */
    suspend fun exportBackupToCache(context: Context, notes: List<Note>): File? {
        return try {
            val dir = File(context.cacheDir, "backups").apply { mkdirs() }
            // Clear old shared backups so the cache doesn't grow unbounded.
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "charnotes_backup_${System.currentTimeMillis()}.zip")
            file.outputStream().use { out -> writeZip(out, notes) }
            file
        } catch (e: Exception) {
            null
        }
    }

    /** Result of an import: how many notes were restored. Null overall = failure. */
    suspend fun importBackup(context: Context, source: Uri): Int? {
        return try {
            val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
            var notesJson: String? = null
            val restoredFiles = mutableSetOf<String>()

            context.contentResolver.openInputStream(source)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == NOTES_ENTRY -> {
                                notesJson = zip.readBytes().toString(Charsets.UTF_8)
                            }
                            entry.name.startsWith(ATTACHMENTS_DIR_IN_ZIP) -> {
                                val fileName = entry.name.removePrefix(ATTACHMENTS_DIR_IN_ZIP)
                                val destFile = File(attachmentsDir, fileName)
                                destFile.outputStream().use { out -> zip.copyTo(out) }
                                restoredFiles.add(fileName)
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return null

            val json = notesJson ?: return null
            val array = JSONArray(json)
            val restored = mutableListOf<Note>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val attachmentFile = obj.optString("attachmentFile", "").ifBlank { null }
                val attachmentPath = if (attachmentFile != null && restoredFiles.contains(attachmentFile)) {
                    File(attachmentsDir, attachmentFile).absolutePath
                } else null

                restored.add(
                    Note(
                        id = 0,
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        attachmentPath = attachmentPath,
                        attachmentName = obj.optString("attachmentName", "").ifBlank { null },
                        attachmentMimeType = obj.optString("attachmentMimeType", "").ifBlank { null }
                    )
                )
            }

            val dao = NoteDatabase.getInstance(context).noteDao()
            for (note in restored) {
                dao.insert(note)
            }
            restored.size
        } catch (e: Exception) {
            null
        }
    }
}
