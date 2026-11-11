# Architecture and privacy boundary

## Modules

- `app` → feature modules, `core:common`, `core:security`, `core:database`, design system.
  It owns the Android adapters (capability source, package scanner, device collector),
  Hilt wiring and the lazy DataStore.
- `feature:*` → design system only (`device` also uses `core:common`; `apps` and `device`
  use security models). Features never depend on each other; `app` passes cross-feature
  content through composable slots. Features render state and emit events; they never
  check permissions or launch system activities.
- `core:common`, `core:security`, `core:testing` are Kotlin/JVM and independent of Compose.
  `core:database` is Room (KSP) plus security models. `core:testing` is test-only.

Implemented: installed-app inventory with local metadata evaluation, and a limited
device posture assessment. There is no backend, account, worker, app service, root
integration, VPN service or `BIND_VPN_SERVICE`. Permission and VPN authorization
requests only prepare optional access; no radio scanning, traffic inspection or
notification delivery exists.

## UI and state

Single-activity Compose app themed by the [design system](design-system.md). Five
primary destinations (Overview, Apps, Network, Nearby, Alerts) use a bottom bar on
compact windows and a rail on wider ones; Settings opens from the top bar and Device
from the overview. Typed Navigation Compose routes carry no sensitive arguments and
the back stack is saved.

- Stateless placeholders stay stateless. `AppsViewModel` owns scan events, query/filter/sort
  state and the saved detail selection; list preparation runs on Default.
- `AccessViewModel` exposes immutable StateFlow and owns `viewModelScope` work. The Activity
  registers stable result launchers and refreshes on every RESUMED transition.
- `core:security/capability` holds the Android-independent API policy, state reducer and
  serialized `AccessRepository`. `AndroidAccessSource` reads fresh platform state on IO.
  Stored history explains denied or revoked states but never authorizes access.
  Storage or platform failures clear displayed grants and offer retry.
- Onboarding (Welcome, Choices, Complete) is saved in one lazily opened Preferences
  DataStore and requests no permission. Restoring state never replays a system prompt.
  On a storage failure the user may continue for the session only.
- Overview shows `NoDeviceAssessment`: it does not aggregate findings or claim
  comprehensive assessment or continuous protection.

See the [capability matrix](android-compatibility.md#capability-contracts) for API
boundaries and platform limits.

## Startup and performance

- `Application` only installs Hilt. Nothing opens storage or starts an engine at startup.
  Room opens when Apps first loads its saved inventory; DataStore is opened after
  Activity creation.
- DataStore holds onboarding step, permission-request history and previously observed
  grants. It holds no device identifiers, app lists, location or traffic.
- Android observations and bitmap decoding run off Main. No main-thread Room queries,
  destructive migrations, polling, wake locks, foreground services or unbounded jobs.
- The app scanner reads serially in batches of 24 with cancellation checks, progress and
  a 90-second deadline. Binder calls cannot be interrupted mid-call.
- No startup, memory, battery or frame-time measurements exist. Measure on a low-RAM
  API 24 device before making any performance claim.

## Android security boundary

- Network Security Config disallows cleartext and trusts system CAs only, with no debug
  override. It covers app traffic only. No network request is implemented; `INTERNET`
  is declared for legacy LAN authorization.
- Only the launcher activity is exported and it reads no external extras. Package events
  use a foreground-only, non-exported dynamic receiver. Merged release manifest also has
  Room's non-exported invalidation service and ProfileInstaller's receiver protected by
  `DUMP`. Debug adds the Compose test host; `PreviewActivity` is removed. Review the
  merged manifest before each release.
- Backup is disabled. Legacy backup and API 31+ transfer rules exclude all data domains,
  including DataStore and the Room inventory (package names, signing fingerprints,
  permissions, findings). Vendor transfer tools vary; do not promise protection from
  privileged OEM tooling. Revisit when storage scope changes.
- No logs, uploads, APK payloads or custom cryptography. No MASVS compliance, malware
  detection efficacy or performance claim is made.

Executed and pending checks are in [validation](validation.md).

## Installed-app scanner

`core:security/apps` separates the immutable inventory, `AppRiskEvaluator` and the
serialized `AppScanner`. `app/scanner` owns PackageManager access, package events and
presentation adapters; `core:database` is the Room store; `feature:apps` renders state.
One Activity/ViewModel-owned lifecycle needs no separate engine module.

- Full scans refresh metadata. Incremental scans compare cheap package stamps (version
  code, install and update times), reuse unchanged metadata and refresh permission flags.
  Failed optional metadata is explicit and retried next scan.
- Vanished or inaccessible packages have no current assessment; stale rows are not shown
  as fresh. A stamp reconciliation and package-event generation check retry once on
  races. Repeated churn, global errors or cancellation keep the previous completed scan.
  PackageManager has no atomic snapshot.
- Opening or resuming Apps reconciles a saved scan. While resumed, a dynamic receiver
  conflates add/replace/change/remove events into refreshes; missed events are reconciled
  on the next resume. Manual scans are cancellable and Activity-owned.
- Room stores one JSON result per package plus scan metadata in one transaction. Process
  death never persists a partial scan. Export schemas at build time and add explicit
  migrations for new versions.

Policy v1 flags target SDK below 29, installers outside the explicit Google Play/Amazon/
Samsung list, Internet plus at least two sensitive permission categories, and declared
special permissions or protected services. Install/update notices (seven days) are
informational. Sideloading is not a malware verdict; store package names do not
authenticate trust. System apps get the same rules. Signing-lineage rotation is reported
separately from changed fingerprints; missing fingerprints never prove a signer change.
AppOps, active accessibility/admin/listener/VPN grants and other profiles are not inferred.
No APK contents or library CVEs are analyzed, and every app says so. See the
[API contracts](android-compatibility.md#installed-app-inventory).

## Device posture assessment

`core:security/device` holds immutable observations, severity/confidence/status,
strict dates, reviewed bulletin facts and deterministic evaluation. `app/device` owns the
public-API collector, a destination-activated ViewModel and settings launches;
`feature:device` renders state and emits events.

Opening or resuming Device reads screen-lock and storage-encryption indicators, USB
debugging, default-network capabilities, Private DNS (28+), proxy presence and settings
resolution. IO collection is sequential, cancellable between calls and bounded by a
10-second deadline; leaving the screen cancels it. No polling, service, storage, network
request, device identifier or network address is added. The only new permission is
`ACCESS_NETWORK_STATE`. There is no automatic remediation.

Limits:

- The offline catalog is deliberately incomplete: six Android Security Bulletin rows
  (2017-09, 2023-09, 2026-11), reviewed 2026-11-11, with no AOSP row for APIs 27–29.
  Changes need source review and an app release; there is no live update.
- Dates establish potential applicability or the OS's patch claim, never exploitability
  or verified patching. Unlisted releases are outside coverage, not certified safe. The
  MediaTek modem entry stays unknown without exact firmware information. Missing,
  invalid or future patch dates and failed reads remain unknown.
- Patch age over 90 days is a local review policy, not a vendor support-end claim.
  Support for a specific model, region or carrier is unknown.
- Play Integrity needs Google services, project enrollment and server verification, which
  a local-only app lacks, so it is not evaluated. Verified Boot, bootloader state, root,
  malware, credential strength, external-storage encryption and hardware-backed keys are
  not inferred. Missing signals do not indicate compromise.

Sources: [ASB catalog](https://source.android.com/docs/security/bulletin),
[Play Integrity verdicts](https://developer.android.com/google/play/integrity/verdicts).
