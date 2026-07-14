package com.mydo.app.core.errors

sealed interface AppError {
    val code: String
    val userMessage: String
    val cause: Throwable?
}

data class DatabaseError(
    override val code: String,
    override val userMessage: String,
    override val cause: Throwable? = null,
) : AppError

data class PermissionDeniedError(
    override val code: String,
    override val userMessage: String,
    override val cause: Throwable? = null,
) : AppError

data class ValidationError(
    override val code: String,
    override val userMessage: String,
    override val cause: Throwable? = null,
) : AppError

data class UnknownAppError(
    override val code: String = "unknown",
    override val userMessage: String = "Something went wrong.",
    override val cause: Throwable? = null,
) : AppError
