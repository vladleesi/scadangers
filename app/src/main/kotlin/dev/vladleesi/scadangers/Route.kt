package dev.vladleesi.scadangers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Small, non-sensitive routes; never place scan results or browsing data in saved state. */
@Serializable
internal sealed interface Route

@Serializable
@SerialName("overview")
internal data object Overview : Route

@Serializable
@SerialName("apps")
internal data object Apps : Route

@Serializable
@SerialName("network")
internal data object Network : Route

@Serializable
@SerialName("nearby")
internal data object Nearby : Route

@Serializable
@SerialName("alerts")
internal data object Alerts : Route

/** Device details, opened from the overview. */
@Serializable
@SerialName("device")
internal data object Device : Route

/** Opened from the top app bar's settings action. */
@Serializable
@SerialName("settings")
internal data object Settings : Route
