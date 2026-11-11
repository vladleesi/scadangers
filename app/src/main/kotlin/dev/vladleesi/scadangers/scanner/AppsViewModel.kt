package dev.vladleesi.scadangers.scanner

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vladleesi.scadangers.core.security.apps.AppScanner
import dev.vladleesi.scadangers.core.security.apps.ScanSnapshot
import dev.vladleesi.scadangers.feature.apps.AppDetailLine
import dev.vladleesi.scadangers.feature.apps.AppFilters
import dev.vladleesi.scadangers.feature.apps.AppsUiState
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException

/** Activity-owned state; storage/engine instantiate only after the Apps destination is opened. */
@HiltViewModel
internal class AppsViewModel @Inject constructor(
    private val scanner: AppScanner,
    private val events: PackageEvents,
    private val savedState: SavedStateHandle
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        AppsUiState(
            filters = AppFilters(
                query =
                    savedState[QUERY] ?: ""
            )
        )
    )
    val state = mutableState.asStateFlow()
    private var snapshot: ScanSnapshot? = null
    private var scanJob: Job? = null
    private var observer: Job? = null
    private var presentation: Job? = null
    private var pendingRefresh = false

    init {
        scanJob = viewModelScope.launch {
            handleFailure {
                snapshot = withContext(Dispatchers.IO) { scanner.lastScan() }
                rebuild()
            }
            mutableState.update { it.copy(loaded = true) }
        }
    }

    fun resume() {
        if (observer != null) return
        observer = viewModelScope.launch {
            scanJob?.join()
            if (snapshot != null) scan(false)
            handleFailure {
                events.changes().collect {
                    if (snapshot != null) {
                        if (scanJob?.isActive == true) pendingRefresh = true else scan(false)
                    }
                }
            }
        }
    }

    fun pause() {
        observer?.cancel()
        observer = null
    }

    fun scan(full: Boolean) {
        if (!state.value.loaded || scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            mutableState.update {
                it.copy(busy = true, error = null, progress = "Reading visible packages…")
            }
            try {
                handleFailure {
                    snapshot = withContext(Dispatchers.Default) {
                        scanner.scan(full) { completed, total ->
                            mutableState.update {
                                it.copy(progress = "$completed / $total visible apps")
                            }
                        }
                    }
                    rebuild()
                }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
            if (pendingRefresh && observer != null) {
                pendingRefresh = false
                // Queue after this job completes; only one scan owns the engine at a time.
                scanJob = null
                scan(false)
            }
        }
    }

    fun cancel() {
        pendingRefresh = false
        scanJob?.cancel()
        mutableState.update {
            it.copy(
                busy = false,
                error = "Scan cancelled. The previous completed scan is retained."
            )
        }
    }

    fun filters(filters: AppFilters) {
        savedState[QUERY] = filters.query
        mutableState.update { it.copy(filters = filters) }
        rebuild()
    }

    fun open(packageName: String?) {
        savedState[SELECTED] = packageName
        rebuild()
    }

    fun settingsUnavailable() {
        mutableState.update {
            it.copy(
                error = "This Android settings screen is unavailable or restricted on this device."
            )
        }
    }

    private fun rebuild() {
        presentation?.cancel()
        val current = snapshot ?: return
        val filters = state.value.filters
        val selected: String? = savedState[SELECTED]
        presentation = viewModelScope.launch {
            val prepared = withContext(Dispatchers.Default) {
                Triple(
                    current.rows(filters),
                    current.apps.firstOrNull {
                        it.inventory.packageName ==
                            selected
                    }?.details(),
                    current.inaccessible.map { AppDetailLine(it.key, it.value) }
                )
            }
            val scope = if (current.visibilityLimited) {
                "Package visibility is filtered."
            } else {
                "Current profile only."
            }
            mutableState.update {
                it.copy(
                    apps = prepared.first,
                    details = prepared.second,
                    inaccessible = prepared.third,
                    lastScan = "Last completed scan: ${formatTimestamp(current.completedAt)}",
                    summary =
                        "${current.apps.size} visible apps assessed; " +
                            "${current.inaccessible.size} inaccessible. $scope"
                )
            }
        }
    }

    private suspend fun handleFailure(block: suspend () -> Unit) {
        val failure = try {
            block()
            null
        } catch (_: TimeoutCancellationException) {
            "Scan deadline exceeded. Retry; the previous completed scan is retained."
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            "Inventory changed or could not be read. Retry; the previous completed scan is retained."
        } catch (_: SQLiteException) {
            "Scan storage is unavailable. The previous completed scan has not been replaced."
        } catch (_: SerializationException) {
            "Saved scan could not be decoded. Results are unavailable; no clean assessment is implied."
        } catch (_: SecurityException) {
            "Android restricted inventory access. No clean assessment is available."
        } catch (_: IllegalStateException) {
            "Scan storage or platform is unavailable. No clean assessment is available."
        }
        if (failure != null) mutableState.update { it.copy(error = failure) }
    }

    private companion object {
        const val QUERY = "apps_query"
        const val SELECTED = "apps_selected"
    }
}
