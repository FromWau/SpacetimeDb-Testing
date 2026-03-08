# SpacetimeDB Kotlin SDK — Test Coverage (chat-all)

**Total: 276 tests across 29 test classes — all passing**

## Tested

### DbConnection.Builder
- `withUri` — set WebSocket URI
- `withDatabaseName` — set database name
- `withToken` — set auth token (+ token reconnect preserves identity)
- `withCompression(GZIP)` — enable GZIP compression
- `withLightMode` — light mode toggle
- `withConfirmedReads` — confirmed reads toggle
- `onConnect` — builder-level connect callback
- `onDisconnect` — builder-level disconnect callback
- `onConnectError` — builder-level connect error callback (invalid URI, unreachable host, invalid DB name, garbage token 401)
- `withCallbackDispatcher` — custom CoroutineDispatcher for callbacks
- `build()` — suspend, builds and connects

### DbConnection
- `use {}` — extension function for auto-cleanup (normal, exception, cancellation)
- `identity` — current identity after connect
- `connectionId` — server-assigned connection ID
- `token` — auth token for persistence (reconnect returns same identity, new connections get new tokens)
- `isActive` — connection state boolean (true when connected, false after error/disconnect)
- `disconnect()` — suspend, clean shutdown (double-disconnect idempotent, onDisconnect fires)
- `stats` — Stats object with network metrics
- `onConnect / removeOnConnect` — add/remove connect callback
- `onDisconnect / removeOnDisconnect` — add/remove disconnect callback
- `onConnectError / removeOnConnectError` — add/remove connect error callback
- `oneOffQuery(sql, callback)` — callback variant (valid SQL → Ok, invalid SQL → Err, concurrent queries, populated/empty results)
- `oneOffQuery(sql): suspend` — suspend variant (valid/invalid SQL, hangs on disconnected conn)
- `subscribe(queries)` — filtered subscription
- `subscribeToAllTables()` — subscribe to all tables

### SubscriptionHandle (6 + existing tests)
- `state` — current SubscriptionState (PENDING → ACTIVE transitions)
- `isActive` — state check
- `isPending` — state check
- `isEnded` — false while active, true after unsubscribeThen completes
- `isUnsubscribing` — false while active
- `queries` — query list accessor (single + multiple queries)
- `querySetId` — assigned non-negative ID
- `unsubscribeThen` — unsubscribe with completion callback (→ ENDED)

### SubscriptionBuilder
- `subscriptionBuilder()` — create builder
- `onApplied(cb)` — applied callback on builder (multiple callbacks all fire)
- `onError(cb)` — error callback on builder (invalid SQL)
- `addQuery(sql)` — add query to builder (multi-query subscription)
- `subscribe()` — subscribe from builder (no queries throws)
- `subscribeToAllTables()` — subscribe all from builder

### TableCache Callbacks
- `onInsert` — row inserted (SubscribeApplied context vs live context)
- `onUpdate` — row updated (old, new)
- `onDelete` — row deleted
- `removeOnInsert` — unregister insert callback
- `removeOnDelete` — unregister delete callback
- `removeOnUpdate` — unregister update callback
- `onBeforeDelete` — before-delete callback (row still in cache during callback)
- `removeOnBeforeDelete` — unregister before-delete callback

### TableCache Query Methods
- `count()` — row count (zero before subscribe, increments after insert)
- `iter()` — iterate rows (consistent with count)
- `all()` — get all rows as list (consistent with count and iter)

### Client-Side Indexes
- `UniqueIndex.find(value)` — lookup by unique key (found + null for missing)

### Type-Safe Query API
- `Table.where { it.col.eq(value) }` — WHERE clause (live subscribe)
- `Table.filter { ... }` — alias for where
- `Col.eq / neq` — column equality/inequality comparisons (live subscribe)
- `Col.lt / lte / gt / gte` — column ordering comparisons (SQL gen + live subscribe)
- `Col.eq(otherCol)` — column-to-column comparison
- `NullableCol.eq / neq / lt / lte / gt / gte` — nullable column comparisons
- `NullableCol.eq(otherNullableCol)` — nullable col-to-col comparison
- `BoolExpr.and() / or() / not()` — boolean combinators
- `FromWhere.where()` — chained where produces AND (2x and 3x chain, live subscribe)
- `FromWhere.filter()` — alias chains like where

### SqlLit Factories
- `SqlLit.string` — string literal (quoting, escaping, empty)
- `SqlLit.bool` — TRUE/FALSE
- `SqlLit.byte / ubyte / short / ushort` — small integer types
- `SqlLit.int / uint / long / ulong` — integer types (min/max values)
- `SqlLit.float / double` — floating point
- `SqlLit.identity` — hex-formatted Identity literal (zero + fromHexString)
- `SqlLit.connectionId` — hex-formatted ConnectionId literal (zero + random)
- `SqlLit.uuid` — hex-formatted SpacetimeUuid literal (NIL + random)

### Join APIs
- `Table.leftSemijoin(right) { ... }` — left semi-join
- `Table.rightSemijoin(right) { ... }` — right semi-join
- `IxCol.eq(otherIxCol)` — indexed join condition
- `LeftSemiJoin.where / filter`
- `RightSemiJoin.where / filter`
- `FromWhere.leftSemijoin` — chained where then join

### Timestamp (24 tests)
- `Timestamp.UNIX_EPOCH` — epoch constant (microsSinceUnixEpoch == 0)
- `Timestamp.now()` — current time factory (non-zero, close to system time)
- `Timestamp.fromMillis(Long)` — factory from millis (roundtrip)
- `Timestamp.fromEpochMicroseconds(Long)` — factory from micros (roundtrip)
- `microsSinceUnixEpoch` / `millisSinceUnixEpoch` — epoch accessors
- `plus(TimeDuration)` / `minus(TimeDuration)` — arithmetic operators
- `minus(Timestamp)` — difference as TimeDuration
- `since(other: Timestamp)` — duration between timestamps
- `compareTo(other)` — ordering (earlier < later, equal)
- `toISOString()` — ISO 8601 formatting (Z suffix, 1970 epoch, microsecond precision)
- `toString()` — display string
- `equals()` / `hashCode()` — consistency

### TimeDuration (18 tests)
- `TimeDuration.fromMillis(Long)` — factory (roundtrip)
- Constructor from `Duration` and microseconds
- `micros` / `millis` — accessors
- `plus(TimeDuration)` / `minus(TimeDuration)` — arithmetic
- Negative duration support
- `compareTo(other)` — ordering
- `toString()` — formatted string (+seconds.micros, 6-digit precision)
- `equals()` / `hashCode()` — consistency

### Identity (16 tests)
- `Identity.zero()` — zero identity factory
- `Identity.fromHexString(hex)` — parse from hex (roundtrip, rejects invalid)
- `toHexString()` — 64 lowercase hex chars
- `toByteArray()` — 32-byte array
- `toString()` — display string
- `compareTo(other)` — ordering
- `equals()` / `hashCode()` — consistency
- Live identity from connection (hex roundtrip, byte array size)

### ConnectionId (21 tests)
- `ConnectionId.zero()` — zero factory
- `ConnectionId.random()` — random factory (non-zero, unique)
- `fromHexString(hex)` — parse from hex (roundtrip, rejects invalid)
- `fromHexStringOrNull(hex)` — parse or null (invalid→null, zero→null, valid→non-null)
- `nullIfZero(addr)` — null if zero
- `toHexString()` — 32 hex chars
- `toByteArray()` — 16-byte array
- `toString()` — display string
- `isZero()` — zero check
- `equals()` / `hashCode()` — consistency
- Live connectionId from connection

### ScheduleAt (11 tests)
- `ScheduleAt.interval(Duration)` — convenience factory (seconds, minutes)
- `ScheduleAt.time(Instant)` — convenience factory
- `ScheduleAt.Interval` / `ScheduleAt.Time` — direct constructors
- Equality (same interval == same interval, Interval != Time)

### SqlFormat (18 tests)
- `quoteIdent(ident)` — wraps in double quotes, escapes internal double quotes, empty string, spaces
- `formatStringLiteral(value)` — wraps in single quotes, escapes single quotes, empty, special chars
- `formatHexLiteral(hex)` — 0x prefix, strips existing prefix (case insensitive), strips hyphens, mixed case, rejects non-hex, rejects empty

### ColExtensions (15 tests)
- `Col<TRow, String>.eq/neq/lt/lte/gt/gte(value: String)` — match SqlLit equivalents
- `Col<TRow, Boolean>.eq/neq(value: Boolean)` — match SqlLit equivalents
- `NullableCol<TRow, String>.eq/gte(value: String)` — match SqlLit equivalents
- `Col<TRow, ULong>.eq/lt/gte(value: ULong)` — match SqlLit equivalents
- `IxCol<TRow, Identity>.eq(value: Identity)` — match SqlLit equivalent
- Convenience extensions produce valid SQL structure
- Note: IxCol<TRow, ULong> has NO convenience extension (SDK gap)

### EventContext
- `EventContext.Reducer.callerIdentity` — matches our identity
- `EventContext.Reducer.callerConnectionId` — non-null for our calls
- `EventContext.Reducer.status` — Committed for success, Failed for error
- `EventContext.Reducer.reducerName` — correct reducer name string
- `EventContext.Reducer.timestamp` — non-null timestamp
- `EventContext.Reducer.args` — correct argument values via callback
- `EventContext.SubscribeApplied` — delivered during initial subscription
- `EventContext.UnsubscribeApplied` — via unsubscribeThen
- Live inserts receive non-SubscribeApplied context

### Status
- `Status.Committed` — reducer succeeded
- `Status.Failed` — reducer failed (e.g. empty name), message non-empty

### Token Reconnect
- Reconnect with saved token → same identity
- Reconnect with saved token → same token
- Anonymous connections → different identities and tokens each time
- Token survives multiple reconnects

### Stats (4 + existing tests)
- `stats.reducerRequestTracker` — reducer latencies
- `stats.subscriptionRequestTracker` — subscription latencies
- `stats.oneOffRequestTracker` — one-off query latencies
- `stats.procedureRequestTracker` — starts empty (no procedures in module)
- `stats.applyMessageTracker` — exists, records after subscription
- All 5 trackers are distinct objects
- `NetworkRequestTracker.allTimeMin / allTimeMax` — all-time min/max
- `NetworkRequestTracker.getMinMaxTimes()` — min/max for time window (null before rotation)
- `NetworkRequestTracker.getSampleCount()` — total samples
- `NetworkRequestTracker.getRequestsAwaitingResponse()` — in-flight count (zero initially)

### UnsubscribeFlags (5 tests)
- `unsubscribeThen` transitions to ENDED
- `unsubscribeThen` callback receives non-null context
- `unsubscribe` completes without error
- Multiple subscriptions can be independently unsubscribed
- Unsubscribe then re-subscribe works (new querySetId assigned)

### Logger
- `Logger.level` — get/set log level
- `Logger.handler` — custom LogHandler
- `Logger.info / warn / debug / trace / error / exception` — log at each level
- Level filtering — messages below threshold suppressed
- Lazy message evaluation — lambda not evaluated when filtered
- Sensitive data redaction — token/password/secret patterns (multiple patterns)
- `Logger.exception(Throwable)` — stack trace logging

### ConnectionState Enum
- `DISCONNECTED / CONNECTING / CONNECTED / CLOSED` — all values present
- `valueOf()` — string-to-enum conversion

### SpacetimeUuid
- `NIL` / `MAX` — sentinel values (version, hex string)
- `random()` — V4 UUID generation (unique values)
- `fromRandomBytesV4()` — V4 from raw bytes (+ wrong size rejection)
- `fromCounterV7()` — V7 UUID from counter+timestamp (+ wrong size rejection)
- `Counter` — monotonic counter for V7, auto-increment
- `getCounter()` — extract embedded counter value
- `parse()` — parse from string (+ invalid string throws)
- `toString()` / `toHexString()` / `toByteArray()` — conversions
- `getVersion()` — detect Nil/V4/V7/Max/Unknown
- `compareTo()` — ordering (NIL < MAX, reflexive)
- `equals()` / `hashCode()` — consistency through parse roundtrip

---

## NOT Tested — Cannot Test in Current Setup

### DbConnection
- `callProcedure` / `ModuleProcedures` — no procedures defined in server module

### Client-Side Indexes
- `BTreeIndex.filter(value)` — no BTreeIndex in schema (all columns use UniqueIndex)

### Status
- `Status.OutOfEnergy` — cannot trigger in local dev environment

### ConnectionState
- Live state transitions — `_state` is private, only `isActive` is public

### EventContext
- `EventContext.Transaction` — requires a transaction not from a reducer
- `EventContext.UnknownTransaction` — defensive; requires cross-client reducer scenario
- `EventContext.Procedure` — no procedures defined in server module

---

## NOT Tested — Infrastructure / Internal (not user-facing)

### BSATN (bsatn/)
- `BsatnReader` — binary protocol reader (all read methods)
- `BsatnWriter` — binary protocol writer (all write methods)
- Used internally by SDK for serialization; users never call these directly

### ClientCache (ClientCache.kt)
- `TableCache.applyInserts/applyDeletes/applyUpdate` — internal mutation methods
- `TableCache.parseUpdate/parseDeletes` — internal parsing
- `TableCache.decodeRowList` — internal row decoding
- `TableCache.clear()` — internal cache reset
- `ClientCache.register/getTable/getTableOrNull/getOrCreateTable/getUntypedTable/tableNames/clear`
- `PendingCallback` — internal callback interface

### Transport (transport/)
- `Transport` interface — transport abstraction
- `SpacetimeTransport` — WebSocket transport implementation
- `SpacetimeTransport.WS_PROTOCOL` — protocol constant

### Protocol Messages (protocol/)
- `ClientMessage` subtypes — `Subscribe`, `Unsubscribe`, `OneOffQuery`, `CallReducer`, `CallProcedure`
- `ServerMessage` subtypes — `InitialConnection`, `SubscribeApplied`, `UnsubscribeApplied`, `SubscriptionError`, `TransactionUpdateMsg`, `ReducerResultMsg`, `ProcedureResultMsg`
- `ReducerOutcome` — `Ok`, `OkEmpty`, `Err`, `InternalError`
- `ProcedureStatus` — `Returned`, `InternalError`
- `TableUpdate`, `QuerySetUpdate`, `TransactionUpdate`, `TableUpdateRows`
- `RowSizeHint`, `BsatnRowList`, `SingleTableRows`, `QueryRows`
- `QuerySetId` — subscription query set identifier

### Compression (protocol/Compression.kt)
- `Compression.NONE/BROTLI/GZIP` — compression constants
- `decompressMessage()` — platform-specific decompression
- `defaultCompressionMode` / `availableCompressionModes` — platform defaults

### Module Descriptor System (DbConnection.kt)
- `ModuleDescriptor` — codegen interface
- `ModuleTables` / `ModuleReducers` / `ModuleProcedures` — marker interfaces
- `ModuleAccessors` — accessor container
- `Builder.withModule()` — register module descriptor

### Table Markers (RemoteTable.kt)
- `RemoteTable` — base marker interface
- `RemotePersistentTable` — persistent table marker
- `RemoteEventTable` — event table marker
