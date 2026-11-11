package dev.vladleesi.scadangers.scanner

import dev.vladleesi.scadangers.core.security.apps.AssessedApp
import dev.vladleesi.scadangers.core.security.apps.ExpectedInstallers
import dev.vladleesi.scadangers.core.security.apps.ReviewLevel
import dev.vladleesi.scadangers.core.security.apps.ScanSnapshot
import dev.vladleesi.scadangers.feature.apps.AppDetailLine
import dev.vladleesi.scadangers.feature.apps.AppDetailsUi
import dev.vladleesi.scadangers.feature.apps.AppFilters
import dev.vladleesi.scadangers.feature.apps.AppRiskFilter
import dev.vladleesi.scadangers.feature.apps.AppRowUi
import dev.vladleesi.scadangers.feature.apps.AppSort
import dev.vladleesi.scadangers.feature.apps.AppSourceFilter
import dev.vladleesi.scadangers.feature.apps.AppTypeFilter
import java.text.DateFormat
import java.util.Date

/** Filtering, sorting and formatting run on Default, never in a composable. */
internal fun ScanSnapshot.rows(filters: AppFilters): List<AppRowUi> {
    val comparator = when (filters.sort) {
        AppSort.Name -> compareBy<AssessedApp, String>(String.CASE_INSENSITIVE_ORDER) {
            it.inventory.label
        }

        AppSort.Risk -> compareByDescending<AssessedApp> { it.level }

        AppSort.Installed -> compareByDescending<AssessedApp> {
            it.inventory.stamp.firstInstallTime
        }

        AppSort.Updated -> compareByDescending<AssessedApp> { it.inventory.stamp.lastUpdateTime }
    }.thenBy { it.inventory.packageName }
    return apps.asSequence().filter { assessed ->
        val app = assessed.inventory
        val matchesName = app.packageName.contains(filters.query, true) ||
            app.label.contains(filters.query, true)
        val matchesRisk = filters.risk == AppRiskFilter.All || assessed.level >= ReviewLevel.Review
        matchesName && matchesRisk && assessed.matchesType(filters.type) &&
            assessed.matchesSource(filters.source)
    }.sortedWith(comparator).map { assessed ->
        val app = assessed.inventory
        val type = if (app.system || app.updatedSystem) "System" else "User"
        AppRowUi(
            app.packageName,
            app.label,
            "$type · Target API ${app.targetSdk} · " +
                "${assessed.findings.count { it.level >= ReviewLevel.Review }} review signals"
        )
    }.toList()
}

private fun AssessedApp.matchesType(filter: AppTypeFilter): Boolean = when (filter) {
    AppTypeFilter.All -> true
    AppTypeFilter.System -> inventory.system || inventory.updatedSystem
    AppTypeFilter.User -> !inventory.system && !inventory.updatedSystem
}

private fun AssessedApp.matchesSource(filter: AppSourceFilter): Boolean {
    val installer = inventory.installer
    val known = installer.observable && installer.installingPackage != null
    val expected = installer.installingPackage in ExpectedInstallers.packageNames
    return when (filter) {
        AppSourceFilter.All -> true
        AppSourceFilter.ExpectedStore -> known && expected
        AppSourceFilter.Other -> known && !expected
        AppSourceFilter.Unknown -> !known
    }
}

internal fun AssessedApp.details(): AppDetailsUi {
    val app = inventory
    val metadata = appMetadata(this)
    return AppDetailsUi(
        app.packageName,
        app.label,
        metadata,
        findings,
        app.permissions.sortedBy { it.name }.map {
            AppDetailLine(
                it.name,
                when (it.granted) {
                    true -> "Observed granted"
                    false -> "Not granted at observation"
                    null -> "Grant status unavailable"
                }
            )
        },
        app.apkPaths
    )
}

private fun appMetadata(assessed: AssessedApp): List<AppDetailLine> {
    val app = assessed.inventory
    val source = app.installer
    val grantedCount = app.permissions.count { it.granted == true }
    return listOf(
        AppDetailLine("Version", "${app.versionName ?: "Unavailable"} (${app.stamp.versionCode})"),
        AppDetailLine("Target SDK", "API ${app.targetSdk}"),
        AppDetailLine(
            "Classification",
            if (app.updatedSystem) {
                "Updated system app"
            } else if (app.system) {
                "System app"
            } else {
                "User app"
            }
        ),
        AppDetailLine("Installed", formatTimestamp(app.stamp.firstInstallTime)),
        AppDetailLine("Updated", formatTimestamp(app.stamp.lastUpdateTime)),
        AppDetailLine("Installing package", source.installingPackage ?: "Unavailable"),
        AppDetailLine("Initiating package", source.initiatingPackage ?: "Unavailable"),
        AppDetailLine(
            "Originating package",
            source.originatingPackage ?: "Unavailable (often restricted by Android)"
        ),
        AppDetailLine(
            "Package source",
            source.packageSource?.toString() ?: "Unavailable (requires API 33+)"
        ),
        AppDetailLine(
            "Current certificate SHA-256",
            app.signing.current.sorted().joinToString("\n").ifEmpty {
                "Unavailable"
            }
        ),
        AppDetailLine(
            "Signing history",
            if (app.signing.historyVerified) {
                app.signing.history.sorted().joinToString(
                    "\n"
                )
            } else {
                "Not observable with this signing API"
            }
        ),
        AppDetailLine(
            "Split names",
            app.splitNames.joinToString().ifEmpty {
                "No splits reported"
            }
        ),
        AppDetailLine(
            "Permission overview",
            "${app.permissions.size} declared; $grantedCount observed granted"
        )
    )
}

internal fun formatTimestamp(timestamp: Long): String = if (timestamp > 0) {
    DateFormat.getDateTimeInstance().format(Date(timestamp))
} else {
    "Unavailable"
}
