@file:Suppress("unused")

package com.clockworklabs.spacetimedb_chat_all.shared_client.core.result

import com.clockworklabs.spacetimedb_chat_all.shared_client.core.result.Error as IError

sealed interface Result<out T, out E : IError> {
    data class Success<out T>(val data: T) : Result<T, Nothing>
    data class Error<out E : IError>(val error: E) : Result<Nothing, E>
}

inline fun <T, E : IError, R> Result<T, E>.map(map: (T) -> R): Result<R, E> = when (this) {
    is Result.Error -> Result.Error(error)
    is Result.Success -> Result.Success(map(data))
}

fun <T, E : IError> Result<T, E>.asEmptyDataResult(): EmptyResult<E> = map { }

inline fun <T, E : IError> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> {
    return when (this) {
        is Result.Error -> this
        is Result.Success -> {
            action(data)
            this
        }
    }
}

inline fun <T, E : IError> Result<T, E>.onError(action: (E) -> Unit): Result<T, E> =
    when (this) {
        is Result.Error -> {
            action(error)
            this
        }

        is Result.Success -> this
    }

typealias EmptyResult<E> = Result<Unit, E>
