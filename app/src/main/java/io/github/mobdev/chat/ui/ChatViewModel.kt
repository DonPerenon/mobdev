package io.github.mobdev.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.mobdev.chat.data.mapper.messageIdAsLong
import io.github.mobdev.chat.data.repository.ChatRepository
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
        viewModelScope.launch {
            repository.unauthorized.collect {
                onUnauthorized()
            }
        }
        viewModelScope.launch {
            val saved = credentialsStore.savedCredentials.first()
            if (saved != null) {
                _uiState.update {
                    it.copy(
                        username = saved.username,
                        password = saved.password,
                        portraitScreen = PortraitScreen.ChatList,
                        skipLoginChecked = true,
                    )
                }
                performLogin(saved.username, saved.password, navigateOnSuccess = false)
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
            performLogin(username, password, navigateOnSuccess = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
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
        val channel = _uiState.value.selectedChannel ?: return
        val state = _uiState.value
        if (!state.hasMoreMessages || state.isLoadingMore || state.isLoadingMessages) return
        val oldestId = state.messages.minOfOrNull { messageIdAsLong(it.id) } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.fetchMessages(
                channel = channel,
                lastKnownId = oldestId,
                reverse = true,
                limit = ChatRepository.PAGE_SIZE,
            )
                .onSuccess { older ->
                    if (older.isEmpty()) {
                        _uiState.update { it.copy(hasMoreMessages = false, isLoadingMore = false) }
                    } else {
                        val merged = mergeMessages(older, _uiState.value.messages)
                        _uiState.update {
                            it.copy(
                                messages = merged,
                                hasMoreMessages = older.size >= ChatRepository.PAGE_SIZE,
                                isLoadingMore = false,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMore = false) }
                    showError(mapError(error))
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
                    refreshLatestMessages(channel)
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
                ensureChannelsLoaded()
            }
            .onFailure { error ->
                _uiState.update { it.copy(isLoggingIn = false) }
                if (error is ChatRepository.HttpException && error.code == HTTP_UNAUTHORIZED) {
                    showError(ERROR_INVALID_CREDENTIALS)
                    _uiState.update { it.copy(portraitScreen = PortraitScreen.Login) }
                } else {
                    showError(mapError(error))
                }
            }
    }

    private fun ensureChannelsLoaded() {
        if (channelsLoaded) return
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
                    _uiState.update { it.copy(isLoadingChannels = false) }
                    showError(mapError(error))
                }
        }
    }

    private fun loadMessages(channel: String, reset: Boolean) {
        viewModelScope.launch {
            if (reset) {
                _uiState.update {
                    it.copy(
                        isLoadingMessages = true,
                        messages = emptyList(),
                        hasMoreMessages = true,
                    )
                }
            }
            repository.fetchMessages(channel = channel)
                .onSuccess { messages ->
                    messagesLoadedForChannel = channel
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoadingMessages = false,
                            hasMoreMessages = messages.size >= ChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingMessages = false) }
                    showError(mapError(error))
                }
        }
    }

    private fun refreshLatestMessages(channel: String) {
        viewModelScope.launch {
            repository.fetchMessages(channel = channel)
                .onSuccess { messages ->
                    messagesLoadedForChannel = channel
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            hasMoreMessages = messages.size >= ChatRepository.PAGE_SIZE,
                        )
                    }
                }
                .onFailure { error -> showError(mapError(error)) }
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

    private fun mergeMessages(older: List<ChatMessage>, current: List<ChatMessage>): List<ChatMessage> {
        val byId = LinkedHashMap<String, ChatMessage>()
        older.forEach { byId[it.id] = it }
        current.forEach { byId[it.id] = it }
        return byId.values.sortedBy { messageIdAsLong(it.id) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorDialogMessage = message) }
    }

    private fun mapError(error: Throwable): String = when (error) {
        is ChatRepository.HttpException -> when (error.code) {
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
    }
}
