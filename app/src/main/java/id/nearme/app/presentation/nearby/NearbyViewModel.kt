package id.nearme.app.presentation.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nearme.app.domain.model.Location
import id.nearme.app.domain.repository.ChatRepository
import id.nearme.app.domain.repository.PostRepository
import id.nearme.app.domain.repository.UserRepository
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
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        NearbyUiState(
            currentUserId = userRepository.getCurrentUserId()
        )
    )
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

    /**
     * Creates a chat with another user or returns existing chat, and navigates to chat detail screen.
     * This function first checks if a chat with the given user already exists,
     * and creates a new one if it doesn't.
     *
     * @param otherUserId ID of the user to chat with
     * @param otherUserName Display name of the user to chat with
     * @param onChatCreated Callback with chat ID to navigate to chat detail screen
     */
    fun createChatAndNavigateSync(
        otherUserId: String,
        otherUserName: String,
        onChatCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // First check if a chat already exists with this user
                val existingChatResult = chatRepository.getChatWithUser(otherUserId)
                val existingChat = existingChatResult.getOrNull()

                if (existingChat != null) {
                    // Chat already exists, navigate to it
                    onChatCreated(existingChat.id)
                } else {
                    // Create a new chat
                    val chatResult = chatRepository.createChat(otherUserId, otherUserName)
                    val chatId = chatResult.getOrNull()?.id ?: ""
                    onChatCreated(chatId)
                }

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create or find chat"
                    )
                }
                onChatCreated("") // Empty string indicates failure
            }
        }
    }
}