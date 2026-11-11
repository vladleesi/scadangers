# Releases

Pull requests to `develop` and pushes to `develop` start Android CI. GitHub Actions builds the debug and unsigned
release APKs, runs JVM tests, lint, formatting and Detekt, audits runtime dependencies,
and runs
instrumentation on API 24 (AOSP, without Google services) and 37.0. Checks run with read access.
SDK setup explicitly installs `platform-tools`; the deprecated `tools` package is no
longer available. Pull-request checks cannot publish or promote a release.
`Android checks` runs only after both verification and the entire instrumentation
matrix succeed. An upstream failure, cancellation or skipped job skips this aggregate
job and its dependent promotion/release jobs.

A successful push to `develop` in `vladleesi/scadangers` automatically fast-forwards
`master` to the exact checked commit. A stale run whose commit is no longer the
`develop` tip skips promotion. A divergent `master` is rejected by Git; no force push
or protection bypass is used. The existing `master` ruleset requires `Android checks`
from GitHub Actions, linear history and an up-to-date branch.

The separate release job checks out `master`, then verifies that its local and remote
heads equal the promoted, checked SHA. If `master` has advanced, the job skips release
creation. Tags use only that verified master SHA. GitHub release creation specifies
`--target master --verify-tag`; it never tags a fresh unchecked branch tip.
These dependent jobs share the CI workflow because a branch push made with
`GITHUB_TOKEN` does not start another push-triggered workflow. No extra credential is
needed; see [GitHub token event behavior](https://docs.github.com/en/actions/concepts/security/github_token#when-github_token-triggers-workflow-runs).

`versionName` in `app/build.gradle.kts` is the release version. The series starts at
`0.1.0` / versionCode 1; the current version is `0.2.0` / 2. Set the next stable SemVer
and increment `versionCode` when preparing a new release. Include exactly one matching
`## <version>` section in [CHANGELOG.md](../CHANGELOG.md), optionally followed by an
em dash and date, with nonempty release notes.

Release descriptions come from that version's changelog section, preserving its
Markdown and excluding other versions. `tools/release_notes.py` rejects missing,
duplicate or empty sections. New releases read the verified master commit's changelog.
For an existing published release, the job updates only its description using the
changelog at the existing tag. The tag must belong to master history; its SHA, title
and assets are preserved. Further passing changes at the same published version can
advance `master` while the release continues to describe its tagged source.

A conflicting unpublished tag, an existing draft, a malformed changelog or a release
metadata/API failure stops publication. If a run fails after creating a tag, rerun
while its commit is still the `develop` and `master` tip; the job can finish the release
without moving the existing tag. Promotion and release require the Actions token's
write access under the repository rules.

The current `dev.vladleesi.scadangers` application ID starts a separate Android installation
and app-private inventory from earlier package names. Existing installs are not upgraded
in place and their app-private data is not automatically migrated. The debug suffix stays
`.debug`. See [changelog](../CHANGELOG.md) for version history.

## Protecting master

Manage protection through GitHub's repository ruleset API. The live `Protect master`
ruleset is authoritative; do not apply the legacy JSON example from the repository.
Inspect it using the repository administrator's authenticated GitHub CLI session:

```sh
gh api repos/vladleesi/scadangers/rulesets
gh api repos/vladleesi/scadangers/rules/branches/master
```

The active rule targets `refs/heads/master`, requires `Android checks` from GitHub
Actions on the promoted commit, linear history and an up-to-date branch. It blocks
force pushes and branch deletion with an empty bypass list. Update that existing
ruleset through the API rather than creating duplicate protection or a bypass.

## Dependency reporting

Enable Issues, Dependency graph and Dependabot alerts in repository settings, and
turn off Dependabot security updates. The repository has no Dependabot version
update schedule. The weekly Dependabot reporting workflow keeps one tracking issue
open while alerts need review. It links to the restricted Security tab and keeps
advisory details there. Run the workflow manually from Actions to refresh the issue.

Enable the [Renovate GitHub app](https://github.com/apps/renovate) for this repository.
`renovate.json` extends the [shared preset](https://github.com/vladleesi/renovate-config).
Its Dependency Dashboard holds updates for approval. Approved updates target `develop`.
