package id.nearme.app.presentation.nearby

import id.nearme.app.domain.model.Post

data class NearbyUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String = ""
)