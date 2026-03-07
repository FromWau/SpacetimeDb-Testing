import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.DbConnection
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.EventContext
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.Status
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Identity
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Timestamp
import kotlinx.coroutines.awaitCancellation
import module_bindings.*
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

sealed interface ChatLine {
    data class Msg(val sender: String, val text: String, val sent: Timestamp) : ChatLine
    data class System(val text: String) : ChatLine
}

var clientId: String? = null

private fun credentialsPath(): Path {
    val filename = if (clientId != null) "token_compose_$clientId" else "token_compose"
    return Path.of(System.getProperty("user.home"), ".spacetime_kotlin_quickstart", filename)
}

private fun loadToken(): String? {
    val path = credentialsPath()
    return if (path.exists()) path.readText().trim() else null
}

private fun saveToken(token: String) {
    val path = credentialsPath()
    path.parent.createDirectories()
    path.writeText(token)
}

private fun userNameOrIdentity(user: User): String =
    user.name ?: user.identity.toHexString().take(8)

private fun senderName(db: RemoteTables, sender: Identity): String {
    val user = db.user.identity.find(sender)
    return if (user != null) userNameOrIdentity(user) else "unknown"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm")

private fun formatTimestamp(ts: Timestamp): String {
    val zoned = ts.instant.toJavaInstant().atZone(ZoneId.systemDefault())
    val isToday = zoned.toLocalDate() == LocalDate.now()
    return if (isToday) zoned.format(timeFormatter) else zoned.format(dateTimeFormatter)
}

private fun kotlin.time.Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(epochSeconds, nanosecondsOfSecond.toLong())

val HOST = System.getenv("SPACETIMEDB_HOST") ?: "ws://localhost:3000"
val DB_NAME = System.getenv("SPACETIMEDB_DB_NAME") ?: "chat-compose"

fun main(args: Array<String>) {
    clientId = args.indexOf("--client").let { if (it >= 0) args[it + 1] else null }
    val title = if (clientId != null) "SpacetimeDB Chat — Client $clientId" else "SpacetimeDB Chat"

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = title,
        ) {
            ChatApp()
        }
    }
}

@Composable
fun ChatApp() {
    val lines = remember { mutableStateListOf<ChatLine>() }
    var input by remember { mutableStateOf("") }
    var conn by remember { mutableStateOf<DbConnection?>(null) }
    var localIdentity by remember { mutableStateOf<Identity?>(null) }
    var connected by remember { mutableStateOf(false) }
    val onlineUsers = remember { mutableStateListOf<User>() }
    val offlineUsers = remember { mutableStateListOf<User>() }
    val listState = rememberLazyListState()

    fun refreshUsers(db: RemoteTables) {
        val all = db.user.all()
        onlineUsers.clear()
        onlineUsers.addAll(all.filter { it.online })
        offlineUsers.clear()
        offlineUsers.addAll(all.filter { !it.online })
    }

    LaunchedEffect(Unit) {
        val connection = DbConnection.Builder()
            .withUri(HOST)
            .withDatabaseName(DB_NAME)
            .withToken(loadToken())
            .withModuleBindings()
            .onConnect { c, identity, token ->
                localIdentity = identity
                saveToken(token)

                c.db.user.onInsert { _, user ->
                    refreshUsers(c.db)
                    if (user.online) {
                        lines += ChatLine.System("${userNameOrIdentity(user)} is online")
                    }
                }

                c.db.user.onUpdate { _, oldUser, newUser ->
                    refreshUsers(c.db)
                    if (oldUser.name != newUser.name) {
                        lines += ChatLine.System("${userNameOrIdentity(oldUser)} renamed to ${newUser.name}")
                    }
                    if (oldUser.online != newUser.online) {
                        if (newUser.online) {
                            lines += ChatLine.System("${userNameOrIdentity(newUser)} connected.")
                        } else {
                            lines += ChatLine.System("${userNameOrIdentity(newUser)} disconnected.")
                        }
                    }
                }

                c.db.message.onInsert { ctx, message ->
                    if (ctx is EventContext.SubscribeApplied) return@onInsert
                    lines += ChatLine.Msg(senderName(c.db, message.sender), message.text, message.sent)
                }

                c.reducers.onSetName { ctx, name ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        lines += ChatLine.System("Failed to change name to $name: ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.reducers.onSendMessage { ctx, text ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        lines += ChatLine.System("Failed to send message \"$text\": ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.subscriptionBuilder()
                    .onApplied { ctx ->
                        connected = true
                        refreshUsers(ctx.db)
                        ctx.db.message.all()
                            .sortedBy { it.sent }
                            .forEach { msg ->
                                lines += ChatLine.Msg(senderName(ctx.db, msg.sender), msg.text, msg.sent)
                            }
                    }
                    .subscribeToAllTables()
            }
            .onConnectError { _, e ->
                lines += ChatLine.System("Connection error: $e")
            }
            .onDisconnect { _, error ->
                connected = false
                onlineUsers.clear()
                offlineUsers.clear()
                if (error != null) {
                    lines += ChatLine.System("Disconnected abnormally: $error")
                } else {
                    lines += ChatLine.System("Disconnected.")
                }
            }
            .build()

        conn = connection

        try {
            awaitCancellation()
        } finally {
            connection.disconnect()
        }
    }

    // Auto-scroll when new lines arrive
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Chat panel
                Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (!connected) {
                            item {
                                Text(
                                    "Connecting to $DB_NAME...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(lines) { line ->
                            when (line) {
                                is ChatLine.Msg -> Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${line.sender}: ${line.text}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        formatTimestamp(line.sent),
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        fun send() {
                            val text = input.trim()
                            if (text.isEmpty()) return
                            val c = conn ?: return
                            if (text.startsWith("/name ")) {
                                c.reducers.setName(text.removePrefix("/name "))
                            } else {
                                c.reducers.sendMessage(text)
                            }
                            input = ""
                        }

                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                                        send()
                                        true
                                    } else false
                                },
                            placeholder = { Text("Type a message or /name <name>") },
                            singleLine = true,
                            enabled = connected,
                        )

                        Spacer(Modifier.width(8.dp))

                        Button(
                            onClick = { send() },
                            enabled = connected && input.isNotBlank(),
                        ) {
                            Text("Send")
                        }
                    }
                }

                // User sidebar
                VerticalDivider()
                Column(
                    modifier = Modifier.width(180.dp).fillMaxHeight().padding(8.dp),
                ) {
                    Text("Online", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    if (onlineUsers.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    onlineUsers.forEach { user ->
                        Text(
                            userNameOrIdentity(user),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (offlineUsers.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Offline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        offlineUsers.forEach { user ->
                            Text(
                                userNameOrIdentity(user),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
