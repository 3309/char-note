package com.example.charnotes

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.charnotes.ui.theme.CharNotesColors
import com.example.charnotes.ui.theme.CharNotesTheme
import com.example.charnotes.ui.theme.NoteCardShape
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: NoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CharNotesTheme {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharNotesColors.Ink)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharNotesColors.Surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CharNotesColors.Gold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Protect your notes",
                    style = MaterialTheme.typography.titleMedium,
                    color = CharNotesColors.TextInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Choose a password you'll remember — there's no recovery option.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharNotesColors.TextMuted
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = CharNotesColors.Ink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set password & continue")
                }
            }
        }
    }
}

@Composable
fun UnlockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CharNotesColors.Ink)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CharNotesColors.Surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = CharNotesColors.Gold,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "CharNotes",
                    style = MaterialTheme.typography.titleLarge,
                    color = CharNotesColors.TextInk
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Enter your password to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharNotesColors.TextMuted
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = {
                        if (PasswordManager.checkPassword(context, password)) {
                            onUnlocked()
                        } else {
                            error = "Incorrect password"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CharNotesColors.Ink),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Unlock")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(viewModel: NoteViewModel, onLock: () -> Unit) {
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current

    // null = dialog closed. A Note with id == 0L represents "new note".
    var editingNote by remember { mutableStateOf<Note?>(null) }
    // Note pending delete confirmation (null = no confirmation dialog showing).
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.exportBackup(uri) { success ->
            statusMessage = if (success) "Backup saved." else "Backup failed."
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.importBackup(uri) { count ->
            statusMessage = if (count != null) "Restored $count note(s)." else "Restore failed."
        }
    }

    Scaffold(
        containerColor = CharNotesColors.Paper,
        topBar = {
            TopAppBar(
                title = { Text("CharNotes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CharNotesColors.Ink,
                    titleContentColor = CharNotesColors.Paper,
                    actionIconContentColor = CharNotesColors.Paper
                ),
                actions = {
                    IconButton(onClick = onLock) {
                        Icon(Icons.Filled.Lock, contentDescription = "Lock app")
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Backup notes…") },
                            onClick = {
                                menuExpanded = false
                                val fileName = "charnotes_backup_${System.currentTimeMillis()}.zip"
                                exportLauncher.launch(fileName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share backup (Drive, email, etc.)…") },
                            onClick = {
                                menuExpanded = false
                                viewModel.prepareShareBackup { file ->
                                    if (file == null) {
                                        statusMessage = "Backup failed."
                                        return@prepareShareBackup
                                    }
                                    val uri = FileProvider.getUriForFile(
                                        context, "com.example.charnotes.fileprovider", file
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(sendIntent, "Share CharNotes backup")
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restore from backup…") },
                            onClick = {
                                menuExpanded = false
                                importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editingNote = Note(id = 0, title = "", content = "") },
                containerColor = CharNotesColors.Ink,
                contentColor = CharNotesColors.Paper,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New note") }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = CharNotesColors.Gold,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Nothing here yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = CharNotesColors.TextInk
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap \"New note\" below to write your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharNotesColors.TextMuted
                    )
                }
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
            viewModel = viewModel,
            onDismiss = { editingNote = null },
            onSave = { title, content, attachment, clearAttachment ->
                if (note.id == 0L) {
                    viewModel.addNote(title, content, attachment)
                } else {
                    viewModel.updateNote(note, title, content, attachment, clearAttachment)
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

    statusMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { statusMessage = null },
            title = { Text("Backup") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { statusMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val dateStr = remember(note.timestamp) {
        SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(note.timestamp))
    }
    Card(
        shape = NoteCardShape,
        colors = CardDefaults.cardColors(containerColor = CharNotesColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CharNotesColors.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // A thin "spine" strip, like the edge of a notebook page.
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(CharNotesColors.Ink)
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onClick)
                    ) {
                        if (note.title.isNotBlank()) {
                            Text(
                                note.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = CharNotesColors.TextInk
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                        }
                        Text(
                            note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharNotesColors.TextInk
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Created $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = CharNotesColors.TextMuted
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete note",
                            tint = CharNotesColors.TextMuted
                        )
                    }
                }
                note.attachmentPath?.let { path ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AttachmentPreview(
                        path = path,
                        name = note.attachmentName ?: "Attachment",
                        mimeType = note.attachmentMimeType,
                        onClick = { openAttachment(context, path, note.attachmentMimeType) }
                    )
                }
            }
        }
    }
}

/** Thumbnail for images, or a tappable filename chip for any other file type. */
@Composable
fun AttachmentPreview(path: String, name: String, mimeType: String?, onClick: () -> Unit) {
    val isImage = mimeType?.startsWith("image/") == true
    if (isImage) {
        val bitmap = remember(path) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClick)
            )
            return
        }
    }
    // Fallback: generic file chip (also used for non-image attachments).
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CharNotesColors.Paper)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = CharNotesColors.Gold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium, color = CharNotesColors.TextInk)
    }
}

/** Opens the attachment with whatever app the user has for that file type. */
fun openAttachment(context: android.content.Context, path: String, mimeType: String?) {
    val file = File(path)
    if (!file.exists()) return
    val uri: Uri = FileProvider.getUriForFile(context, "com.example.charnotes.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditDialog(
    note: Note,
    viewModel: NoteViewModel,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, attachment: Attachment?, clearAttachment: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }

    // Attachment state for this editing session.
    var currentPath by remember { mutableStateOf(note.attachmentPath) }
    var currentName by remember { mutableStateOf(note.attachmentName) }
    var currentMime by remember { mutableStateOf(note.attachmentMimeType) }
    var newAttachment by remember { mutableStateOf<Attachment?>(null) }
    var attachmentCleared by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        coroutineScope.launch {
            val result = viewModel.importAttachment(uri)
            isImporting = false
            if (result != null) {
                newAttachment = result
                attachmentCleared = false
                currentPath = result.path
                currentName = result.name
                currentMime = result.mimeType
            }
        }
    }

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
                    onValueChange = { content = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (currentPath != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            AttachmentPreview(
                                path = currentPath!!,
                                name = currentName ?: "Attachment",
                                mimeType = currentMime,
                                onClick = {}
                            )
                        }
                        IconButton(onClick = {
                            currentPath = null
                            currentName = null
                            currentMime = null
                            newAttachment = null
                            attachmentCleared = true
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove attachment")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { pickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CharNotesColors.Ink),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CharNotesColors.Ink),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AttachFile, contentDescription = null, tint = CharNotesColors.Gold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isImporting) "Adding attachment…" else "Attach a photo or file")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content, newAttachment, attachmentCleared) },
                enabled = content.trim().isNotEmpty() && !isImporting
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
