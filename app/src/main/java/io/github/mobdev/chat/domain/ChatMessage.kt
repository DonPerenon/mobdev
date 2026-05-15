package io.github.mobdev.chat.domain

sealed interface MessageContent {
    data class Text(val text: String) : MessageContent
    data class Image(val link: String) : MessageContent
}

data class ChatMessage(
    val id: String,
    val from: String,
    val to: String,
    val content: MessageContent,
    val time: String?,
)
