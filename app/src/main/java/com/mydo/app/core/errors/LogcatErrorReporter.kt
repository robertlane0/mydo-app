package com.mydo.app.core.errors

import android.util.Log

class LogcatErrorReporter : ErrorReporter {
    override fun report(error: AppError) {
        Log.w("MyDo", "${error.code}: ${error.userMessage}", error.cause)
    }
}
