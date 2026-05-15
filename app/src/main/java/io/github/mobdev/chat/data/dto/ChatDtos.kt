package io.github.mobdev.chat.data.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequestDto(
    val name: String,
    val pwd: String,
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    val id: Long?,
    val from: String,
    val to: String?,
    val data: MessageDataDto,
    val time: Long?,
)

@JsonClass(generateAdapter = true)
data class MessageDataDto(
    val Text: TextPayloadDto?,
    val Image: ImagePayloadDto?,
)

@JsonClass(generateAdapter = true)
data class TextPayloadDto(
    val text: String,
)

@JsonClass(generateAdapter = true)
data class ImagePayloadDto(
    val link: String?,
)

@JsonClass(generateAdapter = true)
data class OutgoingMessageDto(
    val from: String,
    val to: String,
    val data: MessageDataDto,
)
