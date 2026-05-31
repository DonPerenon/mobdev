package io.github.mobdev.chat.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.mobdev.chat.domain.ChatMessage
import kotlinx.coroutines.flow.first

private val Context.chatLocalDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "chat_local_cache",
)

class ChatLocalStore(context: Context) {

    private val dataStore = context.chatLocalDataStore

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val channelsAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java),
    )

    private val messagesAdapter = moshi.adapter<List<CachedMessageDto>>(
        Types.newParameterizedType(List::class.java, CachedMessageDto::class.java),
    )

    private val pendingAdapter = moshi.adapter<List<PendingOutgoingMessage>>(
        Types.newParameterizedType(List::class.java, PendingOutgoingMessage::class.java),
    )

    suspend fun saveChannels(channels: List<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_CHANNELS] = channelsAdapter.toJson(channels)
        }
    }

    suspend fun getChannels(): List<String> {
        val json = dataStore.data.first()[KEY_CHANNELS] ?: return emptyList()
        return channelsAdapter.fromJson(json).orEmpty()
    }

    suspend fun saveMessages(channel: String, messages: List<ChatMessage>) {
        val dtoList = messages
            .filter { !it.isPending }
            .map { it.toCachedDto() }
        dataStore.edit { prefs ->
            prefs[channelKey(channel)] = messagesAdapter.toJson(dtoList)
        }
    }

    suspend fun getMessages(channel: String): List<ChatMessage> {
        val json = dataStore.data.first()[channelKey(channel)] ?: return emptyList()
        return messagesAdapter.fromJson(json)
            .orEmpty()
            .mapNotNull { it.toDomain() }
    }

    suspend fun getPendingMessages(): List<PendingOutgoingMessage> {
        val json = dataStore.data.first()[KEY_PENDING] ?: return emptyList()
        return readPendingList(json)
    }

    private fun readPendingList(json: String?): List<PendingOutgoingMessage> {
        if (json.isNullOrBlank()) return emptyList()
        return pendingAdapter.fromJson(json).orEmpty()
    }

    suspend fun getPendingMessagesForChannel(channel: String): List<PendingOutgoingMessage> {
        return getPendingMessages().filter { it.channel == channel }
    }

    suspend fun addPendingMessage(message: PendingOutgoingMessage) {
        dataStore.edit { prefs ->
            val current = readPendingList(prefs[KEY_PENDING])
            prefs[KEY_PENDING] = pendingAdapter.toJson(current + message)
        }
    }

    suspend fun removePendingMessage(localId: String) {
        dataStore.edit { prefs ->
            val current = readPendingList(prefs[KEY_PENDING])
            prefs[KEY_PENDING] = pendingAdapter.toJson(
                current.filterNot { it.localId == localId },
            )
        }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private fun channelKey(channel: String): Preferences.Key<String> =
        stringPreferencesKey("messages_${channel.replace('@', '_')}")

    private companion object {
        val KEY_CHANNELS = stringPreferencesKey("channels")
        val KEY_PENDING = stringPreferencesKey("pending_outgoing")
    }
}
