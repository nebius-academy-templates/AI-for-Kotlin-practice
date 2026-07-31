package com.sandbox.qa

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.sandbox.qa.condition.ConditionConfig
import com.sandbox.qa.di.AppContainer

/**
 * Application entry point that owns the manual DI container.
 * ViewModel factories reach it through [CreationExtras], see
 * [sandboxApplication].
 */
class SandboxApplication : Application() {
    // Lazy: AppContainer needs a usable Context (AuthStore opens
    // SharedPreferences), which the Application only has after attach.
    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // ConditionConfig owns Compose snapshot state. Initialize it before
        // setContent so a startup network coroutine cannot create it inside a
        // newer snapshot than the first composition.
        ConditionConfig.initialize()
    }
}

/**
 * Resolves [SandboxApplication] from ViewModel [CreationExtras] so each
 * ViewModel factory can grab its dependencies from [AppContainer] without a
 * service locator in composables.
 */
fun CreationExtras.sandboxApplication(): SandboxApplication = this[APPLICATION_KEY] as SandboxApplication
