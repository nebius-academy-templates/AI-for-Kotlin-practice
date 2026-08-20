package com.sandbox.qa.condition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sandbox.qa.SandboxApplication
import kotlinx.coroutines.runBlocking

/**
 * Allows tests and students to control sandbox states from the command line:
 *
 * adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --es condition slow_backend_response --ez enabled true
 * adb shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true
 */
class ConditionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.getBooleanExtra(EXTRA_RESET, false)) {
            ConditionConfig.reset()
            // The same reset seam clears the persistent auth token and the
            // saved profile (back to anonymous), so every autotest starts from
            // a known state (the base class sends this broadcast after each
            // test; run-suite sends it before the run).
            val app = context.applicationContext
            (app as? SandboxApplication)?.container?.let { container ->
                container.authStore.clear()
                container.profileStore.clear()
                container.locationStore.clear()
                container.notificationStore.clear()
                runCatching { runBlocking { container.rideRepository.resetSandbox() } }
                    .onFailure { Log.w(TAG, "Remote sandbox reset failed", it) }
            }
            Log.i(TAG, "All conditions reset, backend data and device-local user state cleared")
            return
        }
        val condition = intent.getStringExtra(EXTRA_CONDITION) ?: return
        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, false)
        ConditionConfig.set(condition, enabled)
        Log.i(TAG, "Condition '$condition' set to $enabled")
    }

    companion object {
        private const val TAG = "ConditionReceiver"
        const val EXTRA_CONDITION = "condition"
        const val EXTRA_ENABLED = "enabled"
        const val EXTRA_RESET = "reset"
    }
}
