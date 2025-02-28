package id.nearme.app.presentation.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.nearme.app.domain.model.Chat
import id.nearme.app.util.formatRelativeTime
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (chatId: String, otherUserName: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading && uiState.chats.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.chats.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No chats yet",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.chats) { chat ->
                        ChatItem(
                            chat = chat,
                            onClick = { 
                                // Determine the other user's name - for a chat there are always 2 participants
                                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                val otherUserId = chat.participants.find { it != currentUserId } ?: ""
                                val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
                                
                                onNavigateToChat(chat.id, otherUserName) 
                            }
                        )
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItem(
    chat: Chat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Get current user ID from Firebase Auth
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Find the other user's ID and name
    val otherUserId = chat.participants.find { it != currentUserId } ?: ""
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown"
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = otherUserName.firstOrNull()?.toString() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.fillMaxSize(),
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = chat.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatRelativeTime(Date(chat.lastMessageTimestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                if (chat.unreadCount > 0) {
                    Badge {
                        Text(text = chat.unreadCount.toString())
                    }
                }
            }
        }
    }
}