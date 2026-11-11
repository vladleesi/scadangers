package dev.vladleesi.scadangers.core.security.device

internal fun networkFindings(observations: DeviceObservations): List<DeviceFinding> =
    with(observations) {
        listOf(
            finding(
                "device.network", DeviceCategory.Network, "Default network indicators",
                listOf(network.connected, network.captivePortal, network.vpn, network.proxy)
                    .joinToString("; ") { it.evidence } +
                    ". A proxy or VPN may be intentional; their presence or absence " +
                    "does not prove " +
                    "trust, interception or protection. A captive portal requires user review.",
                "https://developer.android.com/reference/android/net/NetworkCapabilities",
                status = when (network.captivePortal.state) {
                    SignalState.Enabled -> FindingStatus.ObservedRisk
                    SignalState.Disabled -> FindingStatus.Information
                    else -> FindingStatus.Unknown
                },
                severity = if (network.captivePortal.state ==
                    SignalState.Enabled
                ) {
                    DeviceSeverity.Low
                } else {
                    DeviceSeverity.Informational
                },
                confidence = network.captivePortal.state.confidence(),
                remediation =
                    "Review the selected network and any proxy or VPN. Avoid sensitive actions " +
                        "until a captive portal is resolved and you trust the network.",
                settings = DeviceSettings.Network
            ),
            finding(
                "device.dns", DeviceCategory.Network, "Private DNS on the default network",
                network.privateDns.evidence + ". This is Android's link indicator. " +
                    "Apps and VPNs can use different DNS; this does not prove all DNS " +
                    "is encrypted or exposed.",
                "https://developer.android.com/reference/android/net/LinkProperties#isPrivateDnsActive()",
                status = when (network.privateDns.state) {
                    SignalState.Enabled -> FindingStatus.ReportedProtection
                    SignalState.Disabled -> FindingStatus.ObservedRisk
                    else -> FindingStatus.Unknown
                },
                severity = if (network.privateDns.state ==
                    SignalState.Disabled
                ) {
                    DeviceSeverity.Low
                } else {
                    DeviceSeverity.Informational
                },
                confidence = network.privateDns.state.confidence(),
                remediation = "Review Private DNS in Network settings where supported. " +
                    "Use a trusted provider appropriate to your network; configuration " +
                    "is always your choice.",
                settings = DeviceSettings.Network
            )
        )
    }
