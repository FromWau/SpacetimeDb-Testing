package com.clockworklabs.spacetimedb_chat_all.android_app

import android.app.Application
import com.clockworklabs.spacetimedb_chat_all.shared_client.di.initKoin
import org.koin.android.ext.koin.androidContext

class ChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidContext(this@ChatApplication)
        }
    }
}