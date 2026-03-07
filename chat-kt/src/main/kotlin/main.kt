import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.DbConnection
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.EventContext
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.Status
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.type.Identity
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import module_bindings.*
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

var localIdentity: Identity? = null

private fun credentialsPath(clientId: String?): Path {
    val filename = if (clientId != null) "token_$clientId" else "token"
    return Path.of(System.getProperty("user.home"), ".spacetime_kotlin_quickstart", filename)
}

fun loadToken(clientId: String?): String? {
    val path = credentialsPath(clientId)
    return if (path.exists()) path.readText().trim() else null
}

fun saveToken(clientId: String?, token: String) {
    val path = credentialsPath(clientId)
    path.parent.createDirectories()
    path.writeText(token)
}

fun userNameOrIdentity(user: User): String =
    user.name ?: user.identity.toHexString().take(8)

fun printMessage(db: RemoteTables, message: Message) {
    val sender = db.user.identity.find(message.sender)
    val senderName = if (sender != null) userNameOrIdentity(sender) else "unknown"
    println("$senderName: ${message.text}")
}

val HOST = System.getenv("SPACETIMEDB_HOST") ?: "ws://localhost:3000"
val DB_NAME = System.getenv("SPACETIMEDB_DB_NAME") ?: "chat-kt"

fun main(args: Array<String>): Unit = runBlocking {
    val clientId = args.indexOf("--client").let { if (it >= 0) args[it + 1] else null }

    DbConnection.Builder()
        .withUri(HOST)
        .withDatabaseName(DB_NAME)
        .withToken(loadToken(clientId))
        .withModuleBindings()
        .onConnect { conn, identity, token ->
            localIdentity = identity
            saveToken(clientId, token)

            conn.db.user.onInsert { _, user ->
                if (user.online) {
                    println("${userNameOrIdentity(user)} is online")
                }
            }

            conn.db.user.onUpdate { _, oldUser, newUser ->
                if (oldUser.name != newUser.name) {
                    println("${userNameOrIdentity(oldUser)} renamed to ${newUser.name}")
                }
                if (oldUser.online != newUser.online) {
                    if (newUser.online) {
                        println("${userNameOrIdentity(newUser)} connected.")
                    } else {
                        println("${userNameOrIdentity(newUser)} disconnected.")
                    }
                }
            }

            conn.db.message.onInsert { ctx, message ->
                if (ctx is EventContext.SubscribeApplied) return@onInsert
                printMessage(conn.db, message)
            }

            conn.reducers.onSetName { ctx, name ->
                if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                    println("Failed to change name to $name: ${(ctx.status as Status.Failed).message}")
                }
            }

            conn.reducers.onSendMessage { ctx, text ->
                if (ctx.callerIdentity == localIdentity && ctx.status is Status.Failed) {
                    println("Failed to send message \"$text\": ${(ctx.status as Status.Failed).message}")
                }
            }

            conn.subscriptionBuilder()
                .onApplied { ctx ->
                    println("Connected.")
                    ctx.db.message.all()
                        .sortedBy { it.sent }
                        .forEach { printMessage(ctx.db, it) }
                }
                .subscribeToAllTables()
        }
        .onConnectError { _, e ->
            println("Connection error: $e")
        }
        .onDisconnect { _, error ->
            if (error != null) {
                println("Disconnected abnormally: $error")
            } else {
                println("Disconnected normally.")
            }
        }
        .build()
        .use {
            withContext(Dispatchers.IO) {
                while (true) {
                    val line = readlnOrNull() ?: break
                    if (line.startsWith("/name ")) {
                        it.reducers.setName(line.removePrefix("/name "))
                    } else {
                        it.reducers.sendMessage(line)
                    }
                }
            }
        }
}
