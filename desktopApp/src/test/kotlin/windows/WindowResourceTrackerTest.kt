package windows

import kotlin.test.Test
import kotlin.test.assertEquals

class WindowResourceTrackerTest {
    @Test
    fun closingLastWindowTrimsCachesOnceAndNextSessionCanOpen() {
        var trims = 0
        val resources = WindowResourceTracker { trims++ }
        repeat(50) { session ->
            val main = resources.open()
            val quick = resources.open()
            val popup = resources.open()
            main.close()
            popup.close()
            assertEquals(session, trims)
            quick.close()
            assertEquals(session + 1, trims)
            quick.close()
            assertEquals(session + 1, trims)
        }
    }
}
