package com.clockworklabs.spacetimedb_chat_all.shared_client.core.network

fun String.ensureProtocol(): String = when {
    this.startsWith("https://") -> this
    this.startsWith("http://") -> this
    else -> "https://$this"
}
