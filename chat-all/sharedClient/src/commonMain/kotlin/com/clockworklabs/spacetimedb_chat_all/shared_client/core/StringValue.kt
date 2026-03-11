@file:Suppress("unused")

package com.clockworklabs.spacetimedb_chat_all.shared_client.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
sealed interface StringValue {
    data class Raw(val value: String) : StringValue

    data class Id(
        val id: StringResource,
        val args: ImmutableList<Any> = persistentListOf(),
    ) : StringValue

    @Composable
    fun asString(): String {
        return when (this) {
            is Raw -> value
            is Id -> stringResource(id, *args.toTypedArray())
        }
    }
}

fun StringValue?.orEmpty(): StringValue = this ?: StringValue.Raw("")
