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

    private companion object {
        const val DB = "migration-test.db"
    }
}
