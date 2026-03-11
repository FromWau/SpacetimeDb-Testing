package com.clockworklabs.spacetimedb_chat_all.shared_client.core.logger

import platform.Foundation.NSLog

actual val Log: Logger = object : Logger() {
    override fun doLog(logEntry: LogEntry) {
        NSLog(logEntry.toTextString())
    }
}
