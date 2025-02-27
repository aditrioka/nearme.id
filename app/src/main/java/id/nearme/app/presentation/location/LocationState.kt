package id.nearme.app.presentation.location

// Sealed class to represent location states
sealed class LocationState {
    data object Initial : LocationState()
    data object Loading : LocationState()
    data class Success(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float
    ) : LocationState()

    data class Error(val message: String) : LocationState()
}