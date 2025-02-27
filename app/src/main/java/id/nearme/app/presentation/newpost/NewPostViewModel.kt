package id.nearme.app.presentation.newpost

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nearme.app.domain.model.Location
import id.nearme.app.domain.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewPostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPostUiState())
    val uiState: StateFlow<NewPostUiState> = _uiState.asStateFlow()

    private var currentUserLocation: Location? = null

    fun updateLocation(location: Location) {
        currentUserLocation = location
    }

    fun createPost(content: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSubmitting = true) }

                val location = currentUserLocation ?: Location() // Default to 0,0 if no location
                val result = postRepository.createPost(content, location)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSuccess = true,
                            error = null
                        )
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSuccess = false,
                            error = exception?.message ?: "Failed to create post"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = false,
                        error = e.message ?: "Failed to create post"
                    )
                }
            }
        }
    }

    // Reset state after navigation
    fun resetState() {
        _uiState.update {
            NewPostUiState()
        }
    }
}