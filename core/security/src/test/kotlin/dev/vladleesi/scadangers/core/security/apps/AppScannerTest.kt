package dev.vladleesi.scadangers.core.security.apps

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppScannerTest {
    @Test
    fun emptyRestrictedInventoryIsRecordedWithScope() = runTest {
        val source = FakeSource()
        val store = MemoryStore()
        val result = AppScanner(source, store) { NOW }.scan(false)
        assertTrue(result.visibilityLimited)
        assertTrue(result.apps.isEmpty())
        assertEquals(result, store.saved)
    }

    @Test
    fun unchangedAppsReuseMetadataButGrantsAreRefreshed() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val scanner = AppScanner(source, MemoryStore()) { NOW }
        scanner.scan(false)
        source.apps["example"] =
            app("example").copy(permissions = listOf(AppPermission("microphone", true)))
        val refreshed = scanner.scan(false)
        assertNotNull(source.cached.last())
        assertEquals(true, refreshed.apps.single().inventory.permissions.single().granted)
        scanner.scan(true)
        assertNull(source.cached.last())
    }

    @Test
    fun updateDuringReadRetriesAndCommitsOnlyNewVersion() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        source.beforeRead = {
            source.beforeRead = null
            source.apps["example"] =
                app("example").copy(stamp = STAMP.copy(versionCode = 2, lastUpdateTime = NOW))
        }
        val store = MemoryStore()
        val result = AppScanner(source, store) { NOW }.scan(false)
        assertEquals(2L, result.apps.single().inventory.stamp.versionCode)
        assertEquals(1, store.writes)
    }

    @Test
    fun continuousChangesNeverOverwritePriorResult() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val store = MemoryStore()
        val scanner = AppScanner(source, store) { NOW }
        val original = scanner.scan(false)
        source.beforeRead = { source.generation++ }
        try {
            scanner.scan(false)
            throw AssertionError("Expected unstable inventory failure")
        } catch (_: InventoryChangedException) {
            assertEquals(original, store.saved)
            assertEquals(1, store.writes)
        }
    }

    @Test
    fun inaccessibleAppsAreExplicitAndNotRetainedAsCurrentCleanResults() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val scanner = AppScanner(source, MemoryStore()) { NOW }
        scanner.scan(false)
        source.inaccessible = true
        val result = scanner.scan(false)
        assertTrue(result.apps.isEmpty())
        assertTrue(result.inaccessible.containsKey("example"))
    }

    @Test
    fun removalIsReconciledWithoutScanningOldPackage() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val scanner = AppScanner(source, MemoryStore()) { NOW }
        scanner.scan(false)
        source.apps.clear()
        assertTrue(scanner.scan(false).apps.isEmpty())
        assertEquals(1, source.cached.size)
    }

    @Test
    fun cancellationRetainsLastSuccessfulSnapshot() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val store = MemoryStore()
        val scanner = AppScanner(source, store) { NOW }
        val original = scanner.scan(false)
        val entered = CompletableDeferred<Unit>()
        source.beforeRead = {
            entered.complete(Unit)
            CompletableDeferred<Unit>().await()
        }
        val job = launch { scanner.scan(true) }
        entered.await()
        job.cancelAndJoin()
        assertEquals(original, store.saved)
        assertEquals(1, store.writes)
    }

    @Test
    fun failedEnumerationIsNotAnEmptySuccessfulScan() = runTest {
        val source = FakeSource()
        source.failListing = true
        val store = MemoryStore()
        try {
            AppScanner(source, store).scan(true)
            throw AssertionError("Expected listing failure")
        } catch (_: SecurityException) {
            assertNull(store.saved)
        }
    }

    @Test
    fun packageEventDuringFinalEnumerationForcesReconciliation() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        var listings = 0
        source.beforeListing = {
            listings++
            if (listings == 2) source.generation++
        }
        val store = MemoryStore()
        AppScanner(source, store) { NOW }.scan(false)
        assertEquals(2, source.cached.size)
        assertEquals(1, store.writes)
    }

    @Test
    fun scanDeadlineRetainsPreviousCompletedSnapshot() = runTest {
        val source = FakeSource(mutableMapOf("example" to app("example")))
        val store = MemoryStore()
        val scanner = AppScanner(source, store) { NOW }
        val original = scanner.scan(false)
        source.beforeRead = { CompletableDeferred<Unit>().await() }
        try {
            scanner.scan(true)
            throw AssertionError("Expected scan deadline")
        } catch (_: TimeoutCancellationException) {
            assertEquals(original, store.saved)
            assertEquals(1, store.writes)
        }
    }

    @Test
    fun largeInventoriesUseBoundedProgressBatchesAndSerialReads() = runTest {
        val source =
            FakeSource((1..2000).associate { "example.$it" to app("example.$it") }.toMutableMap())
        val progress = mutableListOf<Pair<Int, Int>>()
        val result = AppScanner(source, MemoryStore()) {
            NOW
        }.scan(false) { completed, total -> progress.add(completed to total) }
        assertEquals(2000, result.apps.size)
        assertEquals(2000 to 2000, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b.first - a.first in 1..24 })
        assertEquals(2000, source.cached.size)
        assertFalse(result.apps.any { it.findings.isEmpty() })
    }
}

internal const val NOW = 1_000_000_000L
internal val STAMP = PackageStamp(1, NOW - 1000, NOW - 1000)
internal fun app(packageName: String = "example") = AppInventory(
    packageName, "Example", "1.0", STAMP, 37, false, false,
    InstallSource(installingPackage = "com.android.vending"),
    SigningIdentity(
        setOf("fingerprint")
    ),
    emptyList(), emptySet(), listOf("/synthetic/base.apk"), emptyList()
)

private class MemoryStore : AppScanStore {
    var saved: ScanSnapshot? = null
    var writes = 0
    override suspend fun load() = saved
    override suspend fun replace(snapshot: ScanSnapshot) {
        saved = snapshot
        writes++
    }
}

private class FakeSource(val apps: MutableMap<String, AppInventory> = mutableMapOf()) :
    AppInventorySource {
    override val apiLevel = 30
    override var generation = 0L
    val cached = mutableListOf<AppInventory?>()
    var inaccessible = false
    var failListing = false
    var beforeRead: (suspend () -> Unit)? = null
    var beforeListing: (() -> Unit)? = null
    override suspend fun packages(): Map<String, PackageStamp> {
        beforeListing?.invoke()
        if (failListing) throw SecurityException("Synthetic restriction")
        return apps.mapValues { it.value.stamp }
    }
    override suspend fun read(packageName: String, cached: AppInventory?): AppInventory {
        this.cached.add(cached)
        beforeRead?.invoke()
        if (inaccessible) throw PackageUnavailableException("Synthetic denial")
        return apps[packageName] ?: throw IOException("Synthetic missing app")
    }
}
