# Changelog

## 0.2.0 — 2026-11-11

### Added

- Local device security posture assessment in Overview → This device → Details:
  reported Android/API, manufacturer/model, patch age, secure screen lock, storage
  encryption, USB debugging, default-network and settings-availability indicators.
- Offline comparison with six reviewed Android Security Bulletin entries. Findings
  separate severity from confidence, retain evidence and official references, and
  distinguish potential applicability, OS-reported patch coverage and unknowns.
- Assessment refresh, cancellation, settings links and explicit scope limitations.
  Google services are optional; Play Integrity and Play Protect are not evaluated.

### Changed

- Application ID, Kotlin packages, module namespaces, tests and Room schema package
  moved to `dev.vladleesi.scadangers`. Earlier package installations keep separate
  private data and are not upgraded or migrated in place.

### Security and limitations

- Only normal ACCESS_NETWORK_STATE was added; no privileged API, exploit test,
  background monitor, telemetry, backend or automatic system change is used.
- The bulletin catalog is incomplete and bundled with the app. Exact vendor support,
  chipset applicability, backports and exploitability remain unverified. No finding
  or patch claim proves overall device safety.

### Fixed

- Successful develop checks automatically fast-forward `master`. Tags and releases
  are created only from the verified promoted commit checked out from `master`.
- GitHub release descriptions use the matching changelog section. Existing releases
  use the changelog at their immutable tag, verified to belong to master history.
- API 37 instrumentation uses stable Espresso 3.7.0, replacing the obsolete
  reflective InputManager call in Compose's transitive Espresso 3.5.0.
- `Android checks` skips when verification or instrumentation does not succeed;
  publishing still requires successful upstream validation.
- CI SDK setup no longer requests Google's removed `tools` package. Pull requests
  to `develop` run validation without publishing, and API 24 tests use an AOSP image
  without Google services.
- Standalone feature modules use the app's pinned Lifecycle version for compatible
  lint checks. Fixed Compose modifier ordering and a stale installed-app UI assertion.
- Updated the checksum-pinned Gradle wrapper to stable 9.9.1. Documented intentional
  filtered package visibility at its collection boundary.
- Dependency auditing verifies a reviewed, checksum-pinned source license for
  `javax.inject:1`, whose POM omits it, and bundles its Apache license. Unknown
  versions, changed source artifacts, service errors and vulnerabilities still fail.

## 0.1.0 — 2026-11-10

- Initial native Kotlin/Compose Android application with adaptive navigation,
  shared Material 3 design system, onboarding and contextual access management.
- Visible installed-app inventory with local metadata risk evaluation, permission
  review, signing evidence, search/filter/detail screens and app-private Room storage.
- Device facts and honest placeholders for unimplemented network, nearby and alert
  engines. No continuous device protection, root or malware verdicts.
- Android API 24 minimum and API 37 compile/target, restricted network/backup policies,
  unsigned shrunk release builds, automated tests, dependency auditing and the
  `develop` → `main` CI/release workflow.
