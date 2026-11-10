package dev.scadangers.core.security.apps

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppInventorySerializationTest {
    @Test
    fun savedSnapshotPreservesUnknownGrantsSignersAndExplicitFailures() {
        val inventory = app().copy(
            permissions = listOf(
                AppPermission("example.unknown", null),
                AppPermission("example.denied", false)
            ),
            signing = SigningIdentity(setOf("a", "b"), multipleSigners = true),
            unavailable = setOf("installer")
        )
        val assessed = AppRiskEvaluator().evaluate(inventory, null, NOW)
        val snapshot =
            ScanSnapshot(
                NOW,
                24,
                false,
                listOf(assessed),
                mapOf("example.restricted" to "Unavailable")
            )
        assertEquals(snapshot, Json.decodeFromString<ScanSnapshot>(Json.encodeToString(snapshot)))
    }
}
