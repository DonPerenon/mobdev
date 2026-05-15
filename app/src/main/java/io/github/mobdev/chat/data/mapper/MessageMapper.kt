package io.github.mobdev.chat.data.mapper

import io.github.mobdev.chat.data.dto.MessageDto
import io.github.mobdev.chat.domain.ChatMessage
import io.github.mobdev.chat.domain.MessageContent

fun MessageDto.toDomain(): ChatMessage? {
    val messageId = id?.toString() ?: return null
    val channel = to ?: return null
    val content = when {
        data.Text != null -> MessageContent.Text(data.Text.text)
        data.Image?.link != null -> MessageContent.Image(data.Image.link)
        else -> return null
    }
    return ChatMessage(
        id = messageId,
        from = from,
        to = channel,
        content = content,
        time = time?.toString(),
    )
}

fun messageIdAsLong(id: String): Long = id.toLongOrNull() ?: 0L
