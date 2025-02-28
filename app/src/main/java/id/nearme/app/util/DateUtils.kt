package id.nearme.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatRelativeTime(date: Date): String {
    val now = Date()
    val diff = now.time - date.time
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            formatter.format(date)
        }
    }
}

fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(date)
}