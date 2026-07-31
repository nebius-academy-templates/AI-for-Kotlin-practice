package com.sandbox.qa.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sandbox.qa.SandboxApplication
import com.sandbox.qa.condition.ConditionConfig
import com.sandbox.qa.data.RideOption
import com.sandbox.qa.data.SandboxContract
import com.sandbox.qa.data.formatEuroCents
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onOpenOrders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory),
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    // Needed to start the classic View-based DriverSignupActivity from Compose.
    val context = LocalContext.current
    // The drawer header shows the live profile: a save on the edit screen
    // re-renders the name here immediately (the store is app-scoped).
    val profile by (context.applicationContext as SandboxApplication)
        .container.profileStore.profile
        .collectAsState()
    val notifications by (context.applicationContext as SandboxApplication)
        .container.notificationStore.notifications
        .collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            // Every exit closes the drawer FIRST (close() suspends until the
            // sheet is gone), so navigating away never saves an open drawer:
            // coming Back from history/settings/signup lands on the plain
            // form, not on a restored menu covering it.
            DrawerContent(
                // Anonymous rider until a profile is created: show the
                // placeholder "Username", which is also the load-bearing
                // launch value the tests assert.
                profileName =
                    if (profile.isAnonymous) {
                        "Username"
                    } else {
                        listOf(profile.firstName, profile.lastName)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                    },
                profileRating = profile.rating,
                unreadNotificationCount = notifications.count { !it.isRead },
                onClose = { scope.launch { drawerState.close() } },
                onOpenProfile = {
                    scope.launch {
                        drawerState.close()
                        onOpenProfile()
                    }
                },
                onOpenOrders = {
                    scope.launch {
                        drawerState.snapTo(DrawerValue.Closed)
                        onOpenOrders()
                    }
                },
                onOpenSettings = {
                    scope.launch {
                        drawerState.close()
                        onOpenSettings()
                    }
                },
                onOpenNotifications = {
                    scope.launch {
                        drawerState.close()
                        onOpenNotifications()
                    }
                },
                onOpenSupport = {
                    scope.launch {
                        drawerState.close()
                        onOpenSupport()
                    }
                },
                onBecomeDriver = {
                    scope.launch {
                        drawerState.close()
                        context.startActivity(Intent(context, DriverSignupActivity::class.java))
                    }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Book a ride") },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("map_menu_button"),
                        ) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                )
            },
        ) { contentPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
            ) {
                // Architecture exception, on purpose: state-driven UI reads
                // ConditionConfig snapshot state directly instead of going through the
                // ViewModel StateFlow. A mid-session adb broadcast must re-render
                // this banner instantly; a snapshotFlow bridge could lose that.
                if (ConditionConfig.isEnabled(ConditionConfig.REGION_UNAVAILABLE)) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(WarningOrange)
                                .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Service is not available in this region yet",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("region_banner"),
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    RideContent(
                        state = uiState,
                        onPickupChange = viewModel::onPickupChange,
                        onDestinationChange = viewModel::onDestinationChange,
                        onRefresh = viewModel::refreshRides,
                        onSelectRide = viewModel::onRideSelected,
                        onOrder = viewModel::orderSelectedRide,
                        onComplete = viewModel::completeRide,
                        onCancel = viewModel::cancelRide,
                        onBookAnother = viewModel::bookAnotherRide,
                        onBackHome = viewModel::returnHomeAfterRide,
                    )
                }
            }
        }
    }
}

@Composable
private fun RideContent(
    state: MapUiState,
    onPickupChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectRide: (Int) -> Unit,
    onOrder: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onBookAnother: () -> Unit,
    onBackHome: () -> Unit,
) {
    when {
        state.searchingForDriver -> {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("ride_driver_searching"),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Searching for a car",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("ride_driver_searching_title"),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "We are checking nearby drivers for your selected tariff.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
            }
        }

        state.completedOrder != null -> {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Ride completed",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("ride_completed_title"),
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "${state.completedOrder.from} → ${state.completedOrder.to}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.completedOrder.price.formatEuroCents(),
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.testTag("ride_completed_price"),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "This ride is now in Order history",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onBookAnother,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_book_another_button"),
                ) {
                    Text("Book another ride")
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onBackHome,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_back_home_button"),
                ) {
                    Text("Back to home")
                }
            }
        }

        state.activeRide != null -> {
            val ride = state.activeRide
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Driver found",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.testTag("ride_driver_found_title"),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${ride.from} → ${ride.to}",
                    modifier = Modifier.testTag("ride_driver_route"),
                )
                Spacer(Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.large,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_driver_card"),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(ride.driver.name, style = MaterialTheme.typography.titleLarge)
                        Text("${ride.driver.car} · ${ride.driver.plate}")
                        Spacer(Modifier.height(8.dp))
                        Text("Arrives in ${ride.driver.etaMinutes} min")
                    }
                }
                Spacer(Modifier.height(16.dp))
                RideActionFeedback(state)
                Button(
                    onClick = onComplete,
                    enabled = !state.rideActionLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_complete_button"),
                ) {
                    Text("Complete demo ride")
                }
                TextButton(
                    onClick = onCancel,
                    enabled = !state.rideActionLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_cancel_button"),
                ) {
                    Text("Cancel ride")
                }
            }
        }

        else -> {
            RideSearchContent(state, onPickupChange, onDestinationChange, onRefresh, onSelectRide, onOrder)
        }
    }
}

@Composable
private fun RideSearchContent(
    state: MapUiState,
    onPickupChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelectRide: (Int) -> Unit,
    onOrder: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.loading,
        onRefresh = {
            if (
                state.pickup.isNotBlank() &&
                state.destination.isNotBlank() &&
                !state.loading &&
                !state.searchingForDriver &&
                !state.rideActionLoading
            ) {
                onRefresh()
            }
        },
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("rides_pull_to_refresh"),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
        ) {
            state.statusMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("ride_status_message"),
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = state.pickup,
                onValueChange = onPickupChange,
                label = { Text("Pickup") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("map_from_field"),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.destination,
                onValueChange = onDestinationChange,
                label = { Text("Where to?") },
                singleLine = true,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("map_to_field"),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    if (state.pickup.isBlank()) {
                        "Enter a pickup location"
                    } else if (state.destination.isBlank()) {
                        "Enter a destination to see tariffs"
                    } else {
                        "Pull down to refresh tariffs"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("rides_pull_hint"),
            )
            Spacer(Modifier.height(12.dp))

            if (state.loading && state.rides != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.testTag("rides_loading"))
                }
            }

            when {
                state.loading && state.rides == null -> {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("rides_loading"))
                    }
                }

                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag("rides_error"),
                        )
                        TextButton(onClick = onRefresh, modifier = Modifier.testTag("rides_retry_button")) {
                            Text("Retry")
                        }
                    }
                }

                state.rides != null -> {
                    Column(modifier = Modifier.testTag("rides_list")) {
                        val carUnavailable = ConditionConfig.isEnabled(ConditionConfig.CAR_UNAVAILABLE)
                        state.rides.forEach { ride ->
                            RideRow(
                                ride = ride,
                                selected = state.selectedRideId == ride.id,
                                unavailable =
                                    !ride.available ||
                                        (carUnavailable && ride.id == SandboxContract.MINIVAN_RIDE_ID),
                                onSelect = { onSelectRide(ride.id) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            state.rides?.firstOrNull { it.id == state.selectedRideId }?.let { selected ->
                Spacer(Modifier.height(12.dp))
                RideActionFeedback(state)
                Button(
                    onClick = onOrder,
                    enabled = !state.rideActionLoading && !state.searchingForDriver,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("ride_order_button"),
                ) {
                    Text("Order ${selected.name} · ${selected.price.formatEuroCents()}")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RideActionFeedback(state: MapUiState) {
    if (state.rideActionLoading) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.testTag("ride_action_loading"))
        }
    }
    state.rideActionError?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("ride_action_error"),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RideRow(
    ride: RideOption,
    selected: Boolean,
    unavailable: Boolean = false,
    onSelect: () -> Unit,
) {
    // Grayed = visible but not selectable: the disabled semantics surface to
    // UiAutomator as enabled="false" on the row, which is the test hook.
    val showSelected = selected && !unavailable
    val mainColor = if (unavailable) MaterialTheme.colorScheme.outline else Color.Unspecified
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = showSelected, enabled = !unavailable, onClick = onSelect)
                .background(if (showSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                .border(width = 1.dp, color = if (showSelected) Lime else Color.Transparent)
                .padding(vertical = 12.dp)
                .testTag("ride_option_${ride.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "🚕", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ride.name,
                style = MaterialTheme.typography.titleMedium,
                color = mainColor,
                modifier = Modifier.testTag("ride_name_${ride.id}"),
            )
            Text(
                text = if (unavailable) "Unavailable for this route" else "👤 ${ride.seats} · ${ride.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (showSelected) {
            Text(
                text = "✓",
                color = Lime,
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier
                        .padding(end = 8.dp)
                        .testTag("ride_selected_${ride.id}"),
            )
        }
        Text(
            text = "~${ride.price.formatEuroCents()}",
            style = MaterialTheme.typography.titleMedium,
            color = mainColor,
            modifier = Modifier.testTag("ride_price_${ride.id}"),
        )
    }
}

@Composable
private fun DrawerContent(
    profileName: String,
    profileRating: String,
    unreadNotificationCount: Int,
    onClose: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onBecomeDriver: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier =
                Modifier
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                // The profile header opens the edit screen.
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenProfile)
                            .testTag("drawer_profile_button"),
                ) {
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("drawer_profile_name"),
                    )
                    Text(
                        text = profileRating,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("drawer_profile_rating"),
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("drawer_close_button"),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close menu")
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            NavigationDrawerItem(
                label = { Text("Order history") },
                selected = false,
                onClick = onOpenOrders,
                modifier = Modifier.testTag("drawer_orders_item"),
            )
            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = false,
                onClick = onOpenSettings,
                modifier = Modifier.testTag("drawer_settings_item"),
            )
            NavigationDrawerItem(
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notifications")
                        if (unreadNotificationCount > 0) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = unreadNotificationCount.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("drawer_notifications_badge"),
                            )
                        }
                    }
                },
                selected = false,
                onClick = onOpenNotifications,
                modifier = Modifier.testTag("drawer_notifications_item"),
            )
            NavigationDrawerItem(
                label = { Text("Help") },
                selected = false,
                onClick = onOpenSupport,
                modifier = Modifier.testTag("drawer_help_item"),
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onBecomeDriver,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .testTag("drawer_become_driver_button"),
            ) {
                Text("Become a driver")
            }
        }
    }
}
