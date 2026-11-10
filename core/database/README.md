# Database

Version 1 Room database for completed installed-app scans. Each package has a serialized
metadata/finding row; a separate completion row records time, API scope and inaccessible
packages. Replacement is transactional, including removals. Errors and cancellation do
not intentionally publish partial results. The app supplies one lazy database builder;
no storage opens from Application startup. No main-thread queries or destructive
migration fallback. Build-time KSP exports schemas to `schemas/` for future migrations.
See [architecture](../../docs/architecture.md#installed-app-scanner).
