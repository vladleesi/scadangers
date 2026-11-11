# Android API compatibility

Contract: minSdk 24, compileSdk 37, targetSdk 37. No library may increase the
application minimum through manifest overrides. The installed API 37 SDK reports
final API 37.0 (preview SDK 0). Debug/release builds and manifest merging are validated locally; device checks
currently cover API 31 only. API policy tests cover every level from 24 through 37.

| API | Platform consideration | Foundation behavior / future integration requirement |
| --- | --- | --- |
| 24 | Android 7: Network Security Config, Doze, low-memory devices | Explicit system CA trust and cleartext denial; no background jobs. Avoid newer Java/Android APIs without guards or supported desugaring. Test low-RAM startup and recreation. |
| 26 | Android 8: background execution limits, notification channels, `ValueAnimator.areAnimatorsEnabled` | No services or notification delivery. App notification enablement can be reviewed in Settings. Reduced motion uses `areAnimatorsEnabled` on 26+ and reads `ANIMATOR_DURATION_SCALE` on 24–25. |
| 28 | Android 9: cleartext defaults and hidden API restrictions | Explicit cleartext denial applies across all supported versions. No hidden APIs or sandbox bypass. |
| 29 | Android 10: scoped storage and background location limits | No broad storage or background location permission; foreground location is optional for radio access. Future APK import should use Storage Access Framework and bounded streams. |
| 30 | Android 11: package visibility and stronger scoped storage | No QUERY_ALL_PACKAGES. A narrow MAIN/LAUNCHER query adds launchable packages to automatically visible packages; users cannot remove filtering with runtime consent. |
| 31 | Android 12: exported components, foreground service launch limits, Bluetooth permissions, data extraction rules | Only the app launcher is exported by app source; the library ProfileInstaller receiver is protected by DUMP. No app pending intents or services. BLE access uses BLUETOOTH_SCAN runtime consent and neverForLocation. Explicit cloud and device-transfer exclusions. |
| 33 | Android 13: notification runtime consent and nearby Wi-Fi permission | POST_NOTIFICATIONS and NEARBY_WIFI_DEVICES are requested only after contextual explanation and confirmation. Wi-Fi scan APIs retain precise-location requirements. |
| 34 | Android 14: foreground service types/permissions and receiver export requirements | No foreground services. Installed-app package events use a lifecycle-owned dynamic receiver with RECEIVER_NOT_EXPORTED. Review integration-specific requirements before adding either. |
| 35 | Android 15: enforced edge-to-edge and 16 KB page-size ecosystem | Activity enables edge-to-edge and Scaffold consumes insets. No app native code; verify any future native SDK and packaged native transitive libraries. |
| 36 | Android 16: predictive back, large-screen adaptation, scheduling changes | Navigation uses platform integration; no orientation locks, custom back interception or scheduling. Layout adapts to window width (bar under 600 dp, rail otherwise). Test resizing, foldable postures and gesture back on device. |
| 37 | Android 17: target-specific behavior changes including stricter local-network access rules | Target remains 37. Broad LAN capability requests ACCESS_LOCAL_NETWORK after explanation. No sockets/discovery implementation exists; a grant is not operational monitoring. |

Launcher icons use distinct standard/round density-qualified PNGs on API 24–25,
adaptive foreground/background vectors with mask-safe primary artwork from API 26,
and the supplied monochrome layer from API 33. Both manifest icon attributes refer
to mipmap resources. A notification small-icon vector is included without implementing
notification delivery. Themed rendering depends
on launcher support and the user's setting. See [brand resources](design-system.md#brand-icon-resources).

API 25, 27 and 32 are covered by the same minimum-SDK contract, with changes
inherited from earlier releases. New framework calls require SDK guards. Raising
minSdk or lowering targetSdk is never an accepted compatibility workaround.

CI defines instrumentation coverage at API 24 and 37; those images are not installed
locally. Intervening permission boundaries require device validation before release.
The JVM policy matrix is not a substitute for real platform/OEM behavior.

## Capability contracts

The table describes foreground access for this app targeting 37. Normal permissions
are installed with the app, not user-grantable runtime or special access. Every optional
system request follows a feature-local explanation and explicit button press; onboarding
requests nothing. All permissions can be skipped. No access request starts an engine. Installed-app scans are separately started in Apps; later resumes reconcile a saved scan.

| Capability | API 24–28 | API 29–30 | API 31–32 | API 33–36 | API 37 | User action / limitations |
| --- | --- | --- | --- | --- | --- | --- |
| Installed-app visibility | No permission | No permission; filtering from 30 | Filtered | Filtered | Filtered | No runtime grant. No QUERY_ALL_PACKAGES; MAIN/LAUNCHER queries plus automatically visible packages support inventory in Apps. Current user/profile only. |
| Device information | No permission | Same | Same | Same | Same | No runtime prompt. Public posture indicators in Device; see the device posture contracts below. |
| Notifications | App notification Settings | Same | Same | POST_NOTIFICATIONS runtime | Same | Consent on 33+; Settings recovery when blocked. App-level setting is checked at all supported API levels. Future channels must be checked separately. |
| BLE scanning | COARSE_LOCATION runtime; BLUETOOTH + BLUETOOTH_ADMIN normal | FINE_LOCATION runtime; same normal permissions | BLUETOOTH_SCAN runtime | Same | Same | Runtime consent. Legacy location services must be enabled. BLE hardware is required. No connect/advertise permission; no physical location inference from BLE. neverForLocation can filter beacons. Radio state remains a future engine precondition. |
| Wi-Fi scanning (start and read results) | COARSE_LOCATION runtime | FINE_LOCATION runtime | FINE_LOCATION runtime, request COARSE together | Same | Same | Runtime consent. ACCESS_WIFI_STATE + CHANGE_WIFI_STATE normal. Location services required from 28. Wi-Fi hardware required. This policy covers the complete foreground scan workflow; read-only APIs may have weaker older requirements. |
| Nearby Wi-Fi connections (separate capability) | COARSE_LOCATION runtime | FINE_LOCATION runtime | FINE_LOCATION runtime, request COARSE together | NEARBY_WIFI_DEVICES runtime | Same | Runtime consent for selected nearby connection APIs, no location inference on 33+. Wi-Fi hardware and normal Wi-Fi permissions required. Legacy location services required. Does not authorize scan results. |
| Broad local network discovery | INTERNET normal | Same | Same | Same | INTERNET normal + ACCESS_LOCAL_NETWORK runtime | Runtime consent only on 37. Prior releases have no separate LAN prompt. No Wi-Fi hardware assumption: a LAN can use Ethernet. No traffic is generated. |
| VPN authorization | VpnService.prepare consent | Same | Same | Same | Same | Separate Android consent activity, not a dangerous permission. Device/profile restrictions are respected. No BIND_VPN_SERVICE use or VPN service exists. Authorization itself can disconnect a prior VPN; warning appears before consent. |

On Android 12+, location requests include both coarse and fine; granting approximate
location alone cannot satisfy the precise-location scan contract. **NEARBY_WIFI_DEVICES
never substitutes for location in WifiManager.startScan/getScanResults.** Fine location
is therefore deliberately not capped at API 32. Bluetooth legacy declarations are capped
at 30. Both radio hardware features are optional in the manifest so missing radios do
not prevent installation. The source checks FEATURE_BLUETOOTH_LE and FEATURE_WIFI.

For Android 17, this stage models broad LAN authorization. A future discovery feature
must assess whether Android's system NSD device picker meets its needs with less access.
The deprecated Android 16 developer opt-in is not enabled or used as production policy.

### State and Settings recovery

The reducer returns NotRequested, Granted, Denied, Revoked, Unsupported or Restricted;
unverified UI uses Unknown. Missing hardware and device policy win over permission grants.
Previously granted access becomes Revoked if a fresh check loses permission, notification
enablement, location services or VPN preparation. Device information and navigation stay
usable. Grants are checked on every Activity RESUMED transition and immediately before a
request, including system Settings re-entry and process recreation. Actual results are
rechecked; RESULT_OK alone never grants access. Android can revoke access while the app
is running, so future engines must still recheck and handle SecurityException at use.

A completed runtime denial with no rationale produces an optional Settings recovery
action. Android does not expose an infallible permanent-denial flag: dismissed dialogs,
OEM behavior and permission policy can be ambiguous. The copy says a prompt *may* no
longer appear. Never infer permanent denial from the initial rationale check. Observed
grants clear Settings recovery history; auto-reset grants can be requested again.
Shared Nearby devices group grants are verified per permission rather than assumed.

### Onboarding and lifecycle

Welcome/Choices/Complete persist in one Preferences DataStore after Activity creation.
Request history is stored separately from current OS authorization. Only the selected
explanation uses SavedStateHandle; restoring UI does not replay system prompts. Users
can skip everything, dismiss explanations, or use a session-only continuation after a
storage failure. VPN replacement and separate future background-monitoring consent are
explained in onboarding and feature rationale. No worker, VPN, service, radio or background
monitoring starts on completion, permission grant, boot or resume.

Official references:

- [Android version changes](https://developer.android.com/about/versions)
- [Network Security Config](https://developer.android.com/privacy-and-security/security-config)
- [Background execution limits](https://developer.android.com/about/versions/oreo/background)
- [Package visibility](https://developer.android.com/training/package-visibility)
- [Backup and transfer rules](https://developer.android.com/identity/data/autobackup)
- [Permission minimization](https://developer.android.com/privacy-and-security/minimize-permission-requests)
- [Android 15 target changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [Android 16 target changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- [Android 17 target changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- [Typed navigation](https://developer.android.com/guide/navigation/design/type-safety)
- [Saving UI state](https://developer.android.com/topic/libraries/architecture/saving-states)
- [Startup measurement](https://developer.android.com/topic/performance/appstartup/analysis-optimization)

- [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)
- [Wi-Fi scanning requirements](https://developer.android.com/develop/connectivity/wifi/wifi-scan)
- [Nearby Wi-Fi permissions](https://developer.android.com/develop/connectivity/wifi/wifi-permissions)
- [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- [Android 17 local network protection](https://developer.android.com/privacy-and-security/local-network-permission)
- [Runtime permission workflow and denial](https://developer.android.com/training/permissions/requesting)
- [VPN consent and replacement](https://developer.android.com/develop/connectivity/vpn)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

## Installed-app inventory

All PackageManager calls, label resource loading and certificate hashing run on IO.
The basic inventory never opens APK/split contents or loads icon bitmaps. APK metadata
consists of OS-reported base/split paths and split names; paths do not establish file
readability. Runtime grant flags are observations from GET_PERMISSIONS, not effective
AppOps decisions. Missing flags remain unknown rather than denied or granted.

| API | Scanner implementation |
| --- | --- |
| 24–27 | Integer PackageManager flags, versionCode widened to Long, legacy GET_SIGNATURES SHA-256 fingerprints. No rotation history can be proven. getInstallerPackageName reports nullable installer attribution. |
| 28–29 | longVersionCode and GET_SIGNING_CERTIFICATES. Single-signer history includes platform-verified rotation; multiple-signer identity is the complete apkContentsSigners set, with no invented history. |
| 30–32 | getInstallSourceInfo supplies installing/initiating/originating package names where observable. Originating attribution is normally unavailable without privileged INSTALL_PACKAGES, which is never requested. Package visibility is filtered. |
| 33–37 | PackageInfoFlags overloads and nullable getPackageSource metadata; older releases retain guarded code paths. AndroidX registers package broadcasts as non-exported, including on 34+. |

No cross-user/profile permission or hidden API is used. Install Scadangers separately
inside an allowed work profile to inspect that profile's visible subset. Device-owner,
OEM and enterprise restrictions may reduce visibility or make settings unavailable.
A restricted/empty inventory never establishes absence of risky apps. Declared special
permissions and service-binding permissions do not establish enabled special access.
Settings intents open app details, overlay, unknown-app installation (26+), write
settings, usage access, accessibility, notification listeners, security/device-admin
entry point and all-files access (30+). Some are category screens or OEM-dependent;
opening settings never changes a grant automatically.

Official contracts reviewed for this implementation:

- [PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager)
- [Package visibility declarations](https://developer.android.com/training/package-visibility/declaring)
- [SigningInfo](https://developer.android.com/reference/android/content/pm/SigningInfo)
- [InstallSourceInfo](https://developer.android.com/reference/android/content/pm/InstallSourceInfo)
- [Settings intents](https://developer.android.com/reference/android/provider/Settings)
- [Broadcast receivers](https://developer.android.com/develop/background-work/background-tasks/broadcasts)

Device tests for the new integration must cover APIs 24, 28, 30, 33 and 37, plus OEM
settings and managed-profile behavior. Direct API-37 compilation and JVM tests alone
cannot validate those runtime contracts. No app Baseline Profile generator exists;
add scan/list/search/filter/detail/settings journeys when one is introduced and
regenerate profiles before release. No responsiveness or battery benchmark is claimed.

## Device posture contracts

SDK levels remain min 24 / compile 37 / target 37. Build Tools remain 36.0.0 and JDK 17.
No third-party runtime was added. The assessment uses API-24-compatible date parsing,
not java.time APIs requiring desugaring or API 26. KeyguardManager.isDeviceSecure (23+),
DevicePolicyManager.getStorageEncryptionStatus (11+, per-user code 24+) and the public
Settings.Global.ADB_ENABLED key require no device-admin enrollment or privileged access.
Unsupported/restricted indicators remain unknown. A device without secure-lock hardware
is explicitly unsupported rather than reported protected. Encryption state refers only
to the platform storage indicator and never verifies external media or key hardware.

ConnectivityManager activeNetwork (23+), NetworkCapabilities and LinkProperties require
normal ACCESS_NETWORK_STATE, which is the only added permission. Private DNS is guarded
at API 28; older versions report unsupported. No default network, disappearing state or
a network change during collection yields explicit unavailable observations. The app
retains no addresses, SSIDs or proxy hostnames and sends no probe or discovery traffic.
VPN/proxy presence is descriptive; absence never establishes compromise or protection.

Security, wireless, developer and device-info Settings intents are public.
The dedicated system-update action requires API 36.1; this app uses it on API 37+
and opens device information as the supported update entry point on earlier versions.
OEM update menus may require another user selection. The
narrow intent queries permit resolution on API 30+. OEM/policy restrictions are handled
at both resolution and launch, and the screen explains failures. Returning to Device
reassesses current state. The OS handles all changes and confirmations.

JVM tests cover policy and date behavior for every API from 24 through 37. Runtime tests
must cover API 24, the API-28 Private DNS boundary and API 37, plus managed/OEM devices.
CI's API-24 image runs without Google services to exercise the independent collector.
Local runtime coverage and remaining gaps are recorded in [validation](validation.md).
There is no application Baseline Profile generator. Add Device open, refresh, finding
scroll and settings-return journeys when infrastructure is introduced, and regenerate
profiles before release. No physical-device rendering, battery or memory claim is made.

Public API references reviewed:
[KeyguardManager](https://developer.android.com/reference/android/app/KeyguardManager#isDeviceSecure()),
[DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#getStorageEncryptionStatus()),
[LinkProperties](https://developer.android.com/reference/android/net/LinkProperties#isPrivateDnsActive()),
[Settings.Global](https://developer.android.com/reference/android/provider/Settings.Global#ADB_ENABLED),
[Settings intents](https://developer.android.com/reference/android/provider/Settings).
