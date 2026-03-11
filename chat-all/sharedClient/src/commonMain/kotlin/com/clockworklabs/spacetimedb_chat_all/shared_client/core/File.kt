package com.clockworklabs.spacetimedb_chat_all.shared_client.core

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "Unused")
expect class SystemAppDirectories {
    fun dataDir(): Path
}

val Path.fileExtension: String?
    get() {
        if (SystemFileSystem.metadataOrNull(this)?.isRegularFile != true) return null

        val extension = this.name.substringAfter(".")
        return if (extension == this.name) null else extension
    }

val Path.nameWithoutExtension: String
    get() {
        val extension = this.fileExtension ?: return this.name
        return this.name.removeSuffix(".$extension")
    }

fun loadToken(
    systemAppDirectories: SystemAppDirectories,
    clientId: String,
): String? {
    val path = Path(systemAppDirectories.dataDir(), "token", clientId)
    if (!SystemFileSystem.exists(path)) return null

    return SystemFileSystem.source(path).buffered().use { it.readString() }
}

fun saveToken(
    systemAppDirectories: SystemAppDirectories,
    clientId: String,
    token: String,
) {
    val path = Path(systemAppDirectories.dataDir(), "token", clientId)
    SystemFileSystem.createDirectories(
        Path(systemAppDirectories.dataDir(), "token"),
        mustCreate = false
    )

    SystemFileSystem.sink(path).buffered().use { it.writeString(token) }
}
