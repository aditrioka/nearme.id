package id.nearme.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import id.nearme.app.domain.model.Chat

data class ChatDto(
    @DocumentId
    val id: String = "",
    val participants: List<String> = listOf(),
    val participantNames: Map<String, String> = mapOf(),
    val lastMessage: String = "",
    @ServerTimestamp
    val lastMessageTimestamp: Timestamp? = null,
    val unreadCounts: Map<String, Int> = mapOf() // Maps user IDs to their unread count
)

fun ChatDto.toDomainModel(currentUserId: String): Chat {
    return Chat(
        id = id,
        participants = participants,
        participantNames = participantNames,
        lastMessage = lastMessage,
        lastMessageTimestamp = lastMessageTimestamp?.toDate()?.time ?: System.currentTimeMillis(),
        unreadCount = unreadCounts[currentUserId] ?: 0
    )
}

fun Chat.toDto(unreadCounts: Map<String, Int>): ChatDto {
    return ChatDto(
        id = id,
        participants = participants,
        participantNames = participantNames,
        lastMessage = lastMessage,
        unreadCounts = unreadCounts
    )
}