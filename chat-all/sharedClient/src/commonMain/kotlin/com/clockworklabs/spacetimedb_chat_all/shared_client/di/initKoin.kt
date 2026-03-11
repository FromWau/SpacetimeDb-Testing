package com.clockworklabs.spacetimedb_chat_all.shared_client.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(sharedPlatformModules, sharedModules, viewModelModules)
    }
}
