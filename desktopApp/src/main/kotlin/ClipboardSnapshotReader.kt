import com.cross.sync.clipboard.domain.entity.CopiedData

/** Avoids decoding unchanged images/RTF on every poll; unavailable revisions use full reads. */
internal class ClipboardSnapshotReader(private val revision: () -> Long?) {
    private var lastRevision: Long? = null
    private var snapshot: CopiedData? = null

    @Synchronized
    fun read(load: () -> CopiedData?): CopiedData? {
        val current = revision()
        if (current != null && current == lastRevision) return snapshot
        snapshot = null
        lastRevision = null
        val data = load()
        // A failed read or a clipboard change during decoding must be retried next time.
        if (data != null && current != null && current == revision()) {
            lastRevision = current
            snapshot = data
        }
        return data
    }
}
