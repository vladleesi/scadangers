# Releases

Pull requests to `develop` and pushes to `develop` start Android CI. GitHub Actions builds the debug and unsigned
release APKs, runs JVM tests, lint, formatting and Detekt, audits runtime dependencies,
and runs
instrumentation on API 24 (AOSP, without Google services) and 37.0. Checks run with read access.
SDK setup explicitly installs `platform-tools`; the deprecated `tools` package is no
longer available. Pull-request checks cannot publish or promote a release.

A push to `develop` in `vladleesi/scadangers` can promote the checked commit to `main`
after every job succeeds. Git rejects a non-fast-forward update. The publish job skips
a run when its commit is no longer the `develop` tip. It uses the repository's Actions
token and requires write access to `main` and release tags under the repository rules.

`versionName` in `app/build.gradle.kts` is the release version. The series starts at
`0.1.0` with Android `versionCode` 1; the current version is `0.2.0` / 2.
Set the next SemVer and increment `versionCode`
when preparing a new release. After promotion, the workflow creates `v<version>` at
the checked commit and publishes a GitHub source release. Further passing changes
at an already published version can advance `main` while preserving its release tag.
If a run fails after tagging, rerun it while that commit is still the `develop` tip.

The current `dev.vladleesi.scadangers` application ID starts a separate Android installation
and app-private inventory from earlier package names. Existing installs are not upgraded
in place and their app-private data is not automatically migrated. The debug suffix stays
`.debug`. See [changelog](../CHANGELOG.md) for version history.

## Protecting main

Import [the main ruleset](../.github/rulesets/main.json) in GitHub under Settings →
Rules → Rulesets, or apply it with a repository administrator's GitHub CLI session.

```sh
gh api --method POST repos/vladleesi/scadangers/rulesets --input .github/rulesets/main.json
```

The rule requires `Android checks` from GitHub Actions on the promoted commit,
linear history, and an up-to-date branch. It blocks force pushes and branch deletion
with an empty bypass list. The checked commit can fast-forward to `main`.

## Dependency reporting

Enable Issues, Dependency graph and Dependabot alerts in repository settings, and
turn off Dependabot security updates. The repository has no Dependabot version
update schedule. The weekly Dependabot reporting workflow keeps one tracking issue
open while alerts need review. It links to the restricted Security tab and keeps
advisory details there. Run the workflow manually from Actions to refresh the issue.

Enable the [Renovate GitHub app](https://github.com/apps/renovate) for this repository.
`renovate.json` extends the [shared preset](https://github.com/vladleesi/renovate-config).
Its Dependency Dashboard holds updates for approval. Approved updates target `develop`.
