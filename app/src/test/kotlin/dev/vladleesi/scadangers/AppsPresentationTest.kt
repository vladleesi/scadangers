package dev.vladleesi.scadangers

import dev.vladleesi.scadangers.core.security.apps.AppInventory
import dev.vladleesi.scadangers.core.security.apps.AppRiskEvaluator
import dev.vladleesi.scadangers.core.security.apps.InstallSource
import dev.vladleesi.scadangers.core.security.apps.PackageStamp
import dev.vladleesi.scadangers.core.security.apps.ScanSnapshot
import dev.vladleesi.scadangers.core.security.apps.SigningIdentity
import dev.vladleesi.scadangers.feature.apps.AppFilters
import dev.vladleesi.scadangers.feature.apps.AppRiskFilter
import dev.vladleesi.scadangers.feature.apps.AppSort
import dev.vladleesi.scadangers.feature.apps.AppSourceFilter
import dev.vladleesi.scadangers.feature.apps.AppTypeFilter
import dev.vladleesi.scadangers.scanner.details
import dev.vladleesi.scadangers.scanner.rows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsPresentationTest {
    private val apps = listOf(
        inventory("example.user", "Zulu", 37, false, "com.android.vending", 1),
        inventory("example.system", "Alpha", 24, true, "example.store", 2),
        inventory("example.unknown", "Beta", 37, false, null, 3)
    ).map { AppRiskEvaluator().evaluate(it, null, 10) }
    private val snapshot = ScanSnapshot(10, 30, true, apps, emptyMap())

    @Test
    fun searchAndFiltersCombineWithoutImplyingSafety() {
        val filters = AppFilters(
            query = "EXAMPLE",
            type = AppTypeFilter.System,
            risk = AppRiskFilter.Review,
            source = AppSourceFilter.Other
        )
        assertEquals(listOf("example.system"), snapshot.rows(filters).map { it.packageName })
        assertEquals(
            listOf("example.unknown"),
            snapshot.rows(AppFilters(source = AppSourceFilter.Unknown)).map {
                it.packageName
            }
        )
        assertEquals(
            listOf("example.user"),
            snapshot.rows(AppFilters(source = AppSourceFilter.ExpectedStore)).map {
                it.packageName
            }
        )
        assertTrue(snapshot.rows(AppFilters(query = "nothing")).isEmpty())
    }

    @Test
    fun allSortOrdersAreDeterministic() {
        assertEquals(listOf("Alpha", "Beta", "Zulu"), snapshot.rows(AppFilters()).map { it.label })
        assertEquals(
            "example.system",
            snapshot.rows(AppFilters(sort = AppSort.Risk)).first().packageName
        )
        assertEquals(
            "example.unknown",
            snapshot.rows(AppFilters(sort = AppSort.Installed)).first().packageName
        )
        assertEquals(
            "example.unknown",
            snapshot.rows(AppFilters(sort = AppSort.Updated)).first().packageName
        )
    }

    @Test
    fun detailsExposeUnavailableSourceAndNoVulnerabilityClaim() {
        val details = apps.last().details()
        assertEquals(
            "Unavailable",
            details.metadata.first {
                it.label == "Installing package"
            }.description
        )
        assertTrue(details.findings.any { it.id == "analysis" })
    }

    @Suppress("LongParameterList") // Synthetic fixtures vary independent package attributes.
    private fun inventory(
        packageName: String,
        label: String,
        target: Int,
        system: Boolean,
        installer: String?,
        installed: Long
    ) = AppInventory(
        packageName, label, null, PackageStamp(1, installed, installed), target, system, false,
        InstallSource(
            installer
        ),
        SigningIdentity(), emptyList(), emptySet(), emptyList(), emptyList()
    )
}
