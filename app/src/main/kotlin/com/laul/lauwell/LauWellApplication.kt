package com.laul.lauwell

import android.app.Application
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.laul.lauwell.core.data.db.LauWellDatabase
import com.laul.lauwell.core.data.repository.MeasurementRepository
import com.laul.lauwell.core.data.source.HealthConnectSource
import java.time.Clock
import java.time.ZoneId

class LauWellApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency wiring. Small enough not to need Hilt yet; swap in later if it grows. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val zone: ZoneId get() = ZoneId.systemDefault()

    /** Null when Health Connect is missing or needs an update; the UI then offers to install it. */
    val healthConnect: HealthConnectSource? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectSource(HealthConnectClient.getOrCreate(context))
        } else {
            null
        }

    private val database = LauWellDatabase.create(context)

    val repository = MeasurementRepository(
        dao = database.measurements(),
        sources = listOfNotNull(healthConnect),
        clock = clock,
    )
}
