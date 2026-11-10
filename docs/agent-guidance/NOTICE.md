# Agent guidance attribution and placement

Source: https://github.com/vladleesi/agent-guidelines/tree/v1.8.0
Release: v1.8.0
Commit: 3951202798c0cb5604cc0f9c0c00412a894f3154
Selection: general, agent-workflow, testing, security, git, ci, release,
deployment, kotlin, compose, python.

The selected module text is reproduced verbatim in root AGENTS.md, applying only
to relevant operations/platforms throughout this project. Project requirements
precede the imported defaults. No imported bullets were removed, split or merged.
No local instruction file existed to rewrite. Partial overlaps are retained.
Backend, frontend and legacy delivery were excluded: there is no backend/browser
application, and focused delivery modules cover the relevant operations.

The release's Task-based loading recipe explicitly allows full inline placement
when dependable discovery cannot be established. Codex CLI 0.167.0 prompt-input
checks failed before producing a prompt with Operation not permitted, including
an attempt with temporary log/state paths. Full inline placement was chosen;
there are no deferred task files, eager task imports, or missing task routes.
The imported source was staged with its unmodified pinned synchronizer and its
--check passed in a temporary directory. Staging is separate from final placement.
Source blob/tree/commit object hashes were verified before staging.

Fetching, comparing with, or updating this collection requires an explicit user
request. Attribution links do not grant permission. Reading this local copy needs
no network, upstream checkout, script or import manifest.

## Verification and limitations

- Root AGENTS.md: 32,154 UTF-8 bytes. Imported Core bullet bodies: 11,681 bytes;
  imported Task bullet bodies: 12,520 bytes (body counts exclude titles/headings).
  All task guidance is inline, so deferred task files have zero bytes. The root
  remains below Codex's documented default 32 KiB discovered-instruction budget;
  additional discovered instructions would share that budget.
- Every complete selected module and bullet/continuation was compared with the
  pinned source. No imported or existing local rules were removed or deduplicated.
  No exact duplicate imported bullet blocks were found. Partial overlaps remain.
- Project overrides retain SDK 24/37/37, native Android modular MVVM boundaries,
  security/privacy and performance requirements, known validation limits, stage
  scope, build/test commands, and unsigned release behavior. Expected-error handling
  remains explicit despite optional Kotlin fallback advice. Synthetic examples
  remain restricted to tests/documentation, never real scan results. Documentation
  validation is explicitly scoped to instruction-only changes.
- Cross-cutting operations, dependency/CI work and indirect release/deployment
  effects have their full safeguards inline. No unresolved wording conflicts were
  identified; project requirements and existing user authorization take precedence.
- Explicit root-file reads succeeded from root, app/src/main, core/security, tools
  and .github/workflows. These establish file readability, not automatic startup
  discovery or model adherence. Both root and nested Codex prompt-input attempts
  failed with Operation not permitted. No other agent interface is configured here
  or claimed tested.
- A local .codex/config.toml root-marker adapter was attempted because the workspace
  has no Git metadata. Automatic approval review rejected writing that read-only
  configuration directory; no adapter was installed and global settings were not
  changed. Automatic root discovery from nested starts remains unverified. Start
  agents at the project root and explicitly supply/read AGENTS.md for nested starts
  until the harness's discovery is verified. Discovery limits cannot be solved by
  inline placement alone.
- git diff --check was attempted but cannot run normally without a Git repository.
  git diff --no-index --check against /dev/null checked each newly added file;
  neither produced whitespace diagnostics. Content hashes confirm all pre-existing
  non-generated project files were preserved. No app/build configuration changed.
- Temporary source/staging artifacts and verification scripts remain outside the
  project. No import manifests, managed markers or maintenance commands were added
  to the final instructions. Android builds/tests were not needed for this
  instruction-only addition and were not run.

## MIT notice (verbatim)

MIT License

Copyright (c) 2026 Vladislav Kochetov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
