package id.nearme.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val currentUser = auth.currentUser
        _uiState.update {
            it.copy(currentUsername = currentUser?.displayName)
        }
    }

    fun updateDisplayName(displayName: String) {
        viewModelScope.launch {
            try {
                if (displayName.isBlank()) {
                    _uiState.update {
                        it.copy(hasAttemptedSubmit = true)
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(isSubmitting = true, hasAttemptedSubmit = true)
                }

                val user = auth.currentUser
                if (user == null) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            error = "You must be logged in to update your profile"
                        )
                    }
                    return@launch
                }

                // Update display name in Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()

                user.updateProfile(profileUpdates).await()

                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        isSuccess = true,
                        currentUsername = displayName,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to update profile"
                    )
                }
            }
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                isSubmitting = false,
                isSuccess = false,
                error = null,
                hasAttemptedSubmit = false
            )
        }
    }
}