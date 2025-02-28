package id.nearme.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nearme.app.domain.repository.ChatRepository
import id.nearme.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState(
        currentUserId = userRepository.getCurrentUserId()
    ))
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()
    
    // Chat ID resolved from navigation or from finding/creating a chat
    private var resolvedChatId: String? = null

    /**
     * Initialize chat - either directly with chat ID or by finding/creating a chat with otherUserId
     */
    fun initialize(chatId: String? = null, otherUserId: String? = null, otherUserName: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // If we already have a chat ID, use it directly
                if (!chatId.isNullOrEmpty()) {
                    resolvedChatId = chatId
                    loadMessages(chatId)
                    return@launch
                }
                
                // Otherwise try to find or create a chat with the other user
                if (!otherUserId.isNullOrEmpty() && !otherUserName.isNullOrEmpty()) {
                    // First check if a chat already exists with this user
                    val existingChatResult = chatRepository.getChatWithUser(otherUserId)
                    val existingChat = existingChatResult.getOrNull()
                    
                    if (existingChat != null) {
                        // Chat already exists, use it
                        resolvedChatId = existingChat.id
                        loadMessages(existingChat.id)
                    } else {
                        // Create a new chat
                        val newChatResult = chatRepository.createChat(otherUserId, otherUserName)
                        val newChat = newChatResult.getOrThrow()
                        resolvedChatId = newChat.id
                        loadMessages(newChat.id)
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Missing required parameters for chat initialization"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to initialize chat"
                    )
                }
            }
        }
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                chatRepository.getChatMessages(chatId).collectLatest { messages ->
                    _uiState.update {
                        it.copy(
                            messages = messages,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || resolvedChatId.isNullOrEmpty()) return
        
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(resolvedChatId!!, content)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to send message")
                }
            }
        }
    }

    fun markMessagesAsRead() {
        if (resolvedChatId.isNullOrEmpty()) return
        
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(resolvedChatId!!)
            } catch (e: Exception) {
                // Silently fail, this is not critical
            }
        }
    }
}