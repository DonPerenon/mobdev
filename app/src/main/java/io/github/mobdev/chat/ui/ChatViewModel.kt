package io.github.mobdev.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.chat.data.mapper.messageIdAsLong
import io.github.mobdev.chat.data.repository.ChatRepository
import io.github.mobdev.chat.data.repository.HttpException
import io.github.mobdev.chat.data.repository.RemoteChatRepository
import io.github.mobdev.chat.data.session.CredentialsStore
import io.github.mobdev.chat.domain.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val credentialsStore: CredentialsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesLoadedForChannel: String? = null
    private var channelsLoaded: Boolean = false

    init {
        _uiState.update { it.copy(isOnline = repository.isOnline.value) }
        viewModelScope.launch {
            repository.unauthorized.collect {
                onUnauthorized()
            }
        }
        viewModelScope.launch {
            repository.isOnline.collect { online ->
                val wasOffline = !_uiState.value.isOnline
                _uiState.update { it.copy(isOnline = online) }
                if (online && wasOffline) {
                    syncAfterReconnect()
                }
            }
        }
        viewModelScope.launch {
            val saved = credentialsStore.savedCredentials.first()
            if (saved != null) {
                credentialsStore.getAuthToken()?.let { repository.restoreAuthToken(it) }
                val cachedChannels = repository.getCachedChannels()
                _uiState.update {
                    it.copy(
                        username = saved.username,
                        password = saved.password,
                        portraitScreen = PortraitScreen.ChatList,
                        skipLoginChecked = true,
                        channels = cachedChannels,
                    )
                }
                if (!repository.isOnline.value && cachedChannels.isNotEmpty()) {
                    channelsLoaded = true
                } else {
                    performLogin(saved.username, saved.password, navigateOnSuccess = false)
                }
            } else {
                _uiState.update { it.copy(skipLoginChecked = true) }
            }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onMessageInputChange(value: String) {
        _uiState.update { it.copy(messageInput = value) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorDialogMessage = null) }
    }

    fun login() {
        val state = _uiState.value
        val username = state.username.trim()
        val password = state.password
        if (username.isEmpty() || password.isEmpty()) {
            showError(ERROR_EMPTY_CREDENTIALS)
            return
        }
        viewModelScope.launch {
            repository.networkMonitor.refresh()
            if (!repository.isOnline.value &&
                tryEnterOfflineMode(username, password, navigateOnSuccess = true)
            ) {
                return@launch
            }
            performLogin(username, password, navigateOnSuccess = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            credentialsStore.clear()
            channelsLoaded = false
            messagesLoadedForChannel = null
            _uiState.update {
                it.copy(
                    portraitScreen = PortraitScreen.Login,
                    selectedChannel = null,
                    fullImagePath = null,
                    channels = emptyList(),
                    messages = emptyList(),
                    messageInput = "",
                    hasMoreMessages = true,
                )
            }
        }
    }

    fun openChannel(channel: String) {
        _uiState.update {
            it.copy(
                selectedChannel = channel,
                portraitScreen = PortraitScreen.Messages,
                fullImagePath = null,
            )
        }
        if (messagesLoadedForChannel != channel) {
            loadMessages(channel, reset = true)
        }
    }

    fun closeChannel() {
        _uiState.update {
            it.copy(
                selectedChannel = null,
                portraitScreen = PortraitScreen.ChatList,
                fullImagePath = null,
            )
        }
    }

    fun navigateToChatList() {
        _uiState.update {
            it.copy(
                portraitScreen = PortraitScreen.ChatList,
                fullImagePath = null,
            )
        }
    }

    fun openFullImage(path: String) {
        _uiState.update { it.copy(fullImagePath = path) }
    }

    fun closeFullImage() {
        _uiState.update { it.copy(fullImagePath = null) }
    }

    fun loadMoreMessages() {
        if (!repository.isOnline.value) return
        val channel = _uiState.value.selectedChannel ?: return
        val state = _uiState.value
        if (!state.hasMoreMessages || state.isLoadingMore || state.isLoadingMessages) return
        val oldestId = state.messages
            .filter { !it.isPending }
            .minOfOrNull { messageIdAsLong(it.id) } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.fetchMessages(
                channel = channel,
                lastKnownId = oldestId,
                reverse = true,
                limit = RemoteChatRepository.PAGE_SIZE,
            )
                .onSuccess { merged ->
                    if (merged.filter { !it.isPending }.size <= state.messages.filter { !it.isPending }.size) {
                        _uiState.update { it.copy(hasMoreMessages = false, isLoadingMore = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                messages = merged,
                                hasMoreMessages = true,
                                isLoadingMore = false,
                            )
                        }
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoadingMore = false) }
                    showErrorIfNoCache(ERROR_NETWORK)
                }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val channel = state.selectedChannel ?: return
        val text = state.messageInput.trim()
        if (text.isEmpty()) return
        val username = state.username.trim()
        if (username.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            repository.sendTextMessage(username, channel, text)
                .onSuccess {
                    _uiState.update { it.copy(messageInput = "", isSending = false) }
                    if (repository.isOnline.value) {
                        refreshLatestMessages(channel)
                    } else {
                        applyCachedMessages(channel)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isSending = false) }
                    showError(mapError(error))
                }
        }
    }

    fun imageUrl(path: String, fullResolution: Boolean): String =
        repository.imageUrl(path, fullResolution)

    private suspend fun performLogin(
        username: String,
        password: String,
        navigateOnSuccess: Boolean,
    ) {
        _uiState.update { it.copy(isLoggingIn = true) }
        repository.login(username, password)
            .onSuccess {
                credentialsStore.save(username, password)
                repository.currentAuthToken()?.let { credentialsStore.saveAuthToken(it) }
                _uiState.update {
                    it.copy(
                        username = username,
                        password = password,
                        isLoggingIn = false,
                        portraitScreen = if (navigateOnSuccess) {
                            PortraitScreen.ChatList
                        } else {
                            it.portraitScreen
                        },
                    )
                }
                ensureChannelsLoaded(force = true)
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoggingIn = false) }
                when {
                    error is HttpException && error.code == HTTP_UNAUTHORIZED -> {
                        showError(ERROR_INVALID_CREDENTIALS)
                        _uiState.update { it.copy(portraitScreen = PortraitScreen.Login) }
                    }
                    error is ChatRepository.OfflineException -> {
                        if (!tryEnterOfflineMode(username, password, navigateOnSuccess)) {
                            if (_uiState.value.channels.isEmpty()) {
                                showError(ERROR_OFFLINE)
                            } else {
                                channelsLoaded = true
                            }
                        }
                    }
                    else -> showError(mapError(error))
                }
            }
    }

    private suspend fun tryEnterOfflineMode(
        username: String,
        password: String,
        navigateOnSuccess: Boolean,
    ): Boolean {
        if (repository.isOnline.value) return false
        val saved = credentialsStore.savedCredentials.first() ?: return false
        if (saved.username != username || saved.password != password) return false
        val cached = repository.getCachedChannels()
        if (cached.isEmpty()) return false
        credentialsStore.getAuthToken()?.let { repository.restoreAuthToken(it) }
        credentialsStore.save(username, password)
        _uiState.update {
            it.copy(
                username = username,
                password = password,
                channels = cached,
                isLoggingIn = false,
                portraitScreen = if (navigateOnSuccess || it.portraitScreen != PortraitScreen.Login) {
                    PortraitScreen.ChatList
                } else {
                    it.portraitScreen
                },
            )
        }
        channelsLoaded = true
        return true
    }

    private fun ensureChannelsLoaded(force: Boolean = false) {
        if (channelsLoaded && !force) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChannels = true) }
            repository.fetchChannels()
                .onSuccess { channels ->
                    channelsLoaded = true
                    _uiState.update {
                        it.copy(
                            channels = channels,
                            isLoadingChannels = false,
                        )
                    }
                }
                .onFailure { error ->
                    val cached = repository.getCachedChannels()
                    channelsLoaded = cached.isNotEmpty()
                    _uiState.update {
                        it.copy(
                            channels = cached.ifEmpty { it.channels },
                            isLoadingChannels = false,
                        )
                    }
                    if (cached.isEmpty()) {
                        showError(mapError(error))
                    }
                }
        }
    }

    private fun loadMessages(channel: String, reset: Boolean) {
        viewModelScope.launch {
            val cached = repository.loadChannelMessages(channel)
            if (reset) {
                _uiState.update {
                    it.copy(
                        messages = cached,
                        isLoadingMessages = repository.isOnline.value && cached.isEmpty(),
                        hasMoreMessages = true,
                    )
                }
            }
            if (!repository.isOnline.value) {
                messagesLoadedForChannel = channel
                _uiState.update { it.copy(isLoadingMessages = false) }
                return@launch
            }
            repository.fetchMessages(channel = channel)
                .onSuccess { messages ->
                    messagesLoadedForChannel = channel
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoadingMessages = false,
                            hasMoreMessages = serverMessageCount(messages) >= RemoteChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingMessages = false,
                            messages = cached.ifEmpty { it.messages },
                        )
                    }
                    if (cached.isEmpty()) {
                        showError(mapError(error))
                    }
                }
        }
    }

    private fun refreshLatestMessages(channel: String) {
        viewModelScope.launch {
            if (!repository.isOnline.value) {
                applyCachedMessages(channel)
                return@launch
            }
            repository.fetchMessages(channel = channel)
                .onSuccess { messages ->
                    messagesLoadedForChannel = channel
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            hasMoreMessages = serverMessageCount(messages) >= RemoteChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { showErrorIfNoCache(ERROR_NETWORK) }
        }
    }

    private suspend fun applyCachedMessages(channel: String) {
        val messages = repository.loadChannelMessages(channel)
        messagesLoadedForChannel = channel
        _uiState.update { it.copy(messages = messages) }
    }

    private fun syncAfterReconnect() {
        viewModelScope.launch {
            repository.flushPendingMessages()
                .onFailure { showErrorIfNoCache(ERROR_NETWORK) }
            if (_uiState.value.portraitScreen != PortraitScreen.Login) {
                ensureChannelsLoaded(force = true)
                _uiState.value.selectedChannel?.let { refreshLatestMessages(it) }
            }
        }
    }

    private fun onUnauthorized() {
        channelsLoaded = false
        messagesLoadedForChannel = null
        _uiState.update {
            it.copy(
                portraitScreen = PortraitScreen.Login,
                selectedChannel = null,
                fullImagePath = null,
                channels = emptyList(),
                messages = emptyList(),
                messageInput = "",
                hasMoreMessages = true,
            )
        }
    }

    private fun serverMessageCount(messages: List<ChatMessage>): Int =
        messages.count { !it.isPending }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorDialogMessage = message) }
    }

    private fun showErrorIfNoCache(message: String) {
        if (_uiState.value.messages.isEmpty() && _uiState.value.channels.isEmpty()) {
            showError(message)
        }
    }

    private fun mapError(error: Throwable): String = when (error) {
        is ChatRepository.OfflineException -> ERROR_OFFLINE
        is HttpException -> when (error.code) {
            HTTP_UNAUTHORIZED -> ERROR_UNAUTHORIZED
            else -> ERROR_NETWORK
        }
        else -> ERROR_NETWORK
    }

    class Factory(
        private val repository: ChatRepository,
        private val credentialsStore: CredentialsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(repository, credentialsStore) as T
        }
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        const val ERROR_EMPTY_CREDENTIALS = "EMPTY"
        const val ERROR_INVALID_CREDENTIALS = "INVALID"
        const val ERROR_UNAUTHORIZED = "UNAUTHORIZED"
        const val ERROR_NETWORK = "NETWORK"
        const val ERROR_OFFLINE = "OFFLINE"
    }
}
