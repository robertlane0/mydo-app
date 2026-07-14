package com.mydo.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MydoApplicationTest {
    @Test
    fun packageNameIsConfigured() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        assertEquals("com.mydo.app", context.packageName)
    }
}
