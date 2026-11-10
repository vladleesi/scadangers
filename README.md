# Scadangers

Scadangers is a native Android app that inventories visible installed apps and explains
security signals such as outdated target SDKs, sensitive permission grants and signing
changes. Scan results stay on the device.

Open **Apps → Full app scan** to start. Android package visibility and the current
user profile determine which apps can be inspected.

Build with JDK 17, Android SDK `platforms;android-37.0` and Build Tools `36.0.0`.

```sh
./gradlew :app:assembleDebug
```

Version `0.1.0` starts the release series. See [releasing](docs/release.md) for the
`develop` → `main` workflow and [Android compatibility](docs/android-compatibility.md)
for scanner coverage.
