# Scadangers

Scadangers is a native Android app that inventories visible installed apps and explains
security signals such as outdated target SDKs, sensitive permission grants and signing
changes. Device posture checks explain observable platform settings and selected Android
Security Bulletin matches, including uncertainty. Results stay on the device.

Open **Apps → Full app scan** to start. Android package visibility and the current
user profile determine which apps can be inspected. Open **Overview → This device → Details**
for current device posture, patch information, evidence, limitations and system settings.
The offline bulletin catalog is incomplete; a missing finding does not establish safety.

Build with JDK 17, Android SDK `platforms;android-37.0` and Build Tools `36.0.0`.

```sh
./gradlew :app:assembleDebug
```

Current version: `0.2.0` (Android versionCode 2). See [releasing](docs/release.md) for the
`develop` → `main` workflow and [Android compatibility](docs/android-compatibility.md)
for scanner coverage.

See [the changelog](CHANGELOG.md) for changes and [assessment architecture](docs/architecture.md#device-posture-assessment) for scope.
