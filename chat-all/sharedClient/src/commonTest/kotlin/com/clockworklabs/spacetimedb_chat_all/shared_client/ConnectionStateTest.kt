import com.clockworklabs.spacetimedb_kotlin_sdk.shared_client.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStateTest {

    @Test
    fun `ConnectionState enum has all expected values`() {
        val values = ConnectionState.entries
        assertEquals(4, values.size, "Should have 4 states")
        assertEquals(ConnectionState.DISCONNECTED, values[0])
        assertEquals(ConnectionState.CONNECTING, values[1])
        assertEquals(ConnectionState.CONNECTED, values[2])
        assertEquals(ConnectionState.CLOSED, values[3])
    }

    @Test
    fun `ConnectionState valueOf works`() {
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"))
        assertEquals(ConnectionState.CONNECTING, ConnectionState.valueOf("CONNECTING"))
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"))
        assertEquals(ConnectionState.CLOSED, ConnectionState.valueOf("CLOSED"))
    }
}
