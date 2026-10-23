package windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import org.jetbrains.skia.Graphics

internal class WindowResourceTracker(private val onIdle: () -> Unit) {
    private var windows = 0

    fun open(): AutoCloseable {
        windows++
        var closed = false
        return AutoCloseable {
            if (!closed) {
                closed = true
                if (--windows == 0) onIdle()
            }
        }
    }
}

internal object DesktopUiResources {
    private var imageLoader: ImageLoader? = null
    private val windows = WindowResourceTracker {
        imageLoader?.memoryCache?.clear()
        Graphics.purgeAllCaches()
    }

    fun configureImageLoader() {
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context)
                .memoryCache { MemoryCache.Builder().maxSizeBytes(32L * 1024 * 1024).build() }
                .build()
                .also { imageLoader = it }
        }
    }

    @Composable
    fun TrackWindow() {
        DisposableEffect(Unit) {
            val lease = windows.open()
            onDispose { lease.close() }
        }
    }
}
