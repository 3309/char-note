package com.example.charnotes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Result of copying a picked attachment into app-private storage. */
data class Attachment(
    val path: String,
    val name: String,
    val mimeType: String?
)

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getInstance(application).noteDao()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        viewModelScope.launch {
            dao.getAllNotes().collect { _notes.value = it }
        }
    }

    /**
     * Copies whatever the user picked (via a document/photo picker) into the app's
     * private files directory, so the note keeps working even if the original is
     * deleted or moved. Returns null if the copy failed.
     */
    suspend fun importAttachment(uri: Uri): Attachment? = withContext(Dispatchers.IO) {
        val context = getApplication<Application>()
        val resolver = context.contentResolver
        try {
            val displayName = queryDisplayName(uri) ?: "attachment_${System.currentTimeMillis()}"
            val mimeType = resolver.getType(uri)

            val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
            val destFile = File(attachmentsDir, "${UUID.randomUUID()}_$displayName")

            resolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null

            Attachment(path = destFile.absolutePath, name = displayName, mimeType = mimeType)
        } catch (e: Exception) {
            null
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val context = getApplication<Application>()
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun addNote(title: String, content: String, attachment: Attachment?) {
        val trimmedContent = content.trim()
        val trimmedTitle = title.trim().take(TITLE_CHAR_LIMIT)
        if (trimmedContent.isEmpty()) return
        viewModelScope.launch {
            dao.insert(
                Note(
                    title = trimmedTitle,
                    content = trimmedContent,
                    attachmentPath = attachment?.path,
                    attachmentName = attachment?.name,
                    attachmentMimeType = attachment?.mimeType
                )
            )
        }
    }

    /**
     * @param attachment pass the current/new attachment to keep or set one, or explicitly
     *   pass null via [clearAttachment] = true to remove an existing attachment.
     */
    fun updateNote(
        note: Note,
        newTitle: String,
        newContent: String,
        attachment: Attachment?,
        clearAttachment: Boolean
    ) {
        val trimmedContent = newContent.trim()
        val trimmedTitle = newTitle.trim().take(TITLE_CHAR_LIMIT)
        if (trimmedContent.isEmpty()) return

        val oldPath = note.attachmentPath
        val updated = when {
            attachment != null -> note.copy(
                title = trimmedTitle,
                content = trimmedContent,
                attachmentPath = attachment.path,
                attachmentName = attachment.name,
                attachmentMimeType = attachment.mimeType
            )
            clearAttachment -> note.copy(
                title = trimmedTitle,
                content = trimmedContent,
                attachmentPath = null,
                attachmentName = null,
                attachmentMimeType = null
            )
            else -> note.copy(title = trimmedTitle, content = trimmedContent)
        }

        viewModelScope.launch {
            dao.update(updated)
            // Clean up the old file on disk if it was replaced or removed.
            if (oldPath != null && oldPath != updated.attachmentPath) {
                withContext(Dispatchers.IO) { File(oldPath).delete() }
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.delete(note)
            note.attachmentPath?.let { path ->
                withContext(Dispatchers.IO) { File(path).delete() }
            }
        }
    }

    /** Exports every note (and its attachment) into a single backup .zip at [destination]. */
    fun exportBackup(destination: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val currentNotes = _notes.value
            val success = withContext(Dispatchers.IO) {
                BackupManager.exportBackup(context, destination, currentNotes)
            }
            onResult(success)
        }
    }

    /** Imports notes (and attachments) from a backup .zip previously created by [exportBackup]. */
    fun importBackup(source: Uri, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val restoredCount = withContext(Dispatchers.IO) {
                BackupManager.importBackup(context, source)
            }
            onResult(restoredCount)
        }
    }
}
