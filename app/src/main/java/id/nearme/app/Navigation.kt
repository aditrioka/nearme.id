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
import id.nearme.app.presentation.chat.ChatDetailScreen
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
                    navController.navigate(route = Screen.NewPost)
                },
                onNavigateToProfile = {
                    navController.navigate(route = Screen.Profile)
                },
                onNavigateToChat = { 
                    navController.navigate(route = Screen.ChatList)
                },
                onNavigateToDirectChat = { authorId, authorName ->
                    navController.navigate("direct_chat/$authorId/$authorName")
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
                    navController.navigate(route = "chat_detail/$chatId/$otherUserName")
                }
            )
        }
        composable(
            route = "chat_detail/{chatId}/{otherUserName}",
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ChatDetailScreen(
                onNavigateBack = {
                    // Go back to chat list instead of popping directly
                    if (navController.previousBackStackEntry?.destination?.route == Screen.ChatList.toString()) {
                        // If we came from chat list, just pop back
                        navController.popBackStack()
                    } else {
                        // If we came from somewhere else (e.g., directly from NearbyScreen),
                        // navigate to ChatList and then remove this screen from backstack
                        navController.navigate(route = Screen.ChatList) {
                            popUpTo("chat_detail/{chatId}/{otherUserName}") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Special route for initiating a direct chat from a post
        composable(
            route = "direct_chat/{authorId}/{authorName}",
            arguments = listOf(
                navArgument("authorId") { type = NavType.StringType },
                navArgument("authorName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""
            val authorName = backStackEntry.arguments?.getString("authorName") ?: ""
            
            // Get the shared nearby view model which already has chat repository access
            val viewModel: NearbyViewModel = hiltViewModel()
            
            LaunchedEffect(authorId, authorName) {
                // Navigate to the chat detail screen
                // This temporarily navigates to a loading screen while the chat is being created
                viewModel.createChatAndNavigateSync(
                    otherUserId = authorId,
                    otherUserName = authorName
                ) { chatId ->
                    // Navigate to the chat detail screen with the chat ID
                    if (chatId.isNotEmpty()) {
                        navController.navigate("chat_detail/$chatId/$authorName") {
                            // Remove this intermediary route from the back stack
                            popUpTo("direct_chat/{authorId}/{authorName}") { inclusive = true }
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
}