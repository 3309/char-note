package com.example.charnotes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Lets other apps on the device (e.g. Tasker) create a note without opening the UI.
 *
 * Send a broadcast with:
 *   action: "com.example.charnotes.ADD_NOTE"
 *   extras: "title"   (optional, String)
 *           "content" (required, String)
 *
 * In Tasker: Action > Alert (no) > System > Send Intent, with:
 *   Action:       com.example.charnotes.ADD_NOTE
 *   Cat:          Default
 *   Target:       Broadcast Receiver
 *   Package:      com.example.charnotes
 *   Class:        com.example.charnotes.NoteReceiver
 *   Extra:        title:Your title here      (optional)
 *   Extra:        content:Your note text here
 *
 * Security note: this receiver is exported with no permission requirement, so any app on
 * the device that knows the action name can add a note this way, and notes added this way
 * skip the password lock screen entirely (there's no UI involved to lock). That's normal
 * for automation, but worth knowing if you're relying on the lock for privacy.
 */
class NoteReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ADD_NOTE) return

        val title = intent.getStringExtra(EXTRA_TITLE)?.trim()?.take(TITLE_CHAR_LIMIT) ?: ""
        val content = intent.getStringExtra(EXTRA_CONTENT)?.trim() ?: ""
        if (content.isEmpty()) return // nothing to save

        // BroadcastReceivers must finish quickly; goAsync() lets us do the DB write
        // off the main thread without the system killing the receiver early.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = NoteDatabase.getInstance(context.applicationContext).noteDao()
                dao.insert(Note(title = title, content = content))
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ADD_NOTE = "com.example.charnotes.ADD_NOTE"
        const val EXTRA_TITLE = "title"
        const val EXTRA_CONTENT = "content"
    }
}
