package com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation

import androidx.compose.runtime.Immutable
import com.clockworklabs.spacetimedb_chat_all.shared_client.core.FormField
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Timestamp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
sealed interface AppState {
    @Immutable
    data class Login(val clientField: FormField<String?> = FormField(null)) : AppState

    @Immutable
    data class Chat(
        val lines: ImmutableList<ChatLine> = persistentListOf(),
        val input: FormField<String?> = FormField(null),
        val connected: Boolean = false,
        val onlineUsers: ImmutableList<String> = persistentListOf(),
        val offlineUsers: ImmutableList<String> = persistentListOf(),
        val notes: ImmutableList<NoteUi> = persistentListOf(),
        val noteSubState: String = "none",
        val dbName: String = "",
    ) : AppState {

        @Immutable
        sealed interface ChatLine {
            @Immutable
            data class Msg(
                val id: ULong,
                val sender: String,
                val text: String,
                val sent: Timestamp,
            ) : ChatLine

            @Immutable
            data class System(val text: String) : ChatLine
        }

        @Immutable
        data class NoteUi(
            val id: ULong,
            val tag: String,
            val content: String,
        )
    }
}
