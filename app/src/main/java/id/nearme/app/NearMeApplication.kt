package id.nearme.app

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NearMeApplication : Application() {
    private val auth: FirebaseAuth by lazy { Firebase.auth }

    override fun onCreate() {
        super.onCreate()
        // Automatically sign in anonymously for this proof-of-concept
        signInAnonymously()
    }

    private fun signInAnonymously() {
        // Check if user is already signed in
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("NearMeAuth", "Anonymous authentication success")
                }
                .addOnFailureListener { e ->
                    Log.e("NearMeAuth", "Anonymous authentication failed", e)
                }
        }
    }
}