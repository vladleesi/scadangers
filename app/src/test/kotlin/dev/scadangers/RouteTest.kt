package dev.scadangers

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class RouteTest {
    @Test
    fun allRoutesSurviveSerialization() {
        val routes = listOf(Overview, Apps, Network, Nearby, Alerts, Device, Settings)
        routes.forEach { route ->
            assertEquals(route, Json.decodeFromString<Route>(Json.encodeToString<Route>(route)))
        }
    }

    @Test
    fun navigationHasFivePrimaryDestinationsWithoutSettings() {
        val primary = TopLevelDestination.entries.map { it.route }

        assertEquals(listOf(Overview, Apps, Network, Nearby, Alerts), primary)
    }
}
