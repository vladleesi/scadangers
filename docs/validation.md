# Validation

Scadangers 0.2.0 / versionCode 2, validated locally on 2026-11-11 using JDK 17,
Gradle 9.9.1, SDK android-37.0 and Build Tools 36.0.0. Application ID and source
namespaces are `dev.vladleesi.scadangers`. Minimum/compile/target APIs remain 24/37/37.

## Build and static checks

The complete required Gradle build passed:

```sh
./gradlew :app:assembleDebug :app:assembleRelease test lint ktlintCheck detekt :app:assembleDebugAndroidTest :app:dependencyInventory --continue --console=plain
```

- Debug APK and instrumentation APK packaged; release APK passed R8/resource shrinking
  and remains unsigned. APK metadata confirms the new ID, version 0.2.0 / 2 and APIs
  24/37. The bundled javax.inject license asset is present.
- 82 JVM tests passed with zero failures/skips (53 JVM-core tests and 29 Android-module
  JVM tests). Security coverage includes strict patch dates, patched/potentially
  applicable scenarios, vendor uncertainty, missing Google signals, all API levels
  24–37, failed refresh, cancellation, resource cleanup and deadlines.
- Full lint, KtLint and Detekt passed. A shared Lifecycle constraint resolves current
  lint checks in standalone features. Package inventory deliberately accepts Android's
  filtered visibility; its narrow QueryPermissionsNeeded annotation documents this
  contract and does not add broad package access.
- Fourteen Python tests passed, covering exact source-license checksum validation,
  tampering, unknown versions, service failure and version-specific changelog notes. The online runtime audit passed for
  the current 150-coordinate inventory with zero findings. OSV coverage is incomplete
  and does not establish absence of vulnerabilities.
- Workflow YAML, GitHub expression contexts (actionlint 1.7.12) and Bash steps passed
  local validation. Disposable Git-remote tests
  cover master-only release source, fast-forward promotion, stale runs, tag conflicts,
  retries, draft/API failures and descriptions from the exact tagged changelog. The live
  master ruleset was applied through GitHub's API; no ruleset JSON change is included.
  SDK package availability was checked against Google's
  repository metadata, including API-24 AOSP x86_64. PR validation cannot publish;
  develop pushes retain the existing checked-commit release process.
- Merged release manifest: only the launcher activity is openly exported; the
  ProfileInstaller receiver remains protected by DUMP. Room's service and AndroidX
  startup provider are non-exported. Backup remains disabled, cleartext remains
  disabled and the system-CA Network Security Config is preserved. The only added
  permission is normal ACCESS_NETWORK_STATE; settings visibility queries are narrow.

## Runtime coverage and remaining limits

All 42 instrumentation tests passed with zero failures/skips on the available API-31
emulator with stable Espresso 3.7.0 (core and idling resource). Coverage includes
real public-API observations, 200% text, settings events,
app inventory, navigation, recreation and the shared design system. CI covers API 24 on an AOSP image without
Google services and API 37.0. These remote jobs must pass before promotion; a local
API-31 result does not verify those platform versions.

API-28 Private DNS boundary behavior, other intervening versions, OEM settings,
managed profiles and low-RAM/physical devices still require runtime validation before
release. The JVM API matrix is not a substitute for platform behavior. No physical-device
startup, frame-time, memory or battery measurements have been made. No application
Baseline Profile generator exists; add the documented Apps and Device journeys and
regenerate profiles when that infrastructure is introduced.

The bundled bulletin catalog is deliberately incomplete (six selected entries), has
no AOSP coverage for APIs 27–29 and is reviewed as of 2026-11-11. No comprehensive CVE,
malware, root, support-end, boot integrity or overall safety verdict is claimed. See
[posture contracts](android-compatibility.md#device-posture-contracts) and
[architecture](architecture.md#device-posture-assessment).
