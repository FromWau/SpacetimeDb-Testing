package com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppAction
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppState
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppState.Chat.ChatLine
import com.clockworklabs.spacetimedb_chat_all.shared_client.app.presentation.AppViewModel

@Composable
fun ChatScreen(
    state: AppState.Chat,
    onAction: (AppAction.Chat) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(state.lines.size - 1)
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        ChatPanel(
            state = state,
            onAction = onAction,
            listState = listState,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        VerticalDivider()

        Sidebar(
            onlineUsers = state.onlineUsers,
            offlineUsers = state.offlineUsers,
            notes = state.notes,
            noteSubState = state.noteSubState,
            modifier = Modifier.width(200.dp).fillMaxHeight(),
        )
    }
}

@Composable
private fun ChatPanel(
    state: AppState.Chat,
    onAction: (AppAction.Chat) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (!state.connected) {
                item {
                    Text(
                        "Connecting to ${state.dbName}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.lines) { line ->
                when (line) {
                    is ChatLine.Msg -> Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "#${line.id} ${line.sender}: ${line.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            AppViewModel.formatTimeStamp(line.sent),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is ChatLine.System -> Text(
                        line.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "/name | /del | /note | /delnote | /unsub | /resub | /query | /squery | /remind | /remind-repeat | /remind-cancel | /test-rmcb",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))

        ChatInput(
            value = state.input.value ?: "",
            enabled = state.connected,
            canSend = state.connected && !state.input.value.isNullOrBlank(),
            onValueChange = { onAction(AppAction.Chat.UpdateInput(it)) },
            onSubmit = { onAction(AppAction.Chat.Submit) },
        )
    }
}

@Composable
private fun ChatInput(
    value: String,
    enabled: Boolean,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        onSubmit()
                        true
                    } else false
                },
            placeholder = { Text("Type a message or command...") },
            singleLine = true,
            enabled = enabled,
        )

        Spacer(Modifier.width(8.dp))

        Button(onClick = onSubmit, enabled = canSend) {
            Text("Send")
        }
    }
}

@Composable
private fun Sidebar(
    onlineUsers: kotlinx.collections.immutable.ImmutableList<String>,
    offlineUsers: kotlinx.collections.immutable.ImmutableList<String>,
    notes: kotlinx.collections.immutable.ImmutableList<AppState.Chat.NoteUi>,
    noteSubState: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Text(
            "Online",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        if (onlineUsers.isEmpty()) {
            Text(
                "\u2014",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        onlineUsers.forEach { name ->
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (offlineUsers.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "Offline",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            offlineUsers.forEach { name ->
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text(
            "Notes",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "sub: $noteSubState",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        if (notes.isEmpty()) {
            Text(
                "\u2014",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        notes.forEach { note ->
            Text(
                "#${note.id} [${note.tag}] ${note.content}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
