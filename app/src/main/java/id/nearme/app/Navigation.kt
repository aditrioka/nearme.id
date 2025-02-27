package id.nearme.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.nearme.app.presentation.location.LocationViewModel
import id.nearme.app.presentation.nearby.NearbyScreen
import id.nearme.app.presentation.newpost.NewPostScreen
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
                // Pass the shared locationViewModel
                locationViewModel = locationViewModel
            )
        }
        composable<Screen.NewPost> {
            NewPostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                locationViewModel = locationViewModel
            )
        }
    }
}

sealed class Screen {

    @Serializable
    data object Nearby : Screen()

    @Serializable
    data object NewPost : Screen()
}