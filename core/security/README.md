# Security boundary

Pure Kotlin capability policy and state transitions live in `capability/`, independent
of Android and Compose. `AccessRepository` serializes fresh platform observations,
request history and onboarding preferences through injected boundaries. Durable
history never substitutes for an OS grant. See the [API contracts](../../docs/android-compatibility.md#capability-contracts).

`apps/` supplies platform-independent inventory models, explainable metadata risk
rules, serialized/cancellable scan orchestration and source/store boundaries. The app
owns Android observations; the database module owns atomic Room persistence. No APK
parsing, network intelligence, library CVE detection or malware verdict is implemented.
See [scanner architecture](../../docs/architecture.md#installed-app-scanner).

`device/` supplies deterministic device-posture evaluation, strict patch dates and the
reviewed, incomplete offline ASB catalog. The app owns public Android observations;
no Google service, exploit probe or automatic remediation is required. Severity and
confidence are independent, and unknown applicability is explicit. See
[posture architecture](../../docs/architecture.md#device-posture-assessment).
