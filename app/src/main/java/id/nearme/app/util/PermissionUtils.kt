package id.nearme.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat

/**
 * Checks if we should show permission rationale for a given permission
 */
fun shouldShowRequestPermissionRationale(context: Context, permission: String): Boolean {
    return ActivityCompat.shouldShowRequestPermissionRationale(
        context as androidx.activity.ComponentActivity,
        permission
    )
}

/**
 * Checks if permissions have been requested before
 */
fun getHasRequestedPermissions(context: Context): Boolean {
    val prefs = context.getSharedPreferences("location_permissions", Context.MODE_PRIVATE)
    return prefs.getBoolean("has_requested_permissions", false)
}

/**
 * Checks if permissions have been requested before
 */
fun setFirstRequestCompleted(context: Context) {
    val prefs = context.getSharedPreferences("location_permissions", Context.MODE_PRIVATE)
    prefs.edit().putBoolean("has_requested_permissions", true).apply()
}

/**
 * Opens the app settings screen to allow the user to grant permissions
 */
fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)

}