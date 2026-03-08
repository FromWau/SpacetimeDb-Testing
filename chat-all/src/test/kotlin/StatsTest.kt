import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import module_bindings.reducers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StatsTest {

    @Test
    fun `stats are zero before any operations`() = runBlocking {
        val client = connectToDb()

        assertEquals(0, client.conn.stats.reducerRequestTracker.getSampleCount(), "reducer samples should be 0 initially")
        assertEquals(0, client.conn.stats.oneOffRequestTracker.getSampleCount(), "oneOff samples should be 0 initially")
        assertEquals(0, client.conn.stats.reducerRequestTracker.getRequestsAwaitingResponse(), "no in-flight requests initially")
        assertNull(client.conn.stats.reducerRequestTracker.allTimeMin, "allTimeMin should be null initially")
        assertNull(client.conn.stats.reducerRequestTracker.allTimeMax, "allTimeMax should be null initially")

        client.conn.disconnect()
    }

    @Test
    fun `subscriptionRequestTracker increments after subscribe`() = runBlocking {
        val client = connectToDb()
        client.subscribeAll()

        val subSamples = client.conn.stats.subscriptionRequestTracker.getSampleCount()
        assertTrue(subSamples > 0, "subscriptionRequestTracker should have samples after subscribe, got $subSamples")

        client.conn.disconnect()
    }

    @Test
    fun `reducerRequestTracker increments after reducer call`() = runBlocking {
        val client = connectToDb()
        client.subscribeAll()

        val before = client.conn.stats.reducerRequestTracker.getSampleCount()

        val reducerDone = CompletableDeferred<Unit>()
        client.conn.reducers.onSendMessage { ctx, _ ->
            if (ctx.callerIdentity == client.identity) reducerDone.complete(Unit)
        }
        client.conn.reducers.sendMessage("stats-reducer-${System.nanoTime()}")
        withTimeout(DEFAULT_TIMEOUT_MS) { reducerDone.await() }

        val after = client.conn.stats.reducerRequestTracker.getSampleCount()
        assertTrue(after > before, "reducerRequestTracker should increment, before=$before after=$after")

        client.cleanup()
    }

    @Test
    fun `oneOffRequestTracker increments after suspend query`() = runBlocking {
        val client = connectToDb()
        client.subscribeAll()

        val before = client.conn.stats.oneOffRequestTracker.getSampleCount()

        // Use suspend variant — no flaky delay needed
        @Suppress("UNUSED_VARIABLE")
        val result = client.conn.oneOffQuery("SELECT * FROM user")

        val after = client.conn.stats.oneOffRequestTracker.getSampleCount()
        assertTrue(after > before, "oneOffRequestTracker should increment, before=$before after=$after")

        client.conn.disconnect()
    }

    @Test
    fun `allTimeMin and allTimeMax are set after reducer call`() = runBlocking {
        val client = connectToDb()
        client.subscribeAll()

        val reducerDone = CompletableDeferred<Unit>()
        client.conn.reducers.onSendMessage { ctx, _ ->
            if (ctx.callerIdentity == client.identity) reducerDone.complete(Unit)
        }
        client.conn.reducers.sendMessage("stats-minmax-${System.nanoTime()}")
        withTimeout(DEFAULT_TIMEOUT_MS) { reducerDone.await() }

        assertNotNull(client.conn.stats.reducerRequestTracker.allTimeMin, "allTimeMin should be set")
        assertNotNull(client.conn.stats.reducerRequestTracker.allTimeMax, "allTimeMax should be set")
        assertTrue(
            client.conn.stats.reducerRequestTracker.allTimeMin!!.duration >= kotlin.time.Duration.ZERO,
            "min duration should be non-negative"
        )

        client.cleanup()
    }

    @Test
    fun `getMinMaxTimes returns null when no window has rotated`() = runBlocking {
        val client = connectToDb()

        // On a fresh tracker, no window has rotated yet
        val minMax = client.conn.stats.reducerRequestTracker.getMinMaxTimes(60)
        assertNull(minMax, "getMinMaxTimes should return null before any window rotation")

        client.conn.disconnect()
    }
}
