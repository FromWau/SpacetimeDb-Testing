package com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation

sealed interface AppAction {
    sealed interface Login : AppAction {
        data class OnClientChanged(val client: String?) : Login
        data object OnSubmitClicked : Login
    }

    sealed interface Chat : AppAction {
        data class UpdateInput(val input: String?) : Chat
        data object Submit : Chat
        data object Logout : Chat
    }
}
