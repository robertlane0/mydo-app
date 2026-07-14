package com.mydo.app.core.errors

import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorTest {
    @Test
    fun databaseErrorCarriesStructuredCodeAndMessage() {
        val error = DatabaseError(
            code = "database_unavailable",
            userMessage = "Unable to open local data.",
        )

        assertEquals("database_unavailable", error.code)
        assertEquals("Unable to open local data.", error.userMessage)
    }
}
