# Releases

## CI

Pull requests to `develop` and pushes to `develop` run Android CI with read-only access:
debug and unsigned release builds, JVM tests, lint, KtLint, Detekt, the dependency audit,
and instrumentation on API 24 (AOSP, no Google services) and API 37.0. `Android checks`
runs only when verification and the whole instrumentation matrix succeed; any failure,
cancellation or skip also skips promotion and release. Pull requests cannot publish.

## Promotion and release

A successful push to `develop` fast-forwards `main` to the exact checked commit. If
`develop` has advanced, the run skips promotion. A divergent `main` is rejected by Git;
no force push or ruleset bypass is used.

The release job checks out `main` and continues only if its local and remote heads equal
the promoted SHA; otherwise it skips. Tags point to that SHA, and the release uses
`--target main --verify-tag`, so an unchecked tip is never tagged. These jobs live in
the CI workflow because a push made with `GITHUB_TOKEN` does not trigger another
workflow ([details](https://docs.github.com/en/actions/concepts/security/github_token#when-github_token-triggers-workflow-runs)).

`versionName` in `app/build.gradle.kts` is the release version (stable SemVer; current
`0.2.0` / versionCode 2). For a new release, set the version, increment `versionCode`
and add exactly one matching `## <version>` section, optionally followed by an em dash
and date, with notes in [CHANGELOG.md](../CHANGELOG.md).

Release notes are that changelog section; `tools/release_notes.py` rejects missing,
duplicate or empty sections. An existing published release only gets its description
updated from the changelog at its tag, which must be in `main` history; its tag, title
and assets are kept.

A conflicting unpublished tag, an existing draft, a malformed changelog or an API
failure stops publication. If a run fails after tagging, rerun while its commit is still
the `develop` and `main` tip; the existing tag is not moved.

The application ID `dev.vladleesi.scadangers` is a separate installation from earlier
package names; existing installs and app-private data are not migrated. The debug
suffix is `.debug`.

## Protecting main

Protection is a GitHub repository ruleset managed through the API; no ruleset file is
kept in the repository. Inspect it with an administrator's `gh` session:

```sh
gh api repos/vladleesi/scadangers/rulesets
gh api repos/vladleesi/scadangers/rules/branches/main
```

`Protect main` targets `refs/heads/main`, requires `Android checks` from GitHub Actions,
linear history and an up-to-date branch, blocks force pushes and deletion, and has no
bypass actors. Update it in place (`gh api --method PUT .../rulesets/<id>`) instead of
adding duplicate protection.

## Dependency reporting

Enable Issues, Dependency graph and Dependabot alerts, and turn off Dependabot security
updates. A weekly workflow keeps one tracking issue open while alerts need review and
links to the restricted Security tab; run it manually from Actions to refresh. Enable the
[Renovate app](https://github.com/apps/renovate); `renovate.json` extends the
[shared preset](https://github.com/vladleesi/renovate-config), holds updates in the
Dependency Dashboard and targets approved updates at `develop`.
