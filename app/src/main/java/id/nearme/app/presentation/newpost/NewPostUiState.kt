package id.nearme.app.presentation.newpost

data class NewPostUiState(
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)