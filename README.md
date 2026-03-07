# SpacetimeDB Kotlin SDK — Testing Projects

Test projects for the [SpacetimeDB Kotlin SDK](https://github.com/clockworklabs/SpacetimeDB/tree/master/sdks/kotlin), using local builds of both the CLI and SDK via `includeBuild`.

## Projects

### basic-kt

Minimal hello-world project. Connects to SpacetimeDB, inserts a `Person`, and calls `say_hello`.

- **Server module:** single `Person` table, `add` and `say_hello` reducers
- **Client:** connects, subscribes, calls reducers, prints callbacks, disconnects

### chat-kt

Console chat application — 1:1 Kotlin port of the official C#/Rust chat quickstart templates.

- **Server module:** `User` table (identity, name, online), `Message` table (sender, sent, text), `set_name`/`send_message` reducers, `client_connected`/`client_disconnected` lifecycle reducers
- **Client:** interactive stdin chat, `/name <name>` to set display name, token persistence across sessions, multi-client support via `--client N` flag
- **Features:** online/offline notifications, name change notifications, message history on connect, reducer error reporting

### chat-compose

Compose Desktop GUI version of the chat client — same server module as chat-kt.

- **Server module:** same as chat-kt (shared `chat-console-rs` template)
- **Client:** Compose Desktop with Material3 dark theme, message list with timestamps, online/offline user sidebar, `/name <name>` support
- **Features:** auto-scrolling chat, per-client token persistence, two run configs (Client 1 / Client 2) for multi-client testing

## Prerequisites

- Local SpacetimeDB repo at `~/Projects/SpacetimeDB/` with CLI and SDK built
- Java 21+
- SpacetimeDB server running locally (`spacetime start`)

## Running

```bash
# Start the server
spacetime start

# Publish the module (from the project directory)
cd <project> && spacetime publish --server local

# Run the client
./gradlew run --console=plain --no-configuration-cache

# For chat-kt with multiple clients:
./gradlew run --console=plain --no-configuration-cache --args="--client 1"
./gradlew run --console=plain --no-configuration-cache --args="--client 2"

# For chat-compose with multiple clients:
./gradlew run -PclientId=1 --no-configuration-cache
./gradlew run -PclientId=2 --no-configuration-cache
```

## Build Setup

All projects use a Gradle plugin (`com.clockworklabs.spacetimedb`) that auto-generates Kotlin bindings from the server module during build. The SDK is resolved via `includeBuild` in `settings.gradle.kts` — no publishing needed.
