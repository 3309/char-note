package com.example.charnotes

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single note. `content` is enforced (in the UI) to never exceed
 * [CHAR_LIMIT] characters, so notes stay short and scannable.
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/** Maximum number of characters allowed per note's content. Change this to taste. */
const val CHAR_LIMIT = 200

/** Maximum number of characters allowed in a note's title. */
const val TITLE_CHAR_LIMIT = 50
