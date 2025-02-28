package id.nearme.app.presentation.chat

import id.nearme.app.domain.model.Chat
import id.nearme.app.domain.model.Message

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatDetailUiState(
    val chatId: String = "",
    val otherUserName: String = "",
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)