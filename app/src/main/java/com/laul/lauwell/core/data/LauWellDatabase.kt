package com.laul.lauwell.core.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Single Room database for the app, shared across feature modules: one physical database, so
 * cross-module queries and a single migration story stay possible.
 *
 * Holds two kinds of tables:
 * - the Health Connect cache ([HealthCacheDao]), which can be rebuilt from Health Connect;
 * - what you log ([RecordDao]), which cannot: the only copy until it reaches Postgres.
 *
 * So every version change comes with a migration (`AppContainer` no longer drops the database), tested
 * in `LauWellDatabaseMigrationTest` against the exported schemas in `app/schemas`. A change to a cache
 * table may still drop and recreate that table in its migration; never a user table.
 *
 * Versions: 1-2 the cache only (dropped on upgrade, nothing typed in it); 3 adds `record`,
 * `record_value`, `record_link`.
 */
@Database(
    entities = [
        DailySummaryValueEntity::class,
        SyncStateEntity::class,
        RecordEntity::class,
        RecordValueEntity::class,
        RecordLinkEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 2, to = 3)],
)
abstract class LauWellDatabase : RoomDatabase() {
    abstract fun healthCacheDao(): HealthCacheDao

    abstract fun recordDao(): RecordDao
}
