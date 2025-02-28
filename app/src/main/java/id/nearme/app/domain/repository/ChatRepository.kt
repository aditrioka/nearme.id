package id.nearme.app.domain.repository

import id.nearme.app.domain.model.Chat
import id.nearme.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    suspend fun createChat(otherUserId: String, otherUserName: String): Result<Chat>
    
    suspend fun sendMessage(chatId: String, content: String): Result<Message>
    
    fun getUserChats(): Flow<List<Chat>>
    
    fun getChatMessages(chatId: String): Flow<List<Message>>
    
    suspend fun markMessagesAsRead(chatId: String): Result<Unit>
    
    suspend fun getChatWithUser(otherUserId: String): Result<Chat?>
}