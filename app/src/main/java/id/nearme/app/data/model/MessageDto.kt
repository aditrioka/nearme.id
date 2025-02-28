package id.nearme.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import id.nearme.app.domain.model.Message

data class MessageDto(
    @DocumentId
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)

fun MessageDto.toDomainModel(): Message {
    return Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        timestamp = timestamp?.toDate()?.time ?: System.currentTimeMillis(),
        isRead = isRead
    )
}

fun Message.toDto(): MessageDto {
    return MessageDto(
        id = id,
        chatId = chatId,
        senderId = senderId,
        senderName = senderName,
        content = content,
        isRead = isRead
    )
}