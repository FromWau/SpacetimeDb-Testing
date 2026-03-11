@file:Suppress("unused")
@file:OptIn(ExperimentalTime::class)

package com.clockworklabs.spacetimedb_chat_all.shared_client.core.logger

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

expect val Log: Logger


data class LogEntry(
    val timestamp: Instant,
    val tag: String,
    val logLevel: LogLevel,
    val message: String,
)

internal fun LogEntry.toTextString(): String {
    val timestampLocal = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    val formattedDate = formatDateTime(timestampLocal)
    val level = logLevel.name.padEnd(7)
    return "$formattedDate $level ${tag.take(35)} - $message"
}

enum class LogLevel {
    VERBOSE, DEBUG, INFO, WARN, ERROR
}

class TaggedLogger(
    private val tag: String,
    private val delegate: Logger,
) {
    fun v(msg: () -> String) = delegate.log(tag, LogLevel.VERBOSE, msg)
    fun d(msg: () -> String) = delegate.log(tag, LogLevel.DEBUG, msg)
    fun i(msg: () -> String) = delegate.log(tag, LogLevel.INFO, msg)
    fun w(msg: () -> String) = delegate.log(tag, LogLevel.WARN, msg)
    fun e(msg: () -> String) = delegate.log(tag, LogLevel.ERROR, msg)
    fun e(throwable: Throwable) = delegate.log(tag, LogLevel.ERROR, throwable)
    fun e(throwable: Throwable, msg: () -> String) {
        delegate.log(tag, LogLevel.ERROR, throwable)
        delegate.log(tag, LogLevel.ERROR, msg)
    }
}

abstract class Logger {
    companion object {
        private const val TAG = "Logger"
        private val LOG_LEVEL = LogLevel.DEBUG
    }

    fun log(tag: String, logLevel: LogLevel, lazyMessage: () -> String) {
        if (logLevel >= LOG_LEVEL) {
            val entry = LogEntry(
                timestamp = Clock.System.now(),
                tag = tag,
                logLevel = logLevel,
                message = lazyMessage()
            )

            doLog(entry)
        }
    }

    fun log(tag: String, logLevel: LogLevel, throwable: Throwable) {
        log(tag, logLevel) { throwable.stackTraceToString() }
    }

    fun tag(tag: String): TaggedLogger = TaggedLogger(tag, this)

    protected abstract fun doLog(logEntry: LogEntry)
}


private fun formatDateTime(dt: LocalDateTime): String {
    val year = dt.year.toString().padStart(4, '0')
    val month = dt.month.number.toString().padStart(2, '0')
    val day = dt.day.toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val minute = dt.minute.toString().padStart(2, '0')
    val second = dt.second.toString().padStart(2, '0')
    val millisecond = (dt.nanosecond / 1_000_000).toString().padStart(3, '0')

    return "$year-$month-$day $hour:$minute:$second.$millisecond"
}
