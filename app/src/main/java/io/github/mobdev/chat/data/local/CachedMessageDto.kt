package io.github.mobdev.chat.data.local

import com.squareup.moshi.JsonClass
import io.github.mobdev.chat.domain.ChatMessage
import io.github.mobdev.chat.domain.MessageContent

@JsonClass(generateAdapter = true)
data class CachedMessageDto(
    val id: String,
    val from: String,
    val to: String,
    val text: String?,
    val imageLink: String?,
    val time: String?,
    val isPending: Boolean = false,
)

fun ChatMessage.toCachedDto(): CachedMessageDto {
    val (text, imageLink) = when (val content = content) {
        is MessageContent.Text -> content.text to null
        is MessageContent.Image -> null to content.link
    }
    return CachedMessageDto(
        id = id,
        from = from,
        to = to,
        text = text,
        imageLink = imageLink,
        time = time,
        isPending = isPending,
    )
}

fun CachedMessageDto.toDomain(): ChatMessage? {
    val content = when {
        text != null -> MessageContent.Text(text)
        imageLink != null -> MessageContent.Image(imageLink)
        else -> return null
    }
    return ChatMessage(
        id = id,
        from = from,
        to = to,
        content = content,
        time = time,
        isPending = isPending,
    )
}
