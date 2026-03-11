package com.clockworklabs.spacetimedb_chat_all.shared_client.core.logger

actual val Log: Logger = object : Logger() {
    override fun doLog(logEntry: LogEntry) {
        println(logEntry.toTextString())
    }
}
