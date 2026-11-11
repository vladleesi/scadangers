# Dependency decisions

Reviewed 2026-11-10. Versions are pinned in `gradle/libs.versions.toml`: no preview
Android libraries, dynamic selectors or minSdk overrides.

| Component | Version / decision |
| --- | --- |
| Android Gradle Plugin | 9.5.1 (API 37 support; needs Gradle 9.6+, JDK 17, Build Tools 36) |
| Gradle | 9.9.1 wrapper with official distribution SHA-256 |
| Kotlin / Compose compiler / Serialization plugin | 2.4.21; AGP built-in Kotlin, no separate Kotlin Android plugin |
| Compose UI, Foundation, tooling | 1.12.1. Tooling is debug-only; the debug manifest removes its exported `PreviewActivity` |
| Material 3 | 1.4.0 |
| Activity / Core / Lifecycle | 1.13.0 / 1.19.1 / 2.11.0 |
| Navigation Compose | 2.10.2 (typed routes, saved state) |
| Hilt / KSP | 2.60.1 (app only) / 2.3.12 (KSP 2.3.13 documents a concurrency regression) |
| Coroutines / Serialization runtime | 1.11.0 / 1.12.0 |
| Room | 2.8.5 (Room 2 line, requires API 23; schema v1 exported by KSP) |
| DataStore | 1.3.0, Preferences, in `app`; one lazy instance |
| Tests | JUnit 4.13.2; AndroidX runner/core 1.7.0, ext JUnit 1.3.0, Espresso 3.7.0 |
| Static analysis | Detekt 1.23.8, KtLint CLI 1.8.0, each in an isolated Java process |
| Fonts / icons | Platform fonts; Material Icons path data (Apache 2.0); supplied brand icons ([design system](design-system.md#brand-icon-resources)). No image-loading library |

Notes:

- Android modules compile with Java 17; JVM-only modules use JDK 17. No AGP opt-out or
  hidden API is used. `tools:targetApi` on API 31/33 permissions documents flags only;
  older systems ignore them.
- Detekt's older embedded Kotlin compiler is isolated and runs syntax rules without type
  resolution. Reassess for newer language syntax.
- `lifecycle-runtime` is constrained to the app's 2.11.0 pin; otherwise standalone
  feature lint resolves 2.9.4 and fails loading obsolete Lifecycle checks. No check is
  disabled or baselined.
- Espresso 3.7.0 is pinned because Compose UI test's transitive 3.5.0 calls the
  reflective `InputManager.getInstance`, which API 37 lacks. It is androidTest-only.
- Device posture and the app inventory add no third-party dependency.

## Audits

`dependencyInventory` resolves the debug and release runtime graphs. The Python audit
(`tools/audit_dependencies.py`, standard library only) sends Maven coordinates to OSV in
batches of 100 and reads POM licenses only from Google Maven and Maven Central, with
timeouts, response limits and bounded concurrency. Unknown licenses, vulnerability
findings and service failures fail CI; there are no automatic suppressions, and reports
upload even on failure. New license names need review. OSV coverage is incomplete, so a
pass does not prove absence of vulnerabilities, and POM metadata does not replace legal
review or NOTICE attribution. Build tooling is outside this runtime audit.

`javax.inject:javax.inject:1` omits its license in the POM. Its Maven Central source
archive headers establish Apache-2.0, and the audit verifies SHA-256
`c4b87ee2911c139c3daf498a781967f1eb2e75bc1a8529a2e7b328a15d0e433e` before using that
reviewed record; any other version or a checksum failure still fails. The license is
bundled in [the APK asset](../app/src/main/assets/third-party/javax.inject-1-NOTICE.txt).

Renovate reports updates from the [shared preset](https://github.com/vladleesi/renovate-config)
in its Dependency Dashboard; approved updates target `develop`.

References: [AGP 9.5](https://developer.android.com/build/releases/agp-9-5-0-release-notes),
[AndroidX versions](https://developer.android.com/jetpack/androidx/versions),
[Compose](https://developer.android.com/jetpack/androidx/releases/compose),
[Kotlin](https://kotlinlang.org/docs/releases.html),
[KSP](https://github.com/google/ksp/releases),
[Room](https://developer.android.com/jetpack/androidx/releases/room),
[DataStore](https://developer.android.com/jetpack/androidx/releases/datastore),
[AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test),
[OSV API](https://google.github.io/osv.dev/api/).
