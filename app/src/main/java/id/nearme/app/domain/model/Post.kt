package id.nearme.app.domain.model

data class Post(
    val id: String = "",
    val content: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val location: Location = Location(),
    val createdAt: Long = System.currentTimeMillis(),
    val distanceInMeters: Double? = null
)