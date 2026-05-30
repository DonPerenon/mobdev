package io.github.mobdev.chat.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PendingOutgoingMessage(
    val localId: String,
    val channel: String,
    val from: String,
    val text: String,
    val createdAt: Long,
)
