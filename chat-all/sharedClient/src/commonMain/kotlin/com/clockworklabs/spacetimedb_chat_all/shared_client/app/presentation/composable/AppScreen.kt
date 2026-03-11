package com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.composable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppState
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                is AppState.Login -> LoginScreen(
                    state = s,
                    onAction = viewModel::onAction,
                )

                is AppState.Chat -> ChatScreen(
                    state = s,
                    onAction = viewModel::onAction,
                )
            }
        }
    }
}
