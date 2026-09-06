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

    fun addNote(content: String) {
        val trimmed = content.trim().take(CHAR_LIMIT)
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.insert(Note(content = trimmed)) }
    }

    fun updateNote(note: Note, newContent: String) {
        val trimmed = newContent.trim().take(CHAR_LIMIT)
        if (trimmed.isEmpty()) return
        viewModelScope.launch { dao.update(note.copy(content = trimmed)) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }
}
