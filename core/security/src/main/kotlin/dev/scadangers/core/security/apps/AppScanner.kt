package dev.scadangers.core.security.apps

import dev.scadangers.core.security.capability.AndroidApi
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield

/** Android boundary. Reads must execute off Main. A null cache forces complete metadata refresh. */
interface AppInventorySource {
    val apiLevel: Int
    val generation: Long
    suspend fun packages(): Map<String, PackageStamp>
    suspend fun read(packageName: String, cached: AppInventory?): AppInventory
}

/** A replacement is atomic: cancellation/failure must leave the previous snapshot intact. */
interface AppScanStore {
    suspend fun load(): ScanSnapshot?
    suspend fun replace(snapshot: ScanSnapshot)
}

class InventoryChangedException :
    IOException("Installed packages changed repeatedly; retry the scan.")
class PackageUnavailableException(message: String) : IOException(message)

/** Serialized, cancellable inventory with bounded batches and a finite scan deadline. */
class AppScanner(
    private val source: AppInventorySource,
    private val store: AppScanStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val mutex = Mutex()
    private val evaluator = AppRiskEvaluator()

    suspend fun lastScan(): ScanSnapshot? = mutex.withLock { store.load() }

    suspend fun scan(
        full: Boolean,
        onProgress: (Int, Int) -> Unit = { _, _ ->
        }
    ): ScanSnapshot = mutex.withLock {
        withTimeout(SCAN_TIMEOUT_MILLIS) {
            val previous = store.load()?.apps.orEmpty().associateBy { it.inventory.packageName }
            repeat(MAX_ATTEMPTS) {
                currentCoroutineContext().ensureActive()
                val generation = source.generation
                val stamps = source.packages()
                val snapshot = try {
                    collect(stamps, previous, full, onProgress)
                } catch (_: InventoryChangedException) {
                    null
                }
                // PackageManager offers no transactional snapshot. Reject observed concurrent changes.
                val finalStamps = source.packages()
                if (snapshot != null && stamps == finalStamps && generation == source.generation) {
                    currentCoroutineContext().ensureActive()
                    val completed = snapshot.copy(completedAt = clock())
                    store.replace(completed)
                    return@withTimeout completed
                }
            }
            throw InventoryChangedException()
        }
    }

    private suspend fun collect(
        stamps: Map<String, PackageStamp>,
        previous: Map<String, AssessedApp>,
        full: Boolean,
        onProgress: (Int, Int) -> Unit
    ): ScanSnapshot {
        val apps = ArrayList<AssessedApp>(stamps.size)
        val inaccessible = linkedMapOf<String, String>()
        val now = clock()
        var completed = 0
        onProgress(0, stamps.size)
        for (batch in stamps.entries.chunked(BATCH_SIZE)) {
            for ((packageName, stamp) in batch) {
                currentCoroutineContext().ensureActive()
                val result = assess(packageName, stamp, previous[packageName], full, now)
                if (result == null) {
                    inaccessible[packageName] =
                        "Package disappeared or access was restricted; no assessment available."
                } else {
                    apps.add(result)
                }
                completed++
            }
            onProgress(completed, stamps.size)
            yield()
        }
        return ScanSnapshot(
            now,
            source.apiLevel,
            source.apiLevel >= AndroidApi.ANDROID_11,
            apps.toList(),
            inaccessible.toMap()
        )
    }

    private suspend fun assess(
        packageName: String,
        stamp: PackageStamp,
        previous: AssessedApp?,
        full: Boolean,
        now: Long
    ): AssessedApp? {
        val cached = previous?.inventory?.takeIf { !full && it.stamp == stamp }
        return try {
            val inventory = source.read(packageName, cached)
            if (inventory.stamp != stamp) throw InventoryChangedException()
            evaluator.evaluate(inventory, previous, now)
        } catch (_: PackageUnavailableException) {
            null // The caller records an explicit inaccessible result, never a clean fallback.
        }
    }

    private companion object {
        const val BATCH_SIZE = 24
        const val MAX_ATTEMPTS = 2
        const val SCAN_TIMEOUT_MILLIS = 90_000L
    }
}
