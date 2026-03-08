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
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.CompressionMode
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.DbConnection
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.EventContext
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.Status
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.SubscriptionHandle
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Identity
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import module_bindings.*
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

// --- Chat line model ---

sealed interface ChatLine {
    data class Msg(val id: ULong, val sender: String, val text: String, val sent: Timestamp) : ChatLine
    data class System(val text: String) : ChatLine
}

// --- Token persistence ---

var clientId: String? = null

private fun credentialsPath(): Path {
    val filename = if (clientId != null) "token_all_$clientId" else "token_all"
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

// --- Helpers ---

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

// --- Config ---

val HOST = System.getenv("SPACETIMEDB_HOST") ?: "ws://localhost:3000"
val DB_NAME = System.getenv("SPACETIMEDB_DB_NAME") ?: "chat-all"

fun main(args: Array<String>) {
    clientId = args.indexOf("--client").let { if (it >= 0) args[it + 1] else null }
    val title = if (clientId != null) "SpacetimeDB Chat All — Client $clientId" else "SpacetimeDB Chat All"

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
    val notes = remember { mutableStateListOf<Note>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Track subscription handles for unsubscribe testing
    var mainSubHandle by remember { mutableStateOf<SubscriptionHandle?>(null) }
    var noteSubHandle by remember { mutableStateOf<SubscriptionHandle?>(null) }

    fun refreshUsers(db: RemoteTables) {
        val all = db.user.all()
        onlineUsers.clear()
        onlineUsers.addAll(all.filter { it.online })
        offlineUsers.clear()
        offlineUsers.addAll(all.filter { !it.online })
    }

    fun refreshNotes(db: RemoteTables) {
        notes.clear()
        notes.addAll(db.note.all())
    }

    fun log(text: String) {
        lines += ChatLine.System(text)
    }

    LaunchedEffect(Unit) {
        val connection = DbConnection.Builder()
            .withUri(HOST)
            .withDatabaseName(DB_NAME)
            .withToken(loadToken())
            .withModuleBindings()
            // --- Builder options under test ---
            .withCompression(CompressionMode.GZIP)
            .withLightMode(false)
            .onConnect { c, identity, token ->
                localIdentity = identity
                saveToken(token)
                log("Identity: ${identity.toHexString().take(16)}...")
                log("Compression: GZIP, LightMode: false")

                // --- Table callbacks ---

                // onInsert (User)
                c.db.user.onInsert { ctx, user ->
                    refreshUsers(c.db)
                    if (ctx !is EventContext.SubscribeApplied && user.online) {
                        log("${userNameOrIdentity(user)} is online")
                    }
                }

                // onUpdate (User)
                c.db.user.onUpdate { _, oldUser, newUser ->
                    refreshUsers(c.db)
                    if (oldUser.name != newUser.name) {
                        log("${userNameOrIdentity(oldUser)} renamed to ${newUser.name}")
                    }
                    if (oldUser.online != newUser.online) {
                        if (newUser.online) {
                            log("${userNameOrIdentity(newUser)} connected.")
                        } else {
                            log("${userNameOrIdentity(newUser)} disconnected.")
                        }
                    }
                }

                // onInsert (Message)
                c.db.message.onInsert { ctx, message ->
                    if (ctx is EventContext.SubscribeApplied) return@onInsert
                    lines += ChatLine.Msg(message.id, senderName(c.db, message.sender), message.text, message.sent)
                }

                // onDelete (Message) — tests onDelete callback
                c.db.message.onDelete { ctx, message ->
                    if (ctx is EventContext.SubscribeApplied) return@onDelete
                    lines.removeAll { it is ChatLine.Msg && it.id == message.id }
                    log("Message #${message.id} deleted")
                }

                // onInsert (Note) — tests additional table callbacks
                c.db.note.onInsert { ctx, _ ->
                    if (ctx is EventContext.SubscribeApplied) return@onInsert
                    refreshNotes(c.db)
                }

                // onDelete (Note)
                c.db.note.onDelete { ctx, note ->
                    if (ctx is EventContext.SubscribeApplied) return@onDelete
                    refreshNotes(c.db)
                    log("Note #${note.id} deleted")
                }

                // onInsert (Reminder) — tests ScheduleAt type in generated bindings
                c.db.reminder.onInsert { ctx, reminder ->
                    if (ctx is EventContext.SubscribeApplied) return@onInsert
                    log("Reminder scheduled: \"${reminder.text}\" (id=${reminder.scheduledId})")
                }

                // onDelete (Reminder) — fires when the scheduled reducer completes
                c.db.reminder.onDelete { ctx, reminder ->
                    if (ctx is EventContext.SubscribeApplied) return@onDelete
                    log("Reminder consumed: \"${reminder.text}\" (id=${reminder.scheduledId})")
                }

                // --- Reducer callbacks ---

                c.reducers.onSetName { ctx, name ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        log("Failed to change name to $name: ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.reducers.onSendMessage { ctx, text ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        log("Failed to send message \"$text\": ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.reducers.onDeleteMessage { ctx, messageId ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        log("Failed to delete message #$messageId: ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.reducers.onAddNote { ctx, content, tag ->
                    if (ctx.callerIdentity == localIdentity) {
                        if (ctx.status is Status.Committed) {
                            log("Note added (tag=$tag)")
                        } else if (ctx.status is Status.Failed) {
                            log("Failed to add note: ${(ctx.status as Status.Failed).message}")
                        }
                    }
                }

                c.reducers.onDeleteNote { ctx, noteId ->
                    if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                        log("Failed to delete note #$noteId: ${(ctx.status as Status.Failed).message}")
                    }
                }

                c.reducers.onScheduleReminder { ctx, text, delayMs ->
                    if (ctx.callerIdentity == localIdentity) {
                        if (ctx.status is Status.Committed) {
                            log("Reminder scheduled in ${delayMs}ms: \"$text\"")
                        } else if (ctx.status is Status.Failed) {
                            log("Failed to schedule reminder: ${(ctx.status as Status.Failed).message}")
                        }
                    }
                }

                c.reducers.onCancelReminder { ctx, reminderId ->
                    if (ctx.callerIdentity == localIdentity) {
                        if (ctx.status is Status.Committed) {
                            log("Reminder #$reminderId cancelled")
                        } else if (ctx.status is Status.Failed) {
                            log("Failed to cancel reminder #$reminderId: ${(ctx.status as Status.Failed).message}")
                        }
                    }
                }

                c.reducers.onScheduleReminderRepeat { ctx, text, intervalMs ->
                    if (ctx.callerIdentity == localIdentity) {
                        if (ctx.status is Status.Committed) {
                            log("Repeating reminder every ${intervalMs}ms: \"$text\"")
                        } else if (ctx.status is Status.Failed) {
                            log("Failed to schedule repeating reminder: ${(ctx.status as Status.Failed).message}")
                        }
                    }
                }

                // --- Subscriptions ---

                // Main subscription: user + message + reminder tables
                mainSubHandle = c.subscriptionBuilder()
                    .onApplied { ctx ->
                        connected = true
                        refreshUsers(ctx.db)
                        ctx.db.message.all()
                            .sortedBy { it.sent }
                            .forEach { msg ->
                                lines += ChatLine.Msg(msg.id, senderName(ctx.db, msg.sender), msg.text, msg.sent)
                            }
                        log("Main subscription applied.")
                    }
                    .onError { _, error ->
                        log("Main subscription error: $error")
                    }
                    .subscribe(listOf(
                        "SELECT * FROM user",
                        "SELECT * FROM message",
                        "SELECT * FROM reminder",
                    ))

                // Filtered subscription: only notes (tests filtered subscribe + separate handle)
                noteSubHandle = c.subscriptionBuilder()
                    .onApplied { ctx ->
                        refreshNotes(ctx.db)
                        log("Note subscription applied (${notes.size} notes).")
                    }
                    .onError { _, error ->
                        log("Note subscription error: $error")
                    }
                    .subscribe("SELECT * FROM note")
            }
            .onConnectError { _, e ->
                log("Connection error: $e")
            }
            .onDisconnect { _, error ->
                connected = false
                onlineUsers.clear()
                offlineUsers.clear()
                notes.clear()
                if (error != null) {
                    log("Disconnected abnormally: $error")
                } else {
                    log("Disconnected.")
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
                                        "#${line.id} ${line.sender}: ${line.text}",
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

                    // Command help
                    Text(
                        "/name | /del | /note | /delnote | /unsub | /resub | /query | /squery | /remind | /remind-repeat | /remind-cancel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        fun send() {
                            val text = input.trim()
                            if (text.isEmpty()) return
                            val c = conn ?: return
                            input = ""

                            val parts = text.trimStart().split(" ", limit = 2)
                            val cmd = parts[0]
                            val arg = parts.getOrElse(1) { "" }

                            when (cmd) {
                                "/name" -> {
                                    c.reducers.setName(arg)
                                }
                                "/del" -> {
                                    val id = arg.trim().toULongOrNull()
                                    if (id != null) {
                                        c.reducers.deleteMessage(id)
                                    } else {
                                        log("Usage: /del <message_id>")
                                    }
                                }
                                "/note" -> {
                                    val noteParts = arg.trim().split(" ", limit = 2)
                                    if (noteParts.size == 2) {
                                        c.reducers.addNote(noteParts[1], noteParts[0])
                                    } else {
                                        log("Usage: /note <tag> <content>")
                                    }
                                }
                                "/delnote" -> {
                                    val id = arg.trim().toULongOrNull()
                                    if (id != null) {
                                        c.reducers.deleteNote(id)
                                    } else {
                                        log("Usage: /delnote <note_id>")
                                    }
                                }
                                "/unsub" -> {
                                    val handle = noteSubHandle
                                    if (handle != null && handle.isActive) {
                                        handle.unsubscribeThen { ctx ->
                                            notes.clear()
                                            log("Note subscription unsubscribed.")
                                        }
                                    } else {
                                        log("Note subscription is not active (state: ${handle?.state})")
                                    }
                                }
                                "/resub" -> {
                                    noteSubHandle = c.subscriptionBuilder()
                                        .onApplied { ctx ->
                                            refreshNotes(ctx.db)
                                            log("Note subscription re-applied (${notes.size} notes).")
                                        }
                                        .onError { _, error ->
                                            log("Note subscription error: $error")
                                        }
                                        .subscribe("SELECT * FROM note")
                                    log("Re-subscribing to notes...")
                                }
                                "/query" -> {
                                    val sql = arg.trim()
                                    if (sql.isEmpty()) {
                                        log("Usage: /query <sql>")
                                    } else {
                                        c.oneOffQuery(sql) { result ->
                                            when (val r = result.result) {
                                                is com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.protocol.QueryResult.Ok ->
                                                    log("OneOffQuery OK: ${r.rows.tables.size} table(s)")
                                                is com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.protocol.QueryResult.Err ->
                                                    log("OneOffQuery error: ${r.error}")
                                            }
                                        }
                                        log("Executing: $sql")
                                    }
                                }
                                "/squery" -> {
                                    // Suspend variant of oneOffQuery
                                    val sql = arg.trim()
                                    if (sql.isEmpty()) {
                                        log("Usage: /squery <sql>")
                                    } else {
                                        log("Executing (suspend): $sql")
                                        scope.launch(Dispatchers.Default) {
                                            val result = c.oneOffQuery(sql)
                                            when (val r = result.result) {
                                                is com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.protocol.QueryResult.Ok ->
                                                    log("SuspendQuery OK: ${r.rows.tables.size} table(s)")
                                                is com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.protocol.QueryResult.Err ->
                                                    log("SuspendQuery error: ${r.error}")
                                            }
                                        }
                                    }
                                }
                                "/remind" -> {
                                    // /remind <delay_ms> <text>
                                    val remindParts = arg.trim().split(" ", limit = 2)
                                    val delayMs = remindParts.getOrNull(0)?.toULongOrNull()
                                    val remindText = remindParts.getOrNull(1)
                                    if (delayMs != null && remindText != null) {
                                        c.reducers.scheduleReminder(remindText, delayMs)
                                    } else {
                                        log("Usage: /remind <delay_ms> <text>")
                                    }
                                }
                                "/remind-cancel" -> {
                                    val id = arg.trim().toULongOrNull()
                                    if (id != null) {
                                        c.reducers.cancelReminder(id)
                                    } else {
                                        log("Usage: /remind-cancel <reminder_id>")
                                    }
                                }
                                "/remind-repeat" -> {
                                    // /remind-repeat <interval_ms> <text>
                                    val remindParts = arg.trim().split(" ", limit = 2)
                                    val intervalMs = remindParts.getOrNull(0)?.toULongOrNull()
                                    val remindText = remindParts.getOrNull(1)
                                    if (intervalMs != null && remindText != null) {
                                        c.reducers.scheduleReminderRepeat(remindText, intervalMs)
                                    } else {
                                        log("Usage: /remind-repeat <interval_ms> <text>")
                                    }
                                }
                                else -> {
                                    c.reducers.sendMessage(text)
                                }
                            }
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
                            placeholder = { Text("Type a message or command...") },
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

                // Sidebar
                VerticalDivider()
                Column(
                    modifier = Modifier.width(200.dp).fillMaxHeight().padding(8.dp),
                ) {
                    // Online/Offline users
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

                    // Notes section
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "sub: ${noteSubHandle?.state ?: "none"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    if (notes.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    notes.forEach { note ->
                        Text(
                            "#${note.id} [${note.tag}] ${note.content}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}
