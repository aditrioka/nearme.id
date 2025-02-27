package id.nearme.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ServerTimestamp
import id.nearme.app.domain.model.Location
import id.nearme.app.domain.model.Post

data class PostDto(
    @DocumentId
    val id: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val location: GeoPoint? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

fun PostDto.toDomainModel(): Post {
    return Post(
        id = id,
        content = content,
        authorId = authorId,
        authorName = authorName,
        location = Location(
            latitude = location?.latitude ?: 0.0,
            longitude = location?.longitude ?: 0.0
        ),
        createdAt = createdAt?.toDate()?.time ?: System.currentTimeMillis()
    )
}

fun Post.toDto(): PostDto {
    return PostDto(
        id = id,
        content = content,
        authorId = authorId,
        authorName = authorName,
        location = if (location.latitude != 0.0 && location.longitude != 0.0) {
            GeoPoint(location.latitude, location.longitude)
        } else null
    )
}