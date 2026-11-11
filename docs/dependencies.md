# Dependency decisions

Reviewed 2026-11-10. Versions are pinned in `gradle/libs.versions.toml`, without
preview Android libraries or dynamic selectors. No minSdk overrides are permitted.

| Component | Version / decision |
| --- | --- |
| Android Gradle Plugin | 9.5.1; official API 37 support, minimum Gradle 9.6, JDK 17, Build Tools 36 |
| Gradle | 9.9.1 wrapper with official distribution SHA-256; CI lint/version and security update |
| Kotlin | 2.4.21; AGP built-in Kotlin, no duplicate Kotlin Android plugin |
| Compose compiler / Serialization plugin | 2.4.21, aligned with Kotlin |
| Compose UI/Foundation | 1.12.1, stable; explicit aligned versions |
| Compose UI tooling / preview | 1.12.1; preview annotations in the design system, tooling debug-only for Android Studio previews. The app debug manifest removes tooling's exported `PreviewActivity` |
| Material 3 | 1.4.0 stable |
| Activity / Core / Lifecycle | 1.13.0 / 1.19.1 / 2.11.0 stable |
| Navigation Compose | 2.10.2 stable, typed routes and saved state |
| Hilt | 2.60.1, KSP processing in app only |
| KSP | 2.3.12 (KSP2); 2.3.13 release documents a concurrency regression |
| Coroutines / Serialization runtime | 1.11.0 / 1.12.0 |
| Room | 2.8.5, maintained stable Room 2 line; version 1 local app inventory schema; KSP exports schemas |
| DataStore | 1.3.0, preferences dependency in app; one lazy instance for onboarding/access history |
| Tests | JUnit 4.13.2; AndroidX runner/core 1.7.0 and ext JUnit 1.3.0 |
| Static analysis | Stable Detekt 1.23.8 and KtLint CLI 1.8.0 (shadowed CLI variant) in isolated Java processes |
| Fonts / icons | Platform fonts and Material Icons path data (Apache 2.0); user-supplied exact Android brand icon package, see [design system](design-system.md#brand-icon-resources). No image-loading dependency |

The root Kotlin JVM plugin dependency selects the Kotlin version for AGP built-in
Kotlin. Android modules use Java 17 compilation; JVM-only modules use JDK 17.
No AGP DSL opt-out, hidden APIs or minSdk override is used. Manifest `tools:targetApi`
annotations document flags on permissions introduced at API 31/33; older systems
ignore those unknown permission names. They do not alter the minimum or target SDK.
Detekt's older embedded Kotlin compiler is isolated from the application compiler;
analysis uses syntax rules without type resolution. Reassess for newer language syntax.

Published release documentation establishes the candidate compatibility set. Full
artifact, manifest-minSdk, compiler-plugin and R8 compatibility still require a
successful Gradle build. Local debug/release builds, JVM tests and API 31 instrumentation
now pass; lint and the license audit still fail (see [validation](validation.md)). Do
not treat these pins as a verified production combination until CI passes.

DataStore moved from the unused settings dependency into `app` for real durable
onboarding state. `core:security` uses the already pinned Coroutines runtime for
serialized access evaluation. No dependency version changed. DataStore 1.3.0 is the
current stable release; its existing Android minimum remains compatible with API 24.
See the [official DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore).

## Audits

`dependencyInventory` resolves debug/release runtime graphs (including transitive and
platform variants). The Python standard-library audit sends public Maven coordinates
to OSV in batches of 100. It fetches POM license metadata only from Google Maven and
Maven Central, with timeouts, response bounds, four workers and bounded parent lookup.
Unknown/missing licenses, vulnerability findings and metadata/service failures fail
CI. No automatic suppressions. Reports are uploaded even on failure. Approve new
license names only after review; POM metadata is not a substitute for license/legal
review or bundled NOTICE attribution before distribution. OSV coverage is incomplete;
a passing query does not prove absence of vulnerabilities. Tooling/plugin dependencies
are outside this runtime license audit. Renovate reports updates from the shared preset
in its Dependency Dashboard. Updates requested there target `develop`. CI runs when changes reach that branch.

Official references:

- [AGP 9.5 compatibility](https://developer.android.com/build/releases/agp-9-5-0-release-notes)
- [Built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Kotlin release history](https://kotlinlang.org/docs/releases.html)
- [AndroidX stable versions and minimum SDK policy](https://developer.android.com/jetpack/androidx/versions)
- [Compose versions](https://developer.android.com/jetpack/androidx/releases/compose)
- [Compose compiler setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup)
- [KSP releases](https://github.com/google/ksp/releases)
- [Coroutines releases](https://github.com/Kotlin/kotlinx.coroutines/releases)
- [Serialization releases](https://github.com/Kotlin/kotlinx.serialization/releases)
- [AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test)
- [Detekt releases](https://github.com/detekt/detekt/releases)
- [KtLint](https://github.com/pinterest/ktlint)
- [OSV API](https://google.github.io/osv.dev/api/)


Installed-app inventory reuses the pinned Room 2.8.5, Coroutines and Serialization
runtime. `core:security` applies the already pinned serialization compiler plugin
for immutable persisted records; `core:database` uses these records and exposes Room
runtime to its app-owned lazy builder. `feature:apps` depends on security models only,
with no Android collection dependency. No new third-party library or version was added.
Room 2.8 requires API 23 and therefore supports minSdk 24; see
[official Room releases](https://developer.android.com/jetpack/androidx/releases/room).
Full Gradle integration, packaging and R8 validation of this stage still require an
unrestricted build environment (see [validation](validation.md)).

Device posture adds no external runtime dependency. `core:security` now uses
`core:common` device normalization, and `feature:device` uses security models. A shared
design-system API constraint aligns transitive `lifecycle-runtime` with the existing
2.11.0 app pin: standalone feature lint otherwise resolves 2.9.4 and fails loading its
obsolete Lifecycle checks under AGP 9.5.1. No check is disabled or baselined. The current
stable version is confirmed by the [official Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle).

The wrapper moves to stable Gradle 9.9.1, above AGP 9.5.1's required minimum 9.6,
with its official distribution checksum. See [release notes](https://docs.gradle.org/9.9.1/release-notes.html).
The runtime audit retains fatal unknown-license, vulnerability and service failures.
For `javax.inject:javax.inject:1` only, its POM omits the license: source headers from
its exact Maven Central source archive establish Apache-2.0 (JSR-330 Expert Group,
2009). Each audit verifies SHA-256
`c4b87ee2911c139c3daf498a781967f1eb2e75bc1a8529a2e7b328a15d0e433e`
before using that reviewed record; unknown versions and checksum/service failures
still fail. The report includes this provenance. The upstream copyright and full
Apache license are bundled in
[the APK asset](../app/src/main/assets/third-party/javax.inject-1-NOTICE.txt).
