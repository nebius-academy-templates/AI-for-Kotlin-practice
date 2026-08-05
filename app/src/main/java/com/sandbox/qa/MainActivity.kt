package com.sandbox.qa

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sandbox.qa.ui.GeoOnboardingScreen
import com.sandbox.qa.ui.MapScreen
import com.sandbox.qa.ui.NotificationsScreen
import com.sandbox.qa.ui.OrderHistoryScreen
import com.sandbox.qa.ui.OtpScreen
import com.sandbox.qa.ui.PasskeyCreateScreen
import com.sandbox.qa.ui.PasskeyPromoScreen
import com.sandbox.qa.ui.PhoneLoginScreen
import com.sandbox.qa.ui.ProfileEditScreen
import com.sandbox.qa.ui.SandboxTheme
import com.sandbox.qa.ui.SettingsScreen
import com.sandbox.qa.ui.SupportScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as SandboxApplication).container
        val testEntrance = intent.getBooleanExtra(EXTRA_AUTHENTICATED, false)
        // Explicit Appium-only seam: skip the UI and allow the repository to
        // issue the deterministic sandbox token after each test reset. A real
        // user never receives this fallback and is signed in only by /auth/otp.
        if (testEntrance) container.enableTestAuthentication()
        val authenticated = testEntrance || container.authStore.isLoggedIn()

        setContent {
            SandboxTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true },
                ) {
                    val navController = rememberNavController()
                    val token by container.authStore.token.collectAsState()
                    LaunchedEffect(token, testEntrance) {
                        val currentRoute = navController.currentDestination?.route
                        if (!testEntrance && token == null && currentRoute != null && currentRoute != "phone_login") {
                            navController.navigate("phone_login") {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = if (authenticated) "map" else "phone_login",
                    ) {
                        composable("phone_login") {
                            PhoneLoginScreen(
                                onContinue = { phone ->
                                    navController.navigate("otp/${Uri.encode(phone)}")
                                },
                            )
                        }
                        composable(
                            route = "otp/{phone}",
                            arguments = listOf(navArgument("phone") { type = NavType.StringType }),
                        ) { backStackEntry ->
                            OtpScreen(
                                phone = backStackEntry.arguments?.getString("phone").orEmpty(),
                                onSuccess = { navController.navigate("passkey") },
                            )
                        }
                        composable("passkey") {
                            PasskeyPromoScreen(
                                onCreate = { navController.navigate("passkey_create") },
                                onSkip = { navController.navigate("geo") },
                            )
                        }
                        composable("passkey_create") {
                            PasskeyCreateScreen(onDone = { navController.navigate("geo") })
                        }
                        composable("geo") {
                            GeoOnboardingScreen(
                                onDone = {
                                    navController.navigate("map") {
                                        popUpTo("phone_login") { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable("map") {
                            MapScreen(
                                onOpenOrders = { navController.navigate("orders") },
                                onOpenSettings = { navController.navigate("settings") },
                                onOpenProfile = { navController.navigate("profile_edit") },
                                onOpenNotifications = { navController.navigate("notifications") },
                                onOpenSupport = { navController.navigate("support") },
                            )
                        }
                        composable("orders") {
                            OrderHistoryScreen(onBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("profile_edit") {
                            ProfileEditScreen(onBack = { navController.popBackStack() })
                        }
                        composable("notifications") {
                            NotificationsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("support") {
                            SupportScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    companion object {
        /** Boolean intent extra: start on the map, skipping auth. Tests only. */
        const val EXTRA_AUTHENTICATED = "authenticated"
    }
}
