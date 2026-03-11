package com.clockworklabs.spacetimedb_chat_all.desktop_app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.composable.App
import com.clockworklabs.spacetimedb_chat_all.shared_client.di.initKoin

fun main() {
    initKoin()

    // INFO: Skiko is slow on default settings, especially on Linux. To improve performance, we can change the rendering API.
    //  Choose one: SOFTWARE, SOFTWARE_FAST, OPENGL, METAL (Mac), VULKAN (Linux/Windows)
    System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Chat All",
        ) {
            App()
        }
    }
}
