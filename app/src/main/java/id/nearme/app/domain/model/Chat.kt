package id.nearme.app.domain.model

data class Chat(
    val id: String = "",
    val participants: List<String> = listOf(), // List of user IDs
    val participantNames: Map<String, String> = mapOf(), // Map of user IDs to display names
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)