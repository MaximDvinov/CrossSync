import com.cross.sync.clipboard.domain.entity.CopiedData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ClipboardSnapshotReaderTest {
    private val image = CopiedData.Image(1, "/fixture/image.png", Clock.System.now(), null)

    @Test
    fun unchangedClipboardDecodesImageOnceAndChangedClipboardReadsAgain() {
        var revision = 10L
        var reads = 0
        val reader = ClipboardSnapshotReader { revision }
        repeat(100) { assertEquals(image, reader.read { reads++; image }) }
        assertEquals(1, reads)
        revision++
        assertEquals(image, reader.read { reads++; image })
        assertEquals(2, reads)
    }

    @Test
    fun clipboardChangeDuringReadDoesNotCacheAnOutdatedSnapshot() {
        var revision = 10L
        var reads = 0
        val reader = ClipboardSnapshotReader { revision }
        reader.read { reads++; revision++; image }
        reader.read { reads++; image }
        assertEquals(2, reads)
    }

    @Test
    fun unavailableRevisionAndFailedReadsAreRetried() {
        var reads = 0
        val fallback = ClipboardSnapshotReader { null }
        repeat(2) { fallback.read { reads++; image } }
        assertEquals(2, reads)
        val reader = ClipboardSnapshotReader { 10L }
        assertNull(reader.read { null })
        assertEquals(image, reader.read { image })
    }
}
