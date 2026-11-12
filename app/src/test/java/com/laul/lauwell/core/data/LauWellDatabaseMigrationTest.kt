package com.laul.lauwell.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Each migration, run on a database built from the exported schema of the version before
// (app/schemas). Room then checks the result matches the new version's schema exactly.
@RunWith(AndroidJUnit4::class)
class LauWellDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), LauWellDatabase::class.java)

    @Test
    fun `2 to 3 adds the record tables and keeps the cache`() {
        helper.createDatabase(DB, 2).use { db ->
            db.execSQL("INSERT INTO sync_state (metric, changesToken, syncedAt, withHistory) VALUES ('heart_rate', 'token', 1, 1)")
        }
        helper.runMigrationsAndValidate(DB, 3, true).use { db ->
            db.query("SELECT changesToken FROM sync_state WHERE metric = 'heart_rate'").use {
                it.moveToFirst()
                assertEquals("token", it.getString(0))
            }
            db.query("SELECT count(*) FROM record").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    @Test
    fun `3 to 4 adds the sync tables and keeps the records`() {
        helper.createDatabase(DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO record (id, metric_id, shape, time_utc, source, recorded_at, schema_version) " +
                    "VALUES ('r1', 'mood', 'Measurement', 1, 'manual', 1, 1)",
            )
        }
        helper.runMigrationsAndValidate(DB, 4, true).use { db ->
            db.query("SELECT count(*) FROM record").use {
                it.moveToFirst()
                assertEquals(1, it.getInt(0))
            }
            db.query("SELECT count(*) FROM outbox").use {
                it.moveToFirst()
                assertEquals(0, it.getInt(0))
            }
        }
    }

    @Test
    fun `4 to 5 adds the last sync time and keeps the cursor`() {
        helper.createDatabase(DB, 4).use { db ->
            db.execSQL("INSERT INTO sync_cursor (name, uid, change_seq) VALUES ('record', 'alice', 14)")
        }
        helper.runMigrationsAndValidate(DB, 5, true).use { db ->
            db.query("SELECT change_seq, synced_at FROM sync_cursor WHERE name = 'record'").use {
                it.moveToFirst()
                assertEquals(14, it.getInt(0))
                assertEquals(true, it.isNull(1))
            }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
