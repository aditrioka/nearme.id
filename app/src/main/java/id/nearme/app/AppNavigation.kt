package id.nearme.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import id.nearme.app.presentation.chat.ChatDetailScreen
import id.nearme.app.presentation.chat.ChatDetailUiState
import id.nearme.app.presentation.chat.ChatListScreen
import id.nearme.app.presentation.location.LocationViewModel
import id.nearme.app.presentation.nearby.NearbyScreen
import id.nearme.app.presentation.nearby.NearbyViewModel
import id.nearme.app.presentation.newpost.NewPostScreen
import id.nearme.app.presentation.profile.ProfileScreen
import kotlinx.serialization.Serializable

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Screen = Screen.Nearby
) {
    // Create a shared LocationViewModel at the navigation level
    val locationViewModel: LocationViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<Screen.Nearby> {
            NearbyScreen(
                onNavigateToNewPost = {
                    navController.navigate(Screen.NewPost)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile)
                },
                onNavigateToChat = { 
                    navController.navigate(Screen.ChatList)
                },
                onNavigateToChatDetail = { authorId, authorName ->
                    navController.navigate(Screen.ChatDetail(authorId, authorName))
                },
                // Pass the shared locationViewModel
                locationViewModel = locationViewModel
            )
        }
        composable<Screen.NewPost> {
            NewPostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = {
                    navController.navigate(route = Screen.Profile)
                },
                locationViewModel = locationViewModel
            )
        }
        composable<Screen.Profile> {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable<Screen.ChatList> {
            ChatListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { chatId, otherUserName ->
                    navController.navigate(route = Screen.ChatDetail(chatId, otherUserName))
                }
            )
        }
        composable<Screen.ChatDetail> { backStackEntry ->
            val chatDetail = backStackEntry.toRoute<Screen.ChatDetail>()
            ChatDetailScreen(
                chatId = chatDetail.chatId,
                otherUserName = chatDetail.otherUserName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Direct chat intermediary screen for initiating a chat from a post
        composable<Screen.DirectChat> { backStackEntry ->
            val directChat = backStackEntry.toRoute<Screen.DirectChat>()

            // Get the shared nearby view model which already has chat repository access
            val viewModel: NearbyViewModel = hiltViewModel()
            
            LaunchedEffect(directChat.authorId, directChat.authorName) {
                // Create chat or get existing chat, then navigate to chat detail screen
                viewModel.createChatAndNavigateSync(
                    otherUserId = directChat.authorId,
                    otherUserName = directChat.authorName
                ) { chatId ->
                    // Navigate to the chat detail screen with the chat ID
                    if (chatId.isNotEmpty()) {
                        navController.navigate(Screen.ChatDetail(chatId, directChat.authorName)) {
                            // Remove this intermediary route from the back stack
                            popUpTo(Screen.DirectChat) { inclusive = true }
                        }
                    } else {
                        // If failed, go back to previous screen
                        navController.popBackStack()
                    }
                }
            }
            
            // Show loading indicator
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

sealed class Screen {

    @Serializable
    data object Nearby : Screen()

    @Serializable
    data object NewPost : Screen()

    @Serializable
    data object Profile : Screen()
    
    @Serializable
    data object ChatList : Screen()

    @Serializable
    data class ChatDetail(val chatId: String, val otherUserName: String) : Screen()

    @Serializable
    data class DirectChat(val authorId: String, val authorName: String) : Screen()
}