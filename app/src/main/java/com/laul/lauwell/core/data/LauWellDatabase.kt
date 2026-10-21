package com.laul.lauwell.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Single Room database for the app, shared across feature modules: one physical database, so
 * cross-module queries and a single migration story stay possible.
 *
 * Holds the Health Connect cache only ([HealthCacheDao]). User-logged data lives in Firestore,
 * not here (CLAUDE.md → Stack). Everything in it can be rebuilt from Health Connect, which is why
 * `AppContainer` lets a schema change drop it rather than migrate.
 */
@Database(
    entities = [DailySummaryValueEntity::class, SyncStateEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class LauWellDatabase : RoomDatabase() {
    abstract fun healthCacheDao(): HealthCacheDao
}
