package id.nearme.app

import androidx.compose.runtime.Composable
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
                    navController.popBackStack()
                }
            )
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