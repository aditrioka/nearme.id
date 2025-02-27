package id.nearme.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NearMeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Any app-wide initialization can go here
    }
}