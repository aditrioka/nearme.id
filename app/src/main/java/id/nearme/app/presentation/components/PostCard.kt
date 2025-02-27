package id.nearme.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.nearme.app.domain.model.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PostCard(
    post: Post,
    isCurrentUser: Boolean = false,
    onChatClick: (String, String) -> Unit = { _, _ -> },
    onLikeClick: (String) -> Unit = { _ -> },
    onReplyClick: (String) -> Unit = { _ -> },
    modifier: Modifier = Modifier
) {
    // State for like button (in a real app this would come from a repository)
    var isLiked by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Author info with avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Author avatar (circle with first letter)
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = post.authorName.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Show "You" badge if it's the current user's post
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "You",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        Text(
                            text = formatDistance(post.distanceInMeters),
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = " • ${formatDate(post.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider before action buttons
            HorizontalDivider()

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Reply button
                IconButton(
                    onClick = { onReplyClick(post.id) }
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Reply"
                    )
                }

                // Like button
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        onLikeClick(post.id)
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Chat button - Only enabled if it's not the current user's post
                IconButton(
                    onClick = {
                        if (!isCurrentUser) {
                            onChatClick(post.authorId, post.authorName)
                        }
                    },
                    enabled = !isCurrentUser
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = if (!isCurrentUser) "Chat with ${post.authorName}" else "Cannot chat with yourself",
                        modifier = Modifier.alpha(if(!isCurrentUser) 1f else 0.5f)
                    )
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp

    return when {
        diff < 60 * 60 * 1000 -> {
            // Less than an hour
            val minutes = (diff / (60 * 1000)).toInt()
            "$minutes min ago"
        }

        diff < 24 * 60 * 60 * 1000 -> {
            // Less than a day
            val hours = (diff / (60 * 60 * 1000)).toInt()
            "$hours hrs ago"
        }

        diff < 7 * 24 * 60 * 60 * 1000 -> {
            // Less than a week
            val days = (diff / (24 * 60 * 60 * 1000)).toInt()
            "$days days ago"
        }

        else -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            format.format(date)
        }
    }
}

private fun formatDistance(distanceInMeters: Double?): String {
    if (distanceInMeters == null) return "Unknown distance"

    return when {
        distanceInMeters < 1000 -> "${distanceInMeters.toInt()} m"
        else -> {
            val distanceInKm = (distanceInMeters / 1000.0)
            if (distanceInKm < 10) {
                String.format("%.1f km", distanceInKm)
            } else {
                "${distanceInKm.toInt()} km"
            }
        }
    }
}