package com.mydo.app.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MydoDatabase::class.java.canonicalName!!,
        FrameworkSQLiteOpenHelperFactory()
    )

    @org.junit.Ignore("Robolectric asset loading issue with MigrationTestHelper")
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        // Create the database in version 1
        var db = helper.createDatabase(TEST_DB, 1)

        // db has schema version 1. insert some data using SQL queries.
        // We know v1 has `preferences` table.
        db.execSQL("INSERT INTO preferences (key, value) VALUES ('test_key', 'test_val')")

        // Prepare for the next version
        db.close()

        // Re-open the database with version 2 and provide MIGRATION_1_2
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // Check if v2 tables exist
        val cursor = db.query("SELECT * FROM projects")
        assert(cursor.count == 0) // The table exists but is empty
        cursor.close()
    }
}
