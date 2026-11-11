# Validation

Last full local validation: Scadangers 0.2.0 / versionCode 2 on 2026-11-11 with JDK 17,
Gradle 9.9.1, SDK android-37.0 and Build Tools 36.0.0. This is a record of that run, not
evidence for later revisions; rerun the checks for each change.

```sh
./gradlew :app:assembleDebug :app:assembleRelease test lint ktlintCheck detekt :app:assembleDebugAndroidTest :app:dependencyInventory --continue --console=plain
python3 -m unittest discover -s tools/tests -v
python3 tools/audit_dependencies.py app/build/reports/dependencies/coordinates.txt
```

## Results

- Debug and instrumentation APKs built; the release APK passed R8 and resource shrinking
  and is unsigned. Metadata confirms the application ID, version and APIs 24/37.
- 82 JVM tests passed (53 core, 29 Android-module). Security tests cover strict patch
  dates, patched and potentially applicable cases, vendor uncertainty, missing Google
  signals, APIs 24–37, failed refresh, cancellation, cleanup and deadlines.
- Lint, KtLint and Detekt passed.
- 14 Python tests passed (license checksum, tampering, unknown versions, service failure,
  changelog notes). The online audit passed for 150 coordinates with zero findings; OSV
  coverage is incomplete.
- The merged release manifest exports only the launcher activity. ProfileInstaller's
  receiver requires `DUMP`; Room's service and the startup provider are not exported.
  Backup and cleartext are disabled, system-CA trust is kept, and the only added
  permission is `ACCESS_NETWORK_STATE`.
- All 42 instrumentation tests passed on a local API-31 emulator. Coverage: public-API
  observations, 200 % text, settings events, app inventory, navigation, recreation and
  the design system.
- The release workflow was checked earlier with actionlint 1.7.12 and disposable Git
  remotes (promotion, stale runs, tag conflicts, retries, draft and API failures). That
  predates the move of promotion to `main` and has not been repeated.

## Remaining limits

- API 24 (AOSP) and API 37.0 instrumentation run only in CI and must pass before
  promotion. The local API-31 run does not verify them.
- The API-28 Private DNS boundary, other intervening versions, OEM settings, managed
  profiles and low-RAM or physical devices still need runtime validation before release.
- No physical-device startup, frame-time, memory or battery measurements exist. No
  Baseline Profile generator exists; add Apps and Device journeys and regenerate when
  one is introduced.
- The bulletin catalog is deliberately incomplete (six entries, no AOSP coverage for
  APIs 27–29, reviewed 2026-11-11). No comprehensive CVE, malware, root, support-end,
  boot-integrity or overall safety verdict is claimed. See
  [posture contracts](android-compatibility.md#device-posture-contracts) and
  [architecture](architecture.md#device-posture-assessment).
