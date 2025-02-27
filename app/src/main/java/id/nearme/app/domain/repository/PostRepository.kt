package id.nearme.app.domain.repository

import id.nearme.app.domain.model.Location
import id.nearme.app.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    suspend fun createPost(content: String, location: Location): Result<Post>

    fun getNearbyPosts(
        currentLocation: Location,
        radiusInKm: Double = 50.0
    ): Flow<List<Post>>

    fun getUserPosts(userId: String): Flow<List<Post>>
}