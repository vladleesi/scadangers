# Design system

`core:designsystem` owns Scadangers's visual language: tokens in `theme/`, window size
classes in `adaptive/`, and reusable components in `component/`. Screens use these
instead of inline colors, sizes or Material defaults.

The direction is modern, minimal and technical: warm neutral surfaces, thin borders,
almost no shadows, subtle corners, a strong type hierarchy and a restrained orange
accent. It takes cues from Cloudflare's website and security dashboard (warm neutrals,
1 px borders at low contrast, 3–8 px radii, findings ranked Critical / Moderate / Low)
without copying its assets, typeface or layout. Launcher and onboarding branding use
assets supplied for Scadangers in `android-icons.zip`. The app resources are under
`app/src/main/res/`. The original package, editable SVGs, and previews can be kept
in a local `artwork/` folder outside version control.

## Brand icon resources

Installed launcher PNGs and vector layers match the supplied bytes. `android:icon`
and `android:roundIcon` refer to mipmap resources:

- API 24–25 use separate supplied standard and genuinely circular round PNGs, in
  48/72/96/144/192 px sizes (mdpi through xxxhdpi). There is no round-icon alias.
- API 26–32 use the supplied 108 dp foreground/background vectors. The package's redundant `drawable-v24` qualifier is
  normalized to `drawable`, since the application minimum is already API 24.
  Foreground scale and translation come directly from the package. The full radar and
  focus mark fit inside the central 66 dp safe circle, including antialiased strokes.
- API 33+ definitions add the supplied monochrome vector. It includes translucent radar
  rings and decorative full-bleed clouds; this decoration intentionally extends outside
  the safe circle. The primary radar/focus features share the color foreground's placement.
  System launchers control mask/tint and whether themed icons are displayed.
- The provided white/transparent `drawable/ic_stat_scan.xml` is retained in minified
  release resources using the narrowly scoped `raw/dev_scadangers_icons_keep.xml`. It is
  an asset only: no notification posting, channels or monitoring implementation is added.
  The vector works throughout the supported API range.
- Onboarding uses the new package's 512 px full-bleed store image as
  `drawable-nodpi/ic_brand.png`. Existing off-Main, sampled decoding bounds it to 96 dp.

The supplied adaptive XML is split into v26 (background/foreground) and v33 (also
monochrome) variants. No layer geometry is redrawn. The clipping regression test keeps
its strict safe-circle check for the color foreground. A second test verifies that the
monochrome layer retains opaque primary features at the same positions, allowing its
intentional ring-alpha differences and cloud decoration.

The artwork was supplied explicitly for this app. No additional license or authorship
claim is made.

References: [adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive),
[manifest icon attributes](https://developer.android.com/guide/topics/manifest/application-element),
[bounded bitmap loading](https://developer.android.com/topic/performance/graphics/load-bitmap),
[retaining supplied resources](https://developer.android.com/topic/performance/app-optimization/customize-which-resources-to-keep).

## Color semantics

Every role has one meaning. Pick a role by meaning, never by hue.

| Role | Meaning | Never use for |
| --- | --- | --- |
| `accent` (orange) | Brand, primary actions, selection, focus | Any status or warning |
| `positive` (green) | Protection verified active; a completed check that passed | Defaults, placeholders, "nothing found" without an assessment |
| `caution` (amber) | Moderate severity; needs the user's attention | Brand or emphasis |
| `critical` (red) | Critical severity; failed protection or assessment | Destructive styling without risk |
| `info` (blue) | Low severity; informational findings | Links or actions |
| `neutral` (gray) | Inactive, unavailable, not assessed, unknown | — |

`SecurityStatus.Active` is the only status mapped to `positive`. `Unavailable` and
`Inactive` are neutral, so an unimplemented feature can never look healthy. Status and
severity always show an icon or written label and announce "Status: …" or
"Severity: …" to screen readers, so meaning never depends on color alone.

Each palette is designed for its theme rather than inverted. `ColorContrastTest` asserts
WCAG AA for every text and status pair in both themes (4.5:1 for text, 3:1 for control
boundaries and status graphics). Change a token only with that test passing.

## Typography

The platform sans-serif (Roboto on most devices) was chosen over bundling Inter:

- Readability for UI text is comparable at these sizes, and Roboto ships with the OS
  under the Apache License 2.0.
- It covers every script the device supports and follows the user's font size and
  bold-text settings.
- It adds no APK size and no font loading. Bundled resource fonts are read from disk
  on first use, which this project avoids on the main thread.

Inter's main advantage, tabular figures for metrics, is covered with the `tnum`
font feature on `ScadangersTypography.metric`. Technical values (patch levels, later
package names and addresses) use the platform monospace in `ScadangersTypography.mono`.
Weights are limited to Regular, Medium and SemiBold. All sizes are in `sp`.

## Tokens

| Token | Location | Notes |
| --- | --- | --- |
| Color | `ScadangersTheme.colors` | Light and dark palettes, mapped onto Material roles |
| Typography | `MaterialTheme.typography`, `ScadangersTheme.typography` | Material roles plus `metric` and `mono` |
| Spacing | `ScadangersSpacing` | 4 dp scale; gutters 16 dp compact, 24 dp wider |
| Shape | `ScadangersShapes` | 4 / 6 / 8 / 12 dp and pill |
| Elevation | `ScadangersElevation` | Flat content with 1 dp borders; only overlays are raised |
| Iconography | `ScadangersIcons` | One filled 24 dp set; sizes 16 / 20 / 24 / 32 dp |
| Dimensions | `ScadangersDimensions` | 48 dp touch targets, 56 dp rows, content width limits |

Icons are Material Icons path data (Apache License 2.0, Google), declared as lazily
built `ImageVector`s so no resource parsing happens on the main thread.

## Components

| Component | Use |
| --- | --- |
| `SecurityStatusIndicator` | State of protection or a module |
| `SeverityLabel` | Critical, Moderate or Low finding |
| `FindingRow` | One finding or event; use as a lazy item keyed by `FindingUiModel.id` |
| `MetricsCard` | One metric; `MetricValue.Unavailable` renders a dash, never `0` |
| `ScanProgress` | Determinate or indeterminate scan progress with optional cancel |
| `EmptyState`, `LoadingState`, `ErrorState`, `PermissionRequiredState` | Explicit non-content states |
| `CompactInfoTable` | Small fixed sets of label/value facts |
| `ConfirmationDialog` | Consequential actions; `destructive` uses the critical role |
| `SettingsItem`, `SettingsToggleItem` | Settings rows; the whole toggle row is one switch |
| `SectionCard`, `ScadangersCard`, `CardContent` | Bordered containers |
| `ScreenLazyColumn` | Scrollable screen body with adaptive gutters |

Each component has light/dark previews built with `ScadangersPreviewSurface`. Preview and
test data must be clearly synthetic and never resemble a real finding.

## Honest states

Until an engine exists, its UI shows an explicit unavailable state. An empty list says
whether anything was assessed or monitored; it must never imply the device is safe.
"Protection is active" requires `SecurityStatus.Active`, which only a running,
verified module may report. A failed assessment is shown with `ErrorState`, never as
an empty or clean result.

## Accessibility and motion

- Touch targets are at least 48 dp; list and settings rows are at least 56 dp tall.
- Headings carry heading semantics; rows merge into single announcements.
- Layouts wrap rather than clip at large font scales. UI tests cover 200 % scale.
- With the system "Remove animations" setting on, indeterminate progress becomes
  static (still announced as in progress) and navigation transitions are removed.
  The app re-reads the setting on every resume. There are no decorative animations.

## Adaptive layout

`ProvideWindowWidthClass` classifies the window's available width with Material's
breakpoints (compact under 600 dp, medium under 840 dp, expanded otherwise). Compact
windows use a bottom navigation bar; wider windows, including unfolded foldables and
tablets, use a navigation rail. The overview switches from one to two columns, and
content is limited to a maximum width. Layout follows window size, never device type.
Foldable posture (tabletop, book) is not handled specifically.
