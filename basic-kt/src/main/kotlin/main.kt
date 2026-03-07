import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.DbConnection
import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.use
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import module_bindings.db
import module_bindings.reducers
import module_bindings.withModuleBindings
import kotlin.time.Duration.Companion.seconds

fun main(): Unit = runBlocking {
    DbConnection.Builder()
        .withUri("ws://localhost:3000")
        .withDatabaseName("basic-kt")
        .withModuleBindings()
        .onConnect { conn, identity, token ->
            println("Connected as $identity (token=${token.take(8)}...)")

            conn.db.person.onInsert { _, person ->
                println("New person: ${person.name}")
            }

            conn.reducers.onAdd { ctx, name ->
                println("[onAdd] Reducer '${ctx.reducerName}' added person: $name (status=${ctx.status})")
            }

            conn.subscribeToAllTables()

            conn.reducers.add("Alice") { ctx ->
                println("[one-shot] Add completed: status=${ctx.status}")
                conn.reducers.sayHello()
            }
        }
        .onDisconnect { _, error ->
            if (error != null) {
                println("Disconnected with error: $error")
            } else {
                println("Disconnected")
            }
        }
        .build()
        .use { conn ->
            delay(1.seconds)
        }
}