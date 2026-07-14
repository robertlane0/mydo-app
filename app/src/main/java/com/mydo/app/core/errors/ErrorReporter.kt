package com.mydo.app.core.errors

interface ErrorReporter {
    fun report(error: AppError)
}
