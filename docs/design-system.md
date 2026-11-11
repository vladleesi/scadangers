# Design system

`core:designsystem` owns the visual language: tokens in `theme/`, window size classes in
`adaptive/` and components in `component/`. Screens use these instead of inline colors,
sizes or Material defaults.

The direction is minimal and technical: warm neutral surfaces, thin low-contrast
borders, almost no shadows, 3–8 px corners, strong type hierarchy and a restrained
orange accent, inspired by Cloudflare's dashboard without copying its assets, typeface
or layout.

## Brand icon resources

The launcher and onboarding artwork was supplied for this app (`android-icons.zip`);
no other license or authorship claim is made. The original package can be kept in a
local `artwork/` folder outside version control. Installed PNGs and vectors match the
supplied bytes.

- API 24–25: separate standard and circular round PNGs, mdpi–xxxhdpi.
- API 26–32: supplied 108 dp adaptive foreground/background vectors. The radar mark fits
  inside the 66 dp safe circle. The package's `drawable-v24` is normalized to `drawable`.
- API 33+: adds the supplied monochrome vector. Its cloud decoration intentionally
  extends beyond the safe circle; launchers control tinting and whether themed icons show.
- `drawable/ic_stat_scan.xml` is kept in release builds by `raw/dev_scadangers_icons_keep.xml`.
  It is an asset only; no notifications are implemented.
- Onboarding uses a 512 px `drawable-nodpi/ic_brand.png`, decoded off Main with sampling
  bounded to 96 dp.

Tests check the color foreground against the safe circle and that the monochrome layer
keeps its primary features in the same positions.

## Color semantics

Pick a role by meaning, never by hue.

| Role | Meaning | Never use for |
| --- | --- | --- |
| `accent` (orange) | Brand, primary actions, selection, focus | Status or warnings |
| `positive` (green) | Protection verified active; a passed check | Defaults, placeholders, "nothing found" without an assessment |
| `caution` (amber) | Moderate severity | Brand or emphasis |
| `critical` (red) | Critical severity; failed protection or assessment | Destructive styling without risk |
| `info` (blue) | Low severity | Links or actions |
| `neutral` (gray) | Inactive, unavailable, not assessed, unknown | — |

Only `SecurityStatus.Active` maps to `positive`; `Unavailable` and `Inactive` are
neutral, so an unimplemented feature never looks healthy. Status and severity always
show an icon or label and are announced to screen readers. `ColorContrastTest` asserts
WCAG AA (4.5:1 text, 3:1 controls and graphics) in both themes; change a token only
with it passing.

## Typography and tokens

Platform sans-serif (Roboto) is used instead of a bundled font: no APK size, no font
loading on Main, full script coverage and respect for user font settings. Tabular figures
use the `tnum` feature on `ScadangersTypography.metric`; technical values use
`ScadangersTypography.mono`. Weights are Regular, Medium and SemiBold; sizes are in `sp`.

| Token | Location | Notes |
| --- | --- | --- |
| Color | `ScadangersTheme.colors` | Light and dark palettes mapped to Material roles |
| Typography | `MaterialTheme.typography`, `ScadangersTheme.typography` | Material roles plus `metric` and `mono` |
| Spacing | `ScadangersSpacing` | 4 dp scale; gutters 16 dp compact, 24 dp wider |
| Shape | `ScadangersShapes` | 4 / 6 / 8 / 12 dp and pill |
| Elevation | `ScadangersElevation` | Flat with 1 dp borders; only overlays are raised |
| Icons | `ScadangersIcons` | Filled 24 dp set (Material Icons, Apache 2.0) as lazy `ImageVector`s |
| Dimensions | `ScadangersDimensions` | 48 dp targets, 56 dp rows, content width limits |

## Components

| Component | Use |
| --- | --- |
| `SecurityStatusIndicator`, `SeverityLabel` | Protection state; Critical / Moderate / Low |
| `FindingRow` | One finding; lazy item keyed by `FindingUiModel.id` |
| `MetricsCard` | One metric; `MetricValue.Unavailable` shows a dash, never `0` |
| `ScanProgress` | Determinate or indeterminate progress with optional cancel |
| `EmptyState`, `LoadingState`, `ErrorState`, `PermissionRequiredState` | Non-content states |
| `CompactInfoTable` | Small fixed label/value sets |
| `ConfirmationDialog` | Consequential actions; `destructive` uses `critical` |
| `SettingsItem`, `SettingsToggleItem` | Settings rows; the whole toggle row is one switch |
| `SectionCard`, `ScadangersCard`, `CardContent` | Bordered containers |
| `ScreenLazyColumn` | Scrollable body with adaptive gutters |

Components have light/dark previews via `ScadangersPreviewSurface`. Preview and test
data must be clearly synthetic and never resemble a real finding.

## Honest states

Until an engine exists its UI shows an explicit unavailable state. An empty list says
whether anything was assessed and never implies the device is safe. "Protection is
active" requires `SecurityStatus.Active` from a running, verified module. A failed
assessment uses `ErrorState`, never an empty or clean result.

## Accessibility and layout

- Touch targets are at least 48 dp and rows at least 56 dp. Headings carry heading
  semantics; rows merge into one announcement. Layouts wrap at large font scales (UI
  tests cover 200 %).
- With "Remove animations" on, indeterminate progress is static (still announced) and
  navigation transitions are removed. The setting is re-read on resume.
- `ProvideWindowWidthClass` uses Material breakpoints (compact < 600 dp, medium < 840 dp).
  Compact windows get a bottom bar, wider ones a rail, with two overview columns and a
  maximum content width. Layout follows window size, not device type; foldable postures
  are not handled specifically.
