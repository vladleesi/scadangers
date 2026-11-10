# Validation status

Scadangers `0.1.0` release setup, checked locally on 2026-11-10 with JDK 17,
SDK `android-37.0` and Build Tools `36.0.0`. Android API levels remain 24/37/37.

## Passing locally

- Direct Kotlin 2.4.21 compilation against the API 37 SDK compiled app and test
  sources with the Compose and serialization compiler plugins. Build Tools linked
  resources under the renamed namespaces. JUnit passed 68 tests and the Python
  dependency audit suite passed 7 tests. KtLint and Detekt passed with project rules.
- Standalone KSP generated Room implementations and Hilt factories under
  `dev.scadangers`. The exported Room schema matches the checked-in version 1 schema.
- Local Git and simulated GitHub API checks covered initial release, retry, repeated
  version, stale commits, tag collisions, and non-fast-forward rejection. The required
  check fails if a build or device job fails, is skipped, or is cancelled. The ruleset
  requires the same check from GitHub Actions. The Dependabot workflow handled an empty
  alert list, issue creation, deduplication, API errors, and issue closure.

## Pending verification

- Gradle could not start because the sandbox denied the socket used by its cache lock
  service. APK assembly, Gradle lint, instrumentation execution, and a fresh runtime
  dependency graph remain unverified. Direct compilation and standalone KSP do not
  exercise APK packaging or Hilt runtime injection.
- The online dependency audit could not resolve its service host. Previous validation
  found a Lifecycle library lint compatibility error (`ObsoleteLintCustomCheck`).
  GitHub promotion requires the build, audit, and device checks to succeed.
- GitHub Actions and the main ruleset require live verification. The local workflow
  checks used a disposable Git remote and simulated API responses.

Physical-device startup, memory, scrolling and battery measurements remain pending.
See [Android compatibility](android-compatibility.md#installed-app-inventory) for
platform validation and Baseline Profile coverage.
