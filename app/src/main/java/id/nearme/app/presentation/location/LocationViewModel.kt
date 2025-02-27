package id.nearme.app.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.nearme.app.domain.model.Location as DomainLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TAG = "LocationViewModel"

@HiltViewModel
class LocationViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Location state for UI
    private val _locationState = MutableStateFlow<LocationState>(LocationState.Initial)
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    // Current location for other ViewModels
    private val _currentLocation = MutableStateFlow<DomainLocation?>(null)
    val currentLocation: StateFlow<DomainLocation?> = _currentLocation.asStateFlow()

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    init {
        // Initialize immediately when created to ensure state is always available
        initLocationClient(context)
    }

    fun initLocationClient(context: Context) {
        try {
            // Check if Google Play Services is available
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)

            if (resultCode != ConnectionResult.SUCCESS) {
                val errorMessage = if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    "Google Play Services needs to be updated"
                } else {
                    "This device doesn't support Google Play Services"
                }
                _locationState.value = LocationState.Error(errorMessage)
                return
            }

            if (fusedLocationClient == null) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location client", e)
            _locationState.value =
                LocationState.Error("Could not initialize location services: ${e.localizedMessage}")
        }
    }

    fun startLocationUpdates(context: Context) {
        if (Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted(context) ||
            Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted(context)
        ) {
            _locationState.value = LocationState.Loading

            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, 60000
                ).apply {
                    setMinUpdateDistanceMeters(10f)
                    setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                    setWaitForAccurateLocation(true)
                }.build()

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        for (location in locationResult.locations) {
                            updateLocationState(location)
                            break // We only need the most recent location
                        }
                    }
                }

                fusedLocationClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback as LocationCallback,
                    Looper.getMainLooper()
                )?.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to request location updates", exception)
                    _locationState.value = LocationState.Error("Failed to start location updates: ${exception.localizedMessage}")
                }

                // Also get last known location for immediate response
                fusedLocationClient?.lastLocation?.addOnSuccessListener { location ->
                    if (location != null) {
                        updateLocationState(location)
                    } else {
                        // If last known location is null, trigger a single location update
                        requestSingleLocationUpdate(context)
                    }
                }?.addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to get last location", exception)
                    _locationState.value = LocationState.Error("Failed to get location: ${exception.localizedMessage}")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception for location", e)
                _locationState.value = LocationState.Error("Location permission denied: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting location updates", e)
                _locationState.value = LocationState.Error("Error setting up location services: ${e.localizedMessage}")
            }
        } else {
            _locationState.value = LocationState.Error("Location permissions are required to show nearby posts")
        }
    }

    // Request a single location update to get the current location
    private fun requestSingleLocationUpdate(context: Context) {
        if (!Manifest.permission.ACCESS_FINE_LOCATION.isPermissionGranted(context) &&
            !Manifest.permission.ACCESS_COARSE_LOCATION.isPermissionGranted(context)
        ) {
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10000
            ).apply {
                setMinUpdateDistanceMeters(0f) // Any distance change
                setMaxUpdateDelayMillis(5000) // Get update within 5 seconds
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setWaitForAccurateLocation(false) // Don't wait for high accuracy
            }.build()

            val singleUpdateCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.firstOrNull()?.let { location ->
                        updateLocationState(location)
                    }
                    // Remove callback after receiving the update
                    fusedLocationClient?.removeLocationUpdates(this)
                }
            }

            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                singleUpdateCallback,
                Looper.getMainLooper()
            )?.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to request single location update", exception)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception for single location update", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting single location update", e)
        }
    }

    private fun updateLocationState(location: Location) {
        val domainLocation = DomainLocation(
            latitude = location.latitude,
            longitude = location.longitude
        )

        _currentLocation.update { domainLocation }

        _locationState.value = LocationState.Success(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy
        )
    }

    fun updateLocationStateForDeniedPermissions() {
        _locationState.value =
            LocationState.Error("Location permissions are required to show nearby posts")
    }

    fun stopLocationUpdates() {
        try {
            locationCallback?.let {
                fusedLocationClient?.removeLocationUpdates(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    private fun String.isPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            this
        ) == PackageManager.PERMISSION_GRANTED
    }
}