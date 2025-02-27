package id.nearme.app.presentation.profile

data class ProfileUiState(
    val currentUsername: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val hasAttemptedSubmit: Boolean = false
)