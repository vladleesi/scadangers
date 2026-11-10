# Architecture and privacy boundary

## Dependencies

- `app` → feature modules, common, security, database, design system.
- `feature:*` → design system; device also uses common for `DeviceProfile`, and apps uses
  security inventory models. Features remain independent of each other.
- `app` owns the Android capability adapter, Hilt access repository and lazy DataStore.
  Features receive access controls through composable slots, without Android checks.
- `core:database` → Room with KSP, security models and serialization; version 1 stores
  per-package observations/results and completion metadata atomically.
- `core:common`, `core:security`, `core:testing` → Kotlin/JVM, independent of Compose.
- Features never depend on each other. The overview shows other features' summaries
  (network protection control, device table) through composable slots wired by `app`.
- `core:testing` is a test dependency only.

Installed-app inventory and local metadata evaluation are implemented. There is no
backend, account, worker, app service or root integration. Permission and VPN
authorization requests prepare optional access only; no radio scanning, traffic
inspection or notification delivery is implemented.
There is no VPN service or BIND_VPN_SERVICE declaration.

## UI and state

Single-activity Compose themed by the design system ([design system](design-system.md)).
Five primary destinations (Overview, Apps, Network, Nearby, Alerts) appear in a bottom
navigation bar on compact windows and a navigation rail on wider ones. Settings opens
from the top app bar; device details open from the overview. Seven serializable route
objects use Navigation Compose's typed APIs. `rememberNavController` saves the back
stack; switching primary destinations uses singleTop, saveState and restoreState.
Routes carry no sensitive arguments. The root row owns the window background, and
navigation bars, top bar and content handle system and display-cutout insets.

Device facts come from static `Build` properties read in `app` (no I/O, IPC or
permission) and are normalized and bounded in `DeviceProfile`. They are descriptive,
not an assessment. Apps shows visible package observations and explainable review
signals. Other security features remain unavailable. `OverviewUiState.NoDeviceAssessment` does not
aggregate app findings or claim a full-device assessment or continuous protection;
open Apps for its last completed inventory.

Stateless feature placeholders remain stateless. `AppsViewModel` owns installed-app
scan events, read-only StateFlow, query/filter/sort state and saved detail selection.
List preparation runs on Default; the lazy list uses package keys and content types.
App details use local saved selection and BackHandler; system settings are opened by
app adapters, with unavailable/restricted screens reported explicitly.

`AccessViewModel` exposes immutable StateFlow, collects UI events and owns
structured viewModelScope work. The Activity
registers stable Activity Result launchers and refreshes on every RESUMED transition,
including return from Settings. Composables render capability state and emit events;
they never check Android permissions or launch system activities.

`core:security/capability` contains the Android-independent API policy, deterministic
state reducer and serialized `AccessRepository`. Requirements distinguish manifest,
runtime and special access, hardware, location services and limited package visibility.
`AndroidAccessSource` reads fresh platform observations on Dispatchers.IO. Stored
history explains denied/revoked states but cannot authorize access. Expected storage
or platform failures clear displayed grants and expose retry, without blocking other
screens. Cancellation propagates.

Onboarding has Welcome, Choices and Complete steps, durably saved in one lazily opened
Preferences DataStore. It requests no permission. Users can skip optional access and
later choose Review access within each feature or Settings. A restored explanation is
held in SavedStateHandle; restoring it never launches a system request. The Activity
Result registry delivers outstanding results after recreation. System commands are
transient and consumed once, never replayed from saved state. Incomplete onboarding
writes remain incomplete after process death; completion is shown after the durable
write. A storage failure permits an explicitly session-only continuation.

See the authoritative [capability matrix](android-compatibility.md#capability-contracts)
for API boundaries, user actions and platform limits.

## Startup and performance

Application only installs Hilt infrastructure. Application startup opens no storage
and starts no engine. Access state is loaded after Activity creation; the singleton
DataStore delegate performs file work asynchronously. It stores only onboarding step,
permission-request/recovery history and previously observed capability grants. It
stores no device identifiers, app lists, location or traffic. Room opens lazily when
the Apps destination first loads its saved inventory, never at app startup.
Android access observations and sampled brand-bitmap decoding run off Main. Updates are
serialized; no main-thread Room queries or destructive migrations are allowed.

No polling, wake locks, foreground services, unbounded jobs, or startup engine loads.
The installed-app engine runs serial reads in batches of 24 with cancellation checks,
progress and a 90-second coroutine deadline. Platform Binder calls cannot be preempted
mid-call. Other future engines must support cancellation and bounded work;
measure startup, allocations, battery and frame timing on a low-RAM API 24 device
before claiming performance. Baseline metrics have not been collected in this stage.

## Android security boundary

Network Security Config disallows cleartext and trusts only system CAs, with no debug
trust override. The manifest declares only the permissions needed for the
[access contracts](android-compatibility.md#capability-contracts), including INTERNET
for legacy LAN authorization. No network request is implemented. This policy applies
to app traffic only and does not filter other applications' traffic or raw sockets.
The launcher is exported solely for MAIN/LAUNCHER; it consumes no external extras.
No other app components are explicitly declared. Package events use a foreground-only,
non-exported dynamic receiver; no persistent monitor or manifest receiver is added.
The previous merged release contained Room's non-exported MultiInstanceInvalidationService
and ProfileInstaller's exported receiver protected by android.permission.DUMP. Debug
includes the ComponentActivity host used by Compose instrumentation; PreviewActivity
remains removed. Review freshly merged manifests before release.

Backup is disabled; legacy backup and API 31+ cloud/device-transfer rules exclude all
credential- and device-protected data domains, including external app files. Vendor
transfer implementations can vary: do not promise absolute protection from privileged
OEM tools. The exclusions cover DataStore files and app-private Room inventory,
including package names, signing fingerprints, permissions and findings. Revisit
backup rules when storage scope changes.
There are no logs, uploads, APK payloads or custom cryptography. No MASVS compliance,
malware detection efficacy or performance improvement is claimed.

Tests cover capability and onboarding state, navigation, UI semantics, device facts,
and platform policy. Installed-app tests add scan races, timeouts and cancellation,
restricted inventories, signing evidence, persistence and large-list behavior. See
[validation](validation.md) for executed and pending checks.

## Installed-app scanner

`core:security/apps` separates immutable inventory from `AppRiskEvaluator` and the
serialized `AppScanner`. `app/scanner` owns PackageManager, package event observation,
Hilt wiring, presentation and settings adapters. No additional engine module is needed:
the current engine has a single Activity/ViewModel-owned lifecycle and JVM tests.
`core:database` provides the Room store; `feature:apps` only renders state/emits events.

Full scans refresh metadata. Incremental scans enumerate cheap package stamps
(version code, installation and update times), reuse unchanged labels/signing/source/
APK paths and refresh permission flags, which may change without an app update.
Failed optional metadata is explicit and retried on the next scan. Packages that vanish
or become inaccessible have no current assessment; old rows are not passed off as
fresh results. End-of-scan stamp reconciliation and a package-event generation check
retry once on observed races. Repeated churn, global collection/storage errors or
cancellation leave the previous completed transaction intact. PackageManager offers
no atomic system-wide snapshot; a package can still change after the final observation.
Snapshots carry completion time and profile/visibility limitations.

After a saved scan exists, opening/resuming Apps reconciles it. A dynamic receiver
observes add/replace/change/remove events while Apps is resumed and conflates pending
refresh requests. Events missed while stopped/process-dead are reconciled on the next
resume; nothing runs continuously. Manual scans are cancellable and Activity-owned,
so navigation/recreation need not restart them. Process death never persists a partial
scan. Room stores one JSON result per package plus scan metadata in one transaction;
there are no destructive migrations, main-thread queries or network calls. Export the
Room schema during builds and add explicit migrations for future schema versions.

Policy v1 flags target SDK below API 29, installer attribution outside the explicit
Google Play/Amazon/Samsung store list, Internet granted with at least two sensitive
permission categories, and declared special permissions or protected services. Seven-day install/update notices
are informational and reevaluated on every scan. Unknown installers are explicit;
store package names alone do not authenticate trust, and sideloading is not a malware
verdict. System apps receive the same rules. Verified signing lineage reports legitimate
rotation separately from changed saved fingerprints within the same observed
installation. Missing fingerprints/reinstallations never prove a signer change.
AppOps, active accessibility/device-admin/listener/VPN grants and other users/profiles
are not inferred from declarations. Every app states that vulnerability analysis has
not run. No APK contents or library CVEs are analyzed. See the
[API contracts](android-compatibility.md#installed-app-inventory).
