package notifications

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MacOsNotificationBridgeSelectionTest {
    @Test
    fun developmentLoadsRebuiltBridgeInsteadOfStaleResourceCopy() = inTempDirectory { directory ->
        val stale = bridge(directory, "resources.dylib", 1_000L)
        val rebuilt = bridge(directory, "generated.dylib", 2_000L)

        assertEquals(rebuilt, selectMacOsNotificationBridge(null, listOf(stale, rebuilt)))
        assertEquals(rebuilt, selectMacOsNotificationBridge(stale, listOf(stale, rebuilt)))
    }

    @Test
    fun installedAppUsesItsOwnResources() = inTempDirectory { directory ->
        val installed = bridge(directory, "installed.dylib", 1_000L)
        val development = bridge(directory, "development.dylib", 2_000L)

        assertEquals(installed, selectMacOsNotificationBridge(installed, listOf(development)))
    }

    @Test
    fun missingCandidatesAreIgnored() = inTempDirectory { directory ->
        val missing = File(directory, "missing.dylib")
        val rebuilt = bridge(directory, "generated.dylib", 2_000L)

        assertEquals(rebuilt, selectMacOsNotificationBridge(missing, listOf(missing, rebuilt)))
        assertNull(selectMacOsNotificationBridge(missing, listOf(missing)))
    }

    private fun bridge(directory: File, name: String, modifiedAt: Long): File =
        File(directory, name).apply {
            writeText(name)
            assertTrue(setLastModified(modifiedAt))
        }

    private fun inTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("crosssync-bridge-selection-").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
