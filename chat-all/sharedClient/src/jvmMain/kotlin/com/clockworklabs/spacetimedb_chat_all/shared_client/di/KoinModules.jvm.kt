package com.clockworklabs.spacetimedb_chat_all.shared_client.di

import com.clockworklabs.spacetimedb_chat_all.shared_client.core.SystemAppDirectories
import com.clockworklabs.spacetimedb_chat_all.shared_client.core.network.HttpClientFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.core.module.Module
import org.koin.dsl.module


actual val sharedPlatformModules: Module
    get() = module {
        single<HttpClientEngine> { OkHttp.create() }
        single<HttpClient> { HttpClientFactory.create(get()) }
        single<SystemAppDirectories> { SystemAppDirectories() }
    }
