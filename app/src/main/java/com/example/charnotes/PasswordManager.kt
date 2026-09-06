package com.example.charnotes

import android.content.Context
import java.security.MessageDigest

/**
 * A basic, fully-local app-lock. The password is never stored in plain text —
 * only a SHA-256 hash is saved in the app's private SharedPreferences.
 *
 * This is meant to keep casual snoopers (someone picking up your unlocked phone)
 * out of your notes. It is NOT strong security: SharedPreferences on a rooted or
 * compromised device isn't tamper-proof, and there's no password-recovery flow.
 * If you forget the password, clearing the app's storage (Settings > Apps >
 * CharNotes > Storage > Clear data) is the only way back in, and it deletes
 * all notes along with it.
 */
object PasswordManager {
    private const val PREFS_NAME = "char_notes_secure_prefs"
    private const val KEY_PASSWORD_HASH = "password_hash"

    fun isPasswordSet(context: Context): Boolean {
        return prefs(context).contains(KEY_PASSWORD_HASH)
    }

    fun setPassword(context: Context, password: String) {
        prefs(context).edit().putString(KEY_PASSWORD_HASH, hash(password)).apply()
    }

    fun checkPassword(context: Context, password: String): Boolean {
        val stored = prefs(context).getString(KEY_PASSWORD_HASH, null) ?: return false
        return stored == hash(password)
    }

    /** Removes the stored password (e.g. if you add a "forgot password" flow later). */
    fun clearPassword(context: Context) {
        prefs(context).edit().remove(KEY_PASSWORD_HASH).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
