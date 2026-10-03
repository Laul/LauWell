package com.laul.lauwell

import android.app.Application
import com.laul.lauwell.core.di.AppContainer

/**
 * Application entry point.
 *
 * Owns the [AppContainer] — the single holder of process-wide singletons (manual DI, see
 * PLAN.md → Decisions). Created before any Activity, so it's always available to screens.
 */
class LauWellApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
