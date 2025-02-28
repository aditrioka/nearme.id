package id.nearme.app.presentation.direct

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nearme.app.domain.model.Chat
import id.nearme.app.domain.repository.ChatRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel specifically for handling direct navigation to a chat from a post
 */
@HiltViewModel
class DirectChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    /**
     * Creates a chat or gets an existing one between the current user and the specified user
     * 
     * @param otherUserId ID of the other user in the chat
     * @param otherUserName Display name of the other user
     * @param onResult Callback that receives the result with the chat object
     */
    fun createChatAndGetId(
        otherUserId: String,
        otherUserName: String,
        onResult: (Result<Chat>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = chatRepository.createChat(otherUserId, otherUserName)
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }
}