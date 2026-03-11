package com.clockworklabs.spacetimedb_chat_all.shared_client.core.logger

import android.util.Log as AndroidLog

actual val Log: Logger = object : Logger() {
    override fun doLog(
        logEntry: LogEntry,
    ) {
        when (logEntry.logLevel) {
            LogLevel.VERBOSE -> AndroidLog.v(logEntry.tag, logEntry.message)
            LogLevel.DEBUG -> AndroidLog.d(logEntry.tag, logEntry.message)
            LogLevel.INFO -> AndroidLog.i(logEntry.tag, logEntry.message)
            LogLevel.WARN -> AndroidLog.w(logEntry.tag, logEntry.message)
            LogLevel.ERROR -> AndroidLog.e(logEntry.tag, logEntry.message)
        }
    }
}
