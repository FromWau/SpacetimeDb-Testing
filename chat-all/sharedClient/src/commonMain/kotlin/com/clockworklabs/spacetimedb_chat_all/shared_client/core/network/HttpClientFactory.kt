package com.clockworklabs.spacetimedb_chat_all.shared_client.core.network

import com.clockworklabs.spacetimedb_chat_all.shared_client.core.logger.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

object HttpClientFactory {
    fun create(
        engine: HttpClientEngine,
    ): HttpClient = HttpClient(engine = engine) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }

        install(Logging) {
            level = LogLevel.ALL
            logger = object : KtorLogger {
                override fun log(message: String) {
                    Log.tag("Ktor").v { message }
                }
            }
        }

        install(WebSockets)

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }

        defaultRequest {
            headers.append("Accept", "application/json")
        }
    }
}