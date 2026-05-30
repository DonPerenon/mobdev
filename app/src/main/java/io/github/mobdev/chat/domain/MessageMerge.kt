package io.github.mobdev.chat.domain

import io.github.mobdev.chat.data.local.PendingOutgoingMessage
import io.github.mobdev.chat.data.mapper.messageIdAsLong

fun mergeMessagesUnique(vararg lists: List<ChatMessage>): List<ChatMessage> {
    val byId = LinkedHashMap<String, ChatMessage>()
    lists.forEach { list ->
        list.forEach { message ->
            byId[message.id] = message
        }
    }
    return byId.values.sortedWith(compareBy({ it.isPending }, { messageIdAsLong(it.id) }))
}

fun PendingOutgoingMessage.toPendingChatMessage(): ChatMessage = ChatMessage(
    id = localId,
    from = from,
    to = channel,
    content = MessageContent.Text(text),
    time = createdAt.toString(),
    isPending = true,
)

fun List<PendingOutgoingMessage>.toPendingChatMessages(): List<ChatMessage> =
    map { it.toPendingChatMessage() }
