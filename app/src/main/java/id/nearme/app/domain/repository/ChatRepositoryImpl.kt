package id.nearme.app.domain.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import id.nearme.app.data.model.ChatDto
import id.nearme.app.data.model.MessageDto
import id.nearme.app.data.model.toDomainModel
import id.nearme.app.data.model.toDto
import id.nearme.app.domain.model.Chat
import id.nearme.app.domain.model.Message
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ChatRepository {

    private val chatsCollection = firestore.collection("chats")
    private val messagesCollection = firestore.collection("messages")

    override suspend fun createChat(otherUserId: String, otherUserName: String): Result<Chat> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Check if a chat already exists with these participants
            val existingChat = getChatWithUser(otherUserId).getOrNull()
            if (existingChat != null) {
                return Result.success(existingChat)
            }

            // Create a new chat
            val chatId = Uuid.random().toString()
            val currentUserName = userRepository.getCurrentDisplayName()
            
            val participants = listOf(currentUser.uid, otherUserId)
            val participantNames = mapOf(
                currentUser.uid to currentUserName,
                otherUserId to otherUserName
            )
            
            val unreadCounts = mapOf(
                currentUser.uid to 0,
                otherUserId to 0
            )
            
            val chatDto = ChatDto(
                id = chatId,
                participants = participants,
                participantNames = participantNames,
                lastMessage = "",
                unreadCounts = unreadCounts
            )

            chatsCollection.document(chatId).set(chatDto).await()

            Result.success(chatDto.toDomainModel(currentUser.uid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(chatId: String, content: String): Result<Message> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Get the chat to update last message and unread counts
            val chatDoc = chatsCollection.document(chatId).get().await()
            val chatDto = chatDoc.toObject(ChatDto::class.java)
                ?: return Result.failure(IllegalStateException("Chat not found"))

            // Create the message
            val messageId = Uuid.random().toString()
            val messageDto = MessageDto(
                id = messageId,
                chatId = chatId,
                senderId = currentUser.uid,
                senderName = userRepository.getCurrentDisplayName(),
                content = content,
                isRead = false
            )

            // Add the message to Firestore
            messagesCollection.document(messageId).set(messageDto).await()

            // Update the chat with last message and increment unread counts for other participants
            val updatedUnreadCounts = chatDto.unreadCounts.toMutableMap()
            chatDto.participants.forEach { userId ->
                if (userId != currentUser.uid) {
                    updatedUnreadCounts[userId] = (updatedUnreadCounts[userId] ?: 0) + 1
                }
            }

            val updatedChat = chatDto.copy(
                lastMessage = content,
                unreadCounts = updatedUnreadCounts
            )

            chatsCollection.document(chatId).set(updatedChat).await()

            Result.success(messageDto.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getUserChats(): Flow<List<Chat>> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            close(IllegalStateException("User not authenticated"))
            return@callbackFlow
        }

        val listener = chatsCollection
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val chats = snapshot.documents.mapNotNull { document ->
                        document.toObject(ChatDto::class.java)?.toDomainModel(currentUser.uid)
                    }
                    
                    trySend(chats)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun getChatMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = messagesCollection
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { document ->
                        document.toObject(MessageDto::class.java)?.toDomainModel()
                    }
                    
                    trySend(messages)
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Update the unread count for the current user in the chat
            val chatRef = chatsCollection.document(chatId)
            val chatDoc = chatRef.get().await()
            val chatDto = chatDoc.toObject(ChatDto::class.java)
                ?: return Result.failure(IllegalStateException("Chat not found"))

            // Reset unread count for current user
            val updatedUnreadCounts = chatDto.unreadCounts.toMutableMap()
            updatedUnreadCounts[currentUser.uid] = 0

            chatRef.update("unreadCounts", updatedUnreadCounts).await()

            // Mark messages as read
            val batch = firestore.batch()
            val messages = messagesCollection
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUser.uid)
                .get()
                .await()

            messages.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }

            if (messages.documents.isNotEmpty()) {
                batch.commit().await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getChatWithUser(otherUserId: String): Result<Chat?> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            // Find chats where both users are participants
            val querySnapshot = chatsCollection
                .whereArrayContains("participants", currentUser.uid)
                .get()
                .await()

            // Find the chat that contains the other user
            val chat = querySnapshot.documents
                .mapNotNull { it.toObject(ChatDto::class.java) }
                .find { it.participants.contains(otherUserId) }
                ?.toDomainModel(currentUser.uid)

            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}