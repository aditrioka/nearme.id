package id.nearme.app.presentation.chat

import androidx.lifecycle.SavedStateHandle
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String = checkNotNull(savedStateHandle["chatId"])
    private val otherUserName: String = checkNotNull(savedStateHandle["otherUserName"])

    private val _uiState = MutableStateFlow(ChatDetailUiState(chatId = chatId, otherUserName = otherUserName))
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        loadMessages()
        markMessagesAsRead()
    }

    private fun loadMessages() {
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
        if (content.isBlank()) return
        
        viewModelScope.launch {
            try {
                chatRepository.sendMessage(chatId, content)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to send message")
                }
            }
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(chatId)
            } catch (e: Exception) {
                // Silently fail, this is not critical
            }
        }
    }
}