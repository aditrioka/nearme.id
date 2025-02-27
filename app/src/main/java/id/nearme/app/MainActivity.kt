package id.nearme.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import id.nearme.app.domain.repository.UserRepository
import id.nearme.app.ui.theme.NearmeidTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Sign in anonymously if not already logged in
        if (!userRepository.isLoggedIn()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val result = userRepository.signInAnonymously()
                    if (result.isSuccess) {
                        Log.d(TAG, "Anonymous sign-in successful")
                    } else {
                        Log.e(TAG, "Anonymous sign-in failed", result.exceptionOrNull())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during anonymous sign-in", e)
                }
            }
        }

        setContent {
            NearmeidTheme {
                AppNavigation()
            }
        }
    }
}