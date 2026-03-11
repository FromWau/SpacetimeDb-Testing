package com.clockworklabs.spacetimedb_chat_all.shared_client.di

import com.clockworklabs.spacetimedb_chat_all.shared_client.app.data.ChatRepository
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModules = module {
    singleOf(::ChatRepository)
}

val viewModelModules = module {
    viewModelOf(::AppViewModel)
}

expect val sharedPlatformModules: Module
