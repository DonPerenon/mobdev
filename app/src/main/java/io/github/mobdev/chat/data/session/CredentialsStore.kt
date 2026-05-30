package io.github.mobdev.chat.data.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.credentialsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chat_credentials",
)

data class SavedCredentials(
    val username: String,
    val password: String,
)

class CredentialsStore(private val context: Context) {

    val savedCredentials: Flow<SavedCredentials?> = context.credentialsDataStore.data.map { prefs ->
        val username = prefs[KEY_USERNAME]
        val password = prefs[KEY_PASSWORD]
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            null
        } else {
            SavedCredentials(username, password)
        }
    }

    suspend fun save(username: String, password: String) {
        context.credentialsDataStore.edit { prefs ->
            prefs[KEY_USERNAME] = username
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.credentialsDataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun getAuthToken(): String? {
        val prefs = context.credentialsDataStore.data.first()
        return prefs[KEY_AUTH_TOKEN]?.takeIf { it.isNotBlank() }
    }

    suspend fun clear() {
        context.credentialsDataStore.edit { prefs ->
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_PASSWORD)
            prefs.remove(KEY_AUTH_TOKEN)
        }
    }

    private companion object {
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_PASSWORD = stringPreferencesKey("password")
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
    }
}
