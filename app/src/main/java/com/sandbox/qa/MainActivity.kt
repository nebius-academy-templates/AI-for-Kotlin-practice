package com.sandbox.qa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

        // Test entrance: UI tests launch the activity with the boolean intent
        // extra "authenticated" set to true (adb: --ez authenticated true) to
        // skip the auth flow and start directly on the map.
        // A real user is remembered instead: the AuthStore flag is set at OTP
        // success and survives restarts; the ConditionReceiver reset broadcast
        // clears it, which the test base class sends after every test.
        val authStore = (application as SandboxApplication).container.authStore
        val authenticated = intent.getBooleanExtra(EXTRA_AUTHENTICATED, false) || authStore.isLoggedIn()

        setContent {
            SandboxTheme {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true },
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = if (authenticated) "map" else "phone_login",
                    ) {
                        composable("phone_login") {
                            PhoneLoginScreen(onContinue = { navController.navigate("otp") })
                        }
                        composable("otp") {
                            OtpScreen(
                                onSuccess = {
                                    authStore.setLoggedIn(true)
                                    navController.navigate("passkey")
                                },
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
