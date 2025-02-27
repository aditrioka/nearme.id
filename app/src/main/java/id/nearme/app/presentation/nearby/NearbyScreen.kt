package id.nearme.app.presentation.nearby

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.nearme.app.domain.model.Location
import id.nearme.app.presentation.components.PostCard
import id.nearme.app.presentation.location.LocationState
import id.nearme.app.presentation.location.LocationViewModel
import id.nearme.app.util.getHasRequestedPermissions
import id.nearme.app.util.openAppSettings
import id.nearme.app.util.setFirstRequestCompleted
import id.nearme.app.util.shouldShowRequestPermissionRationale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyScreen(
    onNavigateToNewPost: () -> Unit,
    locationViewModel: LocationViewModel,
    modifier: Modifier = Modifier,
    nearbyViewModel: NearbyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationState by locationViewModel.locationState.collectAsStateWithLifecycle()
    val nearbyUiState by nearbyViewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentContext = rememberUpdatedState(context)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    // Check and request location permissions
    val permissionsToRequest = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    var permissionsGranted by remember {
        mutableStateOf(
            permissionsToRequest.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Track whether permissions are permanently denied
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    // Track whether this is the first permission request
    var hasRequestedPermission by remember {
        mutableStateOf(getHasRequestedPermissions(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.reduce { acc, isGranted -> acc && isGranted }

        if (permissionsGranted) {
            locationViewModel.startLocationUpdates(context)
            isPermanentlyDenied = false
        } else {
            // Mark that we've requested permissions at least once
            if (!hasRequestedPermission) {
                setFirstRequestCompleted(context)
                hasRequestedPermission = true
            }

            // Check if we should show rationale - if not, it's permanently denied
            val canShowRationale = permissionsToRequest.any {
                shouldShowRequestPermissionRationale(context, it)
            }

            isPermanentlyDenied = !canShowRationale && hasRequestedPermission
            locationViewModel.updateLocationStateForDeniedPermissions()
        }
    }

    // Request permissions when composable is first created if not already granted
    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            // First check if permissions are permanently denied already
            val canShowRationale = permissionsToRequest.any {
                shouldShowRequestPermissionRationale(context, it)
            }

            // If we can't show rationale and we've requested before, it's permanently denied
            isPermanentlyDenied = !canShowRationale && hasRequestedPermission

            if (isPermanentlyDenied) {
                locationViewModel.updateLocationStateForDeniedPermissions()
            } else {
                permissionLauncher.launch(permissionsToRequest)
            }
        } else {
            locationViewModel.startLocationUpdates(context)
        }
    }

    // Update the NearbyViewModel when we get location updates
    LaunchedEffect(locationState) {
        if (locationState is LocationState.Success) {
            val state = locationState as LocationState.Success
            val location = Location(state.latitude, state.longitude)
            nearbyViewModel.updateLocation(location)
        }
    }

    // Observer to detect permission changes when returning from Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check if permissions were granted while we were away
                val nowGranted = permissionsToRequest.all {
                    ContextCompat.checkSelfPermission(
                        currentContext.value,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }

                // If permission state changed, update our state
                if (nowGranted != permissionsGranted) {
                    permissionsGranted = nowGranted
                    if (nowGranted) {
                        isPermanentlyDenied = false
                        locationViewModel.startLocationUpdates(currentContext.value)
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Clean up when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            locationViewModel.stopLocationUpdates()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("nearme.id")

                        // Show coordinates when location is available
                        if (locationState is LocationState.Success) {
                            val successState = locationState as LocationState.Success
                            Text(
                                text = "${String.format("%.4f", successState.latitude)}, ${String.format("%.4f", successState.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                },
                actions = {
                    if (locationState is LocationState.Success) {
                        IconButton(onClick = {
                            scope.launch {
                                nearbyViewModel.refresh()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNewPost() },
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }

        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when {
            // Show loading or error states
            locationState is LocationState.Initial || locationState is LocationState.Loading -> {
                LoadingView(innerPadding)
            }

            locationState is LocationState.Error -> {
                ErrorView(
                    message = (locationState as LocationState.Error).message,
                    isPermanentlyDenied = isPermanentlyDenied,
                    onRetry = {
                        if (!permissionsGranted) {
                            permissionLauncher.launch(permissionsToRequest)
                        } else {
                            locationViewModel.startLocationUpdates(context)
                        }
                    },
                    onOpenSettings = { openAppSettings(context) },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            // Location success, show posts
            locationState is LocationState.Success -> {
                PostsListView(
                    uiState = nearbyUiState,
                    onRefresh = { nearbyViewModel.refresh() },
                    listState = listState,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun LoadingView(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Getting your location...")
    }
}

@Composable
private fun ErrorView(
    message: String,
    isPermanentlyDenied: Boolean,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Nearby Posts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Error: $message",
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPermanentlyDenied) {
            Text(
                text = "Please enable location permissions in app settings",
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        } else {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun PostsListView(
    uiState: NearbyUiState,
    onRefresh: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.posts.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.posts.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No posts nearby",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRefresh) {
                    Text("Refresh")
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Nearby Posts",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(uiState.posts) { post ->
                    PostCard(post = post)
                }
            }
        }
    }
}