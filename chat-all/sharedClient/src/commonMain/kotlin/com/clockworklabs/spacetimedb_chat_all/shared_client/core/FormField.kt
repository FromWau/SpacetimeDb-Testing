@file:Suppress("unused")

package com.clockworklabs.spacetimedb_chat_all.shared_client.core

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class FormField<T>(
    val value: T,
    val errors: ImmutableList<StringValue> = persistentListOf(),
) {
    val isValid: Boolean get() = errors.isEmpty()
    val isInvalid: Boolean get() = errors.isNotEmpty()
}


inline fun <T> FormField<T>.ifValid(action: () -> Unit) {
    if (isValid) action()
}

inline fun <T> FormField<T>.ifInvalid(action: () -> Unit) {
    if (isInvalid) action()
}

inline fun <T> FormField<T>.onValid(action: (T) -> Unit): FormField<T> {
    if (isValid) action(value)
    return this
}

inline fun <T> FormField<T>.onInvalid(action: (ImmutableList<StringValue>) -> Unit): FormField<T> {
    if (isInvalid) action(errors)
    return this
}