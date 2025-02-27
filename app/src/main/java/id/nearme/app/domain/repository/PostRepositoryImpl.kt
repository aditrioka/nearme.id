package id.nearme.app.domain.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import id.nearme.app.data.model.PostDto
import id.nearme.app.data.model.toDomainModel
import id.nearme.app.domain.model.Location
import id.nearme.app.domain.model.Post
import id.nearme.app.util.calculateDistance
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PostRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : PostRepository {

    private val postsCollection = firestore.collection("posts")

    override suspend fun createPost(content: String, location: Location): Result<Post> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(IllegalStateException("User not authenticated"))

            val postId = Uuid.random().toString()
            val postDto = PostDto(
                id = postId,
                content = content,
                authorId = currentUser.uid,
                authorName = currentUser.displayName ?: "Anonymous",
                location = GeoPoint(location.latitude, location.longitude)
            )

            postsCollection.document(postId).set(postDto).await()

            Result.success(postDto.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getNearbyPosts(currentLocation: Location, radiusInKm: Double): Flow<List<Post>> = callbackFlow {
        // We'll fetch all posts and filter by distance
        // In a production app, you'd use Firebase GeoQuery capabilities
        // or a dedicated geospatial database

        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { document ->
                        document.toObject(PostDto::class.java)?.toDomainModel()
                    }

                    // Filter and calculate distance
                    val nearbyPosts = posts.map { post ->
                        val distance = calculateDistance(
                            currentLocation.latitude, currentLocation.longitude,
                            post.location.latitude, post.location.longitude
                        )

                        // Only include posts within the radius (convert km to meters)
                        if (distance <= radiusInKm * 1000) {
                            post.copy(distanceInMeters = distance)
                        } else null
                    }.filterNotNull()

                    trySend(nearbyPosts.sortedBy { it.distanceInMeters })
                }
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("authorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull { document ->
                        document.toObject(PostDto::class.java)?.toDomainModel()
                    }

                    trySend(posts)
                }
            }

        awaitClose {
            listener.remove()
        }
    }
}