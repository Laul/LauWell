package com.laul.lauwell

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.laul.lauwell.core.di.AppContainer
import kotlinx.coroutines.launch

/**
 * Application entry point.
 *
 * Owns the [AppContainer] — the single holder of process-wide singletons (manual DI, see
 * PLAN.md → Decisions). Created before any Activity, so it's always available to screens.
 *
 * Also sets up WorkManager (its default initializer is removed in the manifest), so workers get
 * their dependencies from the container.
 */
class LauWellApplication : Application(), Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Each time the app comes to the foreground (not when only a worker started the process).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                owner.lifecycleScope.launch { container.backgroundSync.onForeground() }
            }
        })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(container.workerFactory).build()
}
