package com.mydo.app.core.time

interface TimeProvider {
    fun nowUtcMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun nowUtcMillis(): Long = System.currentTimeMillis()
}
