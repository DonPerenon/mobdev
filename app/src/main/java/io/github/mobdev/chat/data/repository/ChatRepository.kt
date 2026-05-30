package io.github.mobdev.chat.data.repository

import android.content.Context
import io.github.mobdev.chat.data.local.ChatLocalStore
import io.github.mobdev.chat.data.local.PendingOutgoingMessage
import io.github.mobdev.chat.data.network.NetworkMonitor
import io.github.mobdev.chat.data.session.SessionManager
import io.github.mobdev.chat.domain.ChatMessage
import io.github.mobdev.chat.domain.mergeMessagesUnique
import io.github.mobdev.chat.domain.toPendingChatMessage
import io.github.mobdev.chat.domain.toPendingChatMessages
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import java.util.UUID

class ChatRepository(
    private val remote: RemoteChatRepository,
    private val local: ChatLocalStore,
    val networkMonitor: NetworkMonitor,
    private val sessionManager: SessionManager,
) {

    val unauthorized = remote.unauthorized
    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    fun imageUrl(path: String, fullResolution: Boolean): String =
        remote.imageUrl(path, fullResolution)

    fun restoreAuthToken(token: String) {
        sessionManager.setToken(token)
    }

    fun currentAuthToken(): String? = sessionManager.authToken

    suspend fun login(username: String, password: String): Result<Unit> {
        networkMonitor.refresh()
        return remote.login(username, password)
    }

    suspend fun logout(): Result<Unit> {
        val result = if (networkMonitor.isOnline.value) {
            remote.logout()
        } else {
            Result.success(Unit)
        }
        local.clearAll()
        return result
    }

    suspend fun fetchChannels(): Result<List<String>> {
        if (!networkMonitor.isOnline.value) {
            val cached = local.getChannels()
            return if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(OfflineException())
            }
        }
        return remote.fetchChannels()
            .onSuccess { local.saveChannels(it) }
            .recoverCatching { error ->
                val cached = local.getChannels()
                if (cached.isNotEmpty()) cached else throw error
            }
    }

    suspend fun getCachedChannels(): List<String> = local.getChannels()

    suspend fun loadChannelMessages(channel: String): List<ChatMessage> {
        val cached = local.getMessages(channel)
        val pending = local.getPendingMessagesForChannel(channel).toPendingChatMessages()
        return mergeMessagesUnique(cached, pending)
    }

    suspend fun fetchMessages(
        channel: String,
        lastKnownId: Long = RemoteChatRepository.INITIAL_LAST_KNOWN_ID,
        reverse: Boolean = true,
        limit: Int = RemoteChatRepository.PAGE_SIZE,
    ): Result<List<ChatMessage>> {
        if (!networkMonitor.isOnline.value) {
            val merged = loadChannelMessages(channel)
            return if (merged.isNotEmpty()) {
                Result.success(merged)
            } else {
                Result.failure(OfflineException())
            }
        }
        return remote.fetchMessages(
            channel = channel,
            lastKnownId = lastKnownId,
            reverse = reverse,
            limit = limit,
        ).onSuccess { page ->
            if (!reverse || lastKnownId == RemoteChatRepository.INITIAL_LAST_KNOWN_ID) {
                local.saveMessages(channel, page)
            } else {
                val existing = local.getMessages(channel)
                local.saveMessages(channel, mergeMessagesUnique(existing, page))
            }
        }.recoverCatching { error ->
            val merged = loadChannelMessages(channel)
            if (merged.isNotEmpty()) merged else throw error
        }.map {
            loadChannelMessages(channel)
        }
    }

    suspend fun sendTextMessage(
        username: String,
        channel: String,
        text: String,
    ): Result<Unit> {
        if (!networkMonitor.isOnline.value) {
            val pending = PendingOutgoingMessage(
                localId = "pending-${UUID.randomUUID()}",
                channel = channel,
                from = username,
                text = text,
                createdAt = System.currentTimeMillis(),
            )
            local.addPendingMessage(pending)
            return Result.success(Unit)
        }
        return remote.sendTextMessage(username, channel, text)
            .onSuccess { refreshAfterSend(channel) }
    }

    private suspend fun refreshAfterSend(channel: String) {
        remote.fetchMessages(channel = channel)
            .onSuccess { local.saveMessages(channel, it) }
    }

    suspend fun flushPendingMessages(): Result<Unit> = runCatching {
        if (!networkMonitor.isOnline.value) return@runCatching
        val pending = local.getPendingMessages()
        for (message in pending) {
            remote.sendTextMessage(message.from, message.channel, message.text).getOrThrow()
            local.removePendingMessage(message.localId)
        }
    }

    class OfflineException : IOException("offline")

    companion object {
        fun create(context: Context, sessionManager: SessionManager): ChatRepository {
            val appContext = context.applicationContext
            val networkMonitor = NetworkMonitor(appContext)
            networkMonitor.start()
            return ChatRepository(
                remote = RemoteChatRepository.create(sessionManager),
                local = ChatLocalStore(appContext),
                networkMonitor = networkMonitor,
                sessionManager = sessionManager,
            )
        }
    }
}

typealias HttpException = RemoteChatRepository.HttpException
