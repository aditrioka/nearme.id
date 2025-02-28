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
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        loadChats()
    }

    private fun loadChats() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                chatRepository.getUserChats().collectLatest { chats ->
                    _uiState.update {
                        it.copy(
                            chats = chats,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load chats"
                    )
                }
            }
        }
    }

    fun createChat(otherUserId: String, otherUserName: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                chatRepository.createChat(otherUserId, otherUserName)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create chat"
                    )
                }
            }
        }
    }

    fun refresh() {
        loadChats()
    }
}