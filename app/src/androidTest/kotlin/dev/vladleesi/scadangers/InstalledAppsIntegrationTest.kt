package dev.vladleesi.scadangers

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import dev.vladleesi.scadangers.core.database.AppScanDatabase
import dev.vladleesi.scadangers.core.database.RoomAppScanStore
import dev.vladleesi.scadangers.core.security.apps.AppRiskEvaluator
import dev.vladleesi.scadangers.core.security.apps.ScanSnapshot
import dev.vladleesi.scadangers.scanner.AndroidAppInventorySource
import dev.vladleesi.scadangers.scanner.PackageEvents
import dev.vladleesi.scadangers.scanner.permissions
import dev.vladleesi.scadangers.scanner.signingIdentity
import java.security.MessageDigest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Run on API 24, 28, 30, 33 and 37 to exercise actual framework differences. */
class InstalledAppsIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun visibleInventoryReadsSelfWithoutBroadPermissionOrApkParsing() = runBlocking {
        val source = AndroidAppInventorySource(context, PackageEvents(context))
        val packages = source.packages()
        assertTrue(packages.containsKey(context.packageName))
        val app = source.read(context.packageName, null)
        assertEquals(context.packageName, app.packageName)
        assertEquals(37, app.targetSdk)
        assertTrue(app.signing.current.isNotEmpty())
        assertTrue(app.apkPaths.isNotEmpty())
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            context.checkSelfPermission("android.permission.QUERY_ALL_PACKAGES")
        )
        val refreshed = source.read(context.packageName, app)
        assertEquals(app.signing, refreshed.signing)
        assertEquals(app.permissions, refreshed.permissions)
    }

    @Test
    fun certificateFingerprintsMatchDocumentedApiForDeviceLevel() {
        val pm = context.packageManager

        @Suppress("DEPRECATION")
        val flags = if (Build.VERSION.SDK_INT >=
            28
        ) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }

        @Suppress("DEPRECATION")
        val info = pm.getPackageInfo(context.packageName, flags)
        val identity = info.signingIdentity()
        assertFalse(identity.multipleSigners)
        assertEquals(Build.VERSION.SDK_INT >= 28, identity.historyVerified)
        @Suppress("DEPRECATION")
        val cert = if (Build.VERSION.SDK_INT >= 28) {
            assertNotNull(info.signingInfo)
            info.signingInfo!!.signingCertificateHistory.last()
        } else {
            info.signatures!!.single()
        }
        val expected = MessageDigest.getInstance(
            "SHA-256"
        ).digest(cert.toByteArray()).joinToString("") {
            "%02X".format(it)
        }
        assertEquals(setOf(expected), identity.current)
    }

    @Test
    fun unavailableGrantFlagsAreNotConvertedToDeniedOrGranted() {
        val info = PackageInfo().apply {
            requestedPermissions = arrayOf("example.permission.ONE", "example.permission.TWO")
            requestedPermissionsFlags = intArrayOf(PackageInfo.REQUESTED_PERMISSION_GRANTED)
        }
        assertEquals(true, info.permissions()[0].granted)
        assertEquals(null, info.permissions()[1].granted)
        info.requestedPermissions = null
        assertTrue(info.permissions().isEmpty())
    }

    @Test
    fun roomRoundTripAndCancelledTransactionRetainCompletedResults() = runBlocking {
        withContext(Dispatchers.IO) {
            val db = Room.inMemoryDatabaseBuilder(context, AppScanDatabase::class.java).build()
            try {
                val app = AndroidAppInventorySource(
                    context,
                    PackageEvents(context)
                ).read(context.packageName, null)
                val snapshot = ScanSnapshot(
                    123,
                    Build.VERSION.SDK_INT,
                    Build.VERSION.SDK_INT >= 30,
                    listOf(AppRiskEvaluator().evaluate(app, null, 123)),
                    mapOf("synthetic.inaccessible" to "Restricted")
                )
                val store = RoomAppScanStore { db }
                store.replace(snapshot)
                assertEquals(snapshot, store.load())
                val entered = CompletableDeferred<Unit>()
                val transaction = launch {
                    db.withTransaction {
                        db.scans().clearResults()
                        entered.complete(Unit)
                        CompletableDeferred<Unit>().await()
                    }
                }
                entered.await()
                transaction.cancelAndJoin()
                assertEquals(snapshot, store.load())
            } finally {
                db.close()
            }
        }
    }
}
