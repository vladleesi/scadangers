# Android API compatibility

Contract: minSdk 24, compileSdk 37, targetSdk 37. No library may raise the minimum
through manifest overrides; raising minSdk or lowering targetSdk is never an accepted
workaround. New framework calls need SDK guards. JVM policy tests cover every level
24–37; CI instrumentation covers API 24 and 37.0. The JVM matrix does not replace
real platform and OEM testing.

| API | Consideration | Behavior / requirement |
| --- | --- | --- |
| 24 | Network Security Config, Doze, low-RAM devices | System CA trust, cleartext denied, no background jobs. Guard newer APIs. Test low-RAM startup and recreation. |
| 26 | Background limits, notification channels | No services or notification delivery. Reduced motion uses `areAnimatorsEnabled` (26+) or `ANIMATOR_DURATION_SCALE` (24–25). |
| 28 | Cleartext default, hidden API limits | Cleartext denial applies to all versions. No hidden APIs or sandbox bypass. |
| 29 | Scoped storage, background location limits | No broad storage or background location. Future APK import: Storage Access Framework and bounded streams. |
| 30 | Package visibility | No `QUERY_ALL_PACKAGES`. A narrow MAIN/LAUNCHER query adds launchable packages; users cannot lift filtering. |
| 31 | Exported components, Bluetooth permissions, transfer rules | Only the launcher is exported by app source. BLE uses `BLUETOOTH_SCAN` with `neverForLocation`. Cloud and device-transfer exclusions. |
| 33 | Notification and nearby Wi-Fi consent | `POST_NOTIFICATIONS` and `NEARBY_WIFI_DEVICES` requested only after explanation. Wi-Fi scans still need precise location. |
| 34 | Foreground service types, receiver export flags | No foreground services. Package receiver is dynamic with `RECEIVER_NOT_EXPORTED`. |
| 35 | Enforced edge-to-edge, 16 KB pages | Edge-to-edge with Scaffold insets. No native code; check any future native SDK. |
| 36 | Predictive back, large screens | Platform navigation, no orientation locks or custom back interception. Layout follows width (bar under 600 dp, rail otherwise). |
| 37 | Stricter local-network access | Broad LAN capability requests `ACCESS_LOCAL_NETWORK` after explanation. No sockets or discovery exist; a grant is not monitoring. |

Launcher icons: density-qualified standard and round PNGs on API 24–25, adaptive vectors
from 26, monochrome layer from 33. See [brand resources](design-system.md#brand-icon-resources).

## Capability contracts

Foreground access for this app targeting 37. Every optional request follows a
feature-local explanation and an explicit button press; onboarding requests nothing and
every permission can be skipped. No request starts an engine.

| Capability | API 24–28 | API 29–30 | API 31–32 | API 33–36 | API 37 | Limitations |
| --- | --- | --- | --- | --- | --- | --- |
| Installed-app visibility | None | None; filtered from 30 | Filtered | Filtered | Filtered | No runtime grant. MAIN/LAUNCHER query plus auto-visible packages; current user/profile only. |
| Device information | None | Same | Same | Same | Same | No prompt. See [device posture](#device-posture-contracts). |
| Notifications | App Settings | Same | Same | `POST_NOTIFICATIONS` | Same | Settings recovery when blocked. Future channels need separate checks. |
| BLE scanning | `COARSE_LOCATION` | `FINE_LOCATION` | `BLUETOOTH_SCAN` | Same | Same | Location services required before 31. BLE hardware required. No connect/advertise; no location inference. |
| Wi-Fi scanning | `COARSE_LOCATION` | `FINE_LOCATION` | `FINE_LOCATION` + coarse | Same | Same | Location services required from 28. Wi-Fi hardware required. |
| Nearby Wi-Fi connections | `COARSE_LOCATION` | `FINE_LOCATION` | `FINE_LOCATION` + coarse | `NEARBY_WIFI_DEVICES` | Same | Does not authorize scan results. |
| Local network discovery | `INTERNET` | Same | Same | Same | + `ACCESS_LOCAL_NETWORK` | Prompt only on 37. LAN may be Ethernet. No traffic generated. |
| VPN authorization | `VpnService.prepare` | Same | Same | Same | Same | Separate consent activity; can disconnect a prior VPN, so a warning precedes it. No VPN service exists. |

`NEARBY_WIFI_DEVICES` never substitutes for location in `startScan`/`getScanResults`,
so fine location is not capped at API 32. Bluetooth legacy permissions are capped at 30.
Radio hardware features are optional in the manifest; the source checks
`FEATURE_BLUETOOTH_LE` and `FEATURE_WIFI`. A future discovery feature should first
consider Android's system NSD device picker, which needs less access.

### State and Settings recovery

The reducer yields NotRequested, Granted, Denied, Revoked, Unsupported or Restricted
(Unknown in unverified UI). Missing hardware and device policy win over grants. Access
is rechecked on every RESUMED transition and before each request; `RESULT_OK` alone never
grants access. Future engines must still handle `SecurityException`, since Android can
revoke access while the app runs.

A denial without rationale offers an optional Settings action. Android exposes no
reliable permanent-denial flag, so copy says a prompt *may* no longer appear. Observed
grants clear recovery history. Nearby-device group grants are verified per permission.

### Onboarding and lifecycle

Request history is stored separately from current OS authorization. Only the selected
explanation uses `SavedStateHandle`. No worker, VPN, service, radio or monitoring starts
on completion, grant, boot or resume.

## Installed-app inventory

PackageManager calls, label loading and certificate hashing run on IO. The inventory
never opens APK contents or loads icons; APK metadata is OS-reported paths and split
names. Grant flags come from `GET_PERMISSIONS` and are not effective AppOps decisions;
missing flags are unknown.

| API | Implementation |
| --- | --- |
| 24–27 | Integer flags, `versionCode` widened to Long, `GET_SIGNATURES` SHA-256. No rotation history. |
| 28–29 | `longVersionCode`, `GET_SIGNING_CERTIFICATES`; single-signer history includes verified rotation, multi-signer uses `apkContentsSigners`. |
| 30–32 | `getInstallSourceInfo` (installing/initiating/originating where observable; originating usually unavailable). Filtered visibility. |
| 33–37 | `PackageInfoFlags` overloads and nullable `getPackageSource`; older paths guarded. |

No cross-user permission or hidden API is used. Install Scadangers inside a work profile
to inspect that profile. Enterprise and OEM restrictions can reduce visibility; an empty
or restricted inventory never shows absence of risky apps. Declared special permissions
do not establish enabled special access. Settings intents (app details, overlay, unknown
installs, write settings, usage access, accessibility, notification listeners, device
admin, all-files access on 30+) are OEM-dependent and never change a grant.

Runtime tests for this integration should cover APIs 24, 28, 30, 33 and 37, OEM settings
and managed profiles. No Baseline Profile generator exists; add scan/list/search/filter/
detail/settings journeys with one and regenerate before release.

## Device posture contracts

- Date parsing is API-24 compatible (no `java.time` desugaring). `KeyguardManager.isDeviceSecure`,
  `DevicePolicyManager.getStorageEncryptionStatus` and `Settings.Global.ADB_ENABLED` need no
  admin or privileged access. Unsupported or restricted indicators are unknown; a device
  without secure-lock hardware is unsupported, not protected. Encryption refers to the
  platform storage indicator only.
- Network state needs normal `ACCESS_NETWORK_STATE` (the only added permission). Private
  DNS is guarded at 28. A missing default network or change mid-collection yields an
  explicit unavailable observation. No addresses, SSIDs or proxy hosts are kept and no
  probe traffic is sent. VPN/proxy presence is descriptive.
- Security, wireless, developer and device-info Settings intents are public. The dedicated
  system-update action needs API 36.1, so it is used on 37+; earlier versions open device
  information. Narrow intent queries enable resolution on 30+. Resolution and launch
  failures are explained, and returning to Device reassesses.
- Runtime tests should cover API 24, the API-28 Private DNS boundary, API 37 and
  managed/OEM devices. CI's API-24 image has no Google services. Gaps are in
  [validation](validation.md). No Device Baseline Profile journeys exist yet.

References: [Android version changes](https://developer.android.com/about/versions),
[Network Security Config](https://developer.android.com/privacy-and-security/security-config),
[package visibility](https://developer.android.com/training/package-visibility),
[Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions),
[Wi-Fi scanning](https://developer.android.com/develop/connectivity/wifi/wifi-scan),
[Android 17 local network](https://developer.android.com/privacy-and-security/local-network-permission),
[VPN consent](https://developer.android.com/develop/connectivity/vpn),
[PackageManager](https://developer.android.com/reference/android/content/pm/PackageManager),
[Settings intents](https://developer.android.com/reference/android/provider/Settings).
