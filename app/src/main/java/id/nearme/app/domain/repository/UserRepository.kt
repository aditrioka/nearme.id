package id.nearme.app.domain.repository

import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val currentUser: StateFlow<FirebaseUser?>

    suspend fun signInAnonymously(): Result<FirebaseUser>

    suspend fun updateDisplayName(displayName: String): Result<Unit>

    fun getCurrentDisplayName(): String

    fun isLoggedIn(): Boolean
}