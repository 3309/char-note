package com.example.charnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(viewModel)
                }
            }
        }
    }
}

/**
 * Top-level flow: decide whether to show the "create a password" screen,
 * the "enter password" lock screen, or the notes list.
 */
@Composable
fun AppRoot(viewModel: NoteViewModel) {
    val context = LocalContext.current
    var passwordIsSet by remember { mutableStateOf(PasswordManager.isPasswordSet(context)) }
    var unlocked by remember { mutableStateOf(false) }

    when {
        unlocked -> NotesScreen(
            viewModel = viewModel,
            onLock = { unlocked = false }
        )
        passwordIsSet -> UnlockScreen(
            onUnlocked = { unlocked = true }
        )
        else -> SetPasswordScreen(
            onPasswordSet = {
                passwordIsSet = true
                unlocked = true
            }
        )
    }
}

@Composable
fun SetPasswordScreen(onPasswordSet: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Set a password to protect your notes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    when {
                        password.isBlank() -> error = "Password can't be empty"
                        password != confirmPassword -> error = "Passwords don't match"
                        else -> {
                            PasswordManager.setPassword(context, password)
                            onPasswordSet()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Set password & continue")
            }
        }
    }
}

@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter your password", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (PasswordManager.checkPassword(context, password)) {
                        onUnlocked()
                    } else {
                        error = "Incorrect password"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel, onLock: () -> Unit) {
    val notes by viewModel.notes.collectAsState()

    // null = dialog closed. A Note with id == 0L represents "new note".
    var editingNote by remember { mutableStateOf<Note?>(null) }
    // Note pending delete confirmation (null = no confirmation dialog showing).
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CharNotes") },
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock app")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingNote = Note(id = 0, title = "", content = "") }) {
                Icon(Icons.Filled.Add, contentDescription = "Add note")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes yet. Tap + to add one (max $CHAR_LIMIT characters).")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { editingNote = note },
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        }
    }

    editingNote?.let { note ->
        NoteEditDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { title, content ->
                if (note.id == 0L) {
                    viewModel.addNote(title, content)
                } else {
                    viewModel.updateNote(note, title, content)
                }
                editingNote = null
            }
        )
    }

    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete this note?") },
            text = {
                Text(
                    if (note.title.isNotBlank()) "\"${note.title}\" will be deleted permanently."
                    else "This note will be deleted permanently."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateStr = remember(note.timestamp) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(note.timestamp))
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
            ) {
                if (note.title.isNotBlank()) {
                    Text(note.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(note.content, fontWeight = FontWeight.Normal)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Created $dateStr  •  ${note.content.length}/$CHAR_LIMIT",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete note")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(note: Note, onDismiss: () -> Unit, onSave: (title: String, content: String) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note.id == 0L) "New note" else "Edit note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= TITLE_CHAR_LIMIT) title = it },
                    placeholder = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= CHAR_LIMIT) content = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${content.length}/$CHAR_LIMIT",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = content.trim().isNotEmpty()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
