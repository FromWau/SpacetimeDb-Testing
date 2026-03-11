package com.clockworklabs.spacetimedb_chat_all.shared_client.core

import kotlinx.io.files.Path

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class SystemAppDirectories {
    private val os = System.getProperty("os.name").lowercase()
    private val userHome = System.getProperty("user.home")
    private val appName = "spacetimedb-chat-all"

    actual fun dataDir(): Path {
        return when {
            os.contains("win") -> Path(System.getenv("APPDATA"), appName)
            os.contains("mac") -> Path(userHome, "Library/Application Support/$appName")
            else -> {
                System.getenv("XDG_DATA_HOME")
                    ?.let { Path(it, appName) }
                    ?: Path("$userHome/.local/share/$appName")
            }
        }
    }
}
