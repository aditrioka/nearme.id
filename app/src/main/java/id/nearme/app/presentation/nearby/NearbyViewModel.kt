package id.nearme.app.presentation.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nearme.app.domain.model.Location
import id.nearme.app.domain.repository.ChatRepository
import id.nearme.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NearbyUiState())
    val uiState: StateFlow<NearbyUiState> = _uiState.asStateFlow()

    private var currentUserLocation: Location? = null

    fun updateLocation(location: Location) {
        currentUserLocation = location
        loadNearbyPosts(location)
    }

    private fun loadNearbyPosts(location: Location) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                postRepository.getNearbyPosts(location).collectLatest { posts ->
                    _uiState.update {
                        it.copy(
                            posts = posts,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load nearby posts"
                    )
                }
            }
        }
    }

    fun refresh() {
        currentUserLocation?.let {
            loadNearbyPosts(it)
        }
    }
    
    fun createChatAndNavigateSync(
        otherUserId: String,
        otherUserName: String,
        onNavigate: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                chatRepository.createChat(otherUserId, otherUserName)
                onNavigate()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create chat")
                }
            }
        }
    }
    
    /**
     * Creates a chat with another user and returns the chat ID for direct navigation
     */
    fun createChatAndNavigateSync(
        otherUserId: String,
        otherUserName: String,
        onChatCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val chatResult = chatRepository.createChat(otherUserId, otherUserName)
                val chatId = chatResult.getOrNull()?.id ?: ""
                onChatCreated(chatId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create chat")
                }
                onChatCreated("")
            }
        }
    }

    fun startDirectChatWithUser(
        otherUserId: String,
        otherUserName: String,
        onNavigateToChatList: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Create/get chat with this user
                val chatResult = chatRepository.createChat(otherUserId, otherUserName)
                
                // Navigate to chat list, which will handle deep-linking to the specific chat
                // The direct navigation to chat detail will be handled in Navigation.kt
                onNavigateToChatList()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to create chat")
                }
            }
        }
    }
}