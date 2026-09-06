package com.example.charnotes

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note.
 *
 * A note can optionally carry one attachment (a picture or any other file), copied into
 * the app's private storage when it's picked so it keeps working even if the original
 * file is later moved or deleted.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Absolute path to the copied attachment file inside app-private storage, or null. */
    val attachmentPath: String? = null,
    /** Original display file name, e.g. "vacation.jpg", or null. */
    val attachmentName: String? = null,
    /** MIME type of the attachment, e.g. "image/jpeg", or null. */
    val attachmentMimeType: String? = null
)

/** Maximum number of characters allowed in a note's title. */
const val TITLE_CHAR_LIMIT = 50
