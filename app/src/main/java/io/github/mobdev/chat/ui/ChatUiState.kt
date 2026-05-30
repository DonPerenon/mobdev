package io.github.mobdev.chat.ui

import io.github.mobdev.chat.domain.ChatMessage

enum class PortraitScreen {
    Login,
    ChatList,
    Messages,
}

data class ChatUiState(
    val portraitScreen: PortraitScreen = PortraitScreen.Login,
    val username: String = "",
    val password: String = "",
    val channels: List<String> = emptyList(),
    val selectedChannel: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasMoreMessages: Boolean = true,
    val isLoadingChannels: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val isLoggingIn: Boolean = false,
    val messageInput: String = "",
    val fullImagePath: String? = null,
    val errorDialogMessage: String? = null,
    val skipLoginChecked: Boolean = false,
    val isOnline: Boolean = true,
)
