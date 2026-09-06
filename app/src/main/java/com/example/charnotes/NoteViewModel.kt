package com.example.charnotes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NoteDatabase.getInstance(application).noteDao()

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        viewModelScope.launch {
            dao.getAllNotes().collect { _notes.value = it }
        }
    }

    fun addNote(title: String, content: String) {
        val trimmedContent = content.trim().take(CHAR_LIMIT)
        val trimmedTitle = title.trim().take(TITLE_CHAR_LIMIT)
        if (trimmedContent.isEmpty()) return
        viewModelScope.launch {
            dao.insert(Note(title = trimmedTitle, content = trimmedContent))
        }
    }

    fun updateNote(note: Note, newTitle: String, newContent: String) {
        val trimmedContent = newContent.trim().take(CHAR_LIMIT)
        val trimmedTitle = newTitle.trim().take(TITLE_CHAR_LIMIT)
        if (trimmedContent.isEmpty()) return
        viewModelScope.launch {
            dao.update(note.copy(title = trimmedTitle, content = trimmedContent))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }
}
