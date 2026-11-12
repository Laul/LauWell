package com.laul.lauwell

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.work.Configuration
import com.laul.lauwell.core.di.AppContainer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
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
        // Signing in (or switching account) while the app is open: fetch that account's entries now.
        // The first value is the session at launch, already covered by the foreground request below.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            container.authRepository.session.map { it?.uid }.distinctUntilChanged().drop(1).filterNotNull()
                .collect { container.entrySyncSchedule.request() }
        }
        // Each time the app comes to the foreground (not when only a worker started the process).
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                owner.lifecycleScope.launch { container.backgroundSync.onForeground() }
                // What you logged: sent and fetched once online (EntrySync, which skips when signed out).
                container.entrySyncSchedule.request()
            }
        })
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(container.workerFactory).build()
}
