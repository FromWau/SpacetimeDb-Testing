package com.clockworklabs.spacetimedb_chat_all.shared_client

import androidx.compose.ui.window.ComposeUIViewController
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.composable.App
import com.clockworklabs.spacetimedb_chat_all.shared_client.di.initKoin

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) { App() }
