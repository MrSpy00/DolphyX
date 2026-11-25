# DolphyX Design System

**Source of truth:** `app/src/main/java/com/aegissoft/dolphyx/ui/theme/Theme.kt`, `ui/components/DolphyXComponents.kt`
**Reference:** original Dolphy app design language (monochrome OLED, capsule dock, pixel art)

## Color tokens

| Token | Value | Use |
| :--- | :--- | :--- |
| Background | `#0A0A0A` | App canvas |
| Surface | `#2A2A2A` | Cards, search bar, dock |
| Surface variant | `#3D3D3D` | Selected chip, secondary button |
| On background/surface | `#F4F4F4` | Primary text |
| On surface variant | `#9C9C9C` | Secondary text, passive icons |
| Outline | `#3D3D3D` | Single hairline when required |

Light theme: `#F3F3F0` background, `#FFFFFF` cards, `#141414` text, `#DCDCD8` outline. System bars follow the active theme through `WindowCompat`, so the status bar is never black on a light canvas.

Accents: `RAINBOW` (default monochrome white), `ORANGE`, `RED`, `GREEN`, `PURPLE`, `CYAN`,
and `DYNAMIC` (Material You on API 31+, silently falling back to Rainbow below that).
Accents only touch primary roles, selected controls and emphasis; the canvas, cards and text
stay monochrome so the Dolphy identity survives every palette.

## Typography

- Screen title: `headlineMedium`, bold, `#F4F4F4`.
- Section label: `labelLarge`, uppercase, `letterSpacing 1.2sp`, `#9C9C9C`.
- Body: `bodyMedium` / `bodyLarge`.
- Telemetry: monospace.
- Optional Flipper pixel font (`born2b_sporty_v2.ttf`) applies to titles only when the user enables it.
- UI scale: 85–135%, applied to `fontScale` only so layouts can reflow instead of shrinking.

## Components

| Component | Rule |
| :--- | :--- |
| `DolphyXCard` | `#2A2A2A`, 28dp radius, no border, 18dp padding |
| `DolphyScreenHeader` | 52dp back circle + large title (auto-size 18–26sp, 2 lines) + subtitle (3 lines) |
| `DolphyPrimaryPillButton` | Full width, 64dp height, white fill, black text, 999dp radius |
| `DolphySegmentedPill` | Full-width capsule, animated `onSurface 16%` selection, auto-sizing single-line labels, scrolls past four options |
| `DolphySearchBar` | Filled text field, capsule, no outline |
| `DolphyFloatingDock` | Capsule + three fixed-width slots (selected slot widens to 1.7 weight, hit target never shifts) + 64dp square FAB |
| `DolphyFilterDropdown` | Capsule field + menu; used by the IR browser so long category/brand/remote names stay readable |
| `DolphyToolRow` / `DolphyValueRow` / `DolphySwitchRow` | Borderless rows, title 1 line, subtitle max 3 lines |
| `DolphyPixelBanner` | `px_scene.png` / `px_dolphin.png`, nearest-neighbor scaling |

Spacing: 20dp screen edge, 10–12dp between cards, 18dp card padding, 24dp between sections. Dock respects `navigationBarsPadding()`.

## Rules

1. Edge-to-edge with `safeDrawingPadding()`; content never under the status bar or notch.
2. Zero emoji; use `Icons.Rounded.*` or Canvas/SVG.
3. No clipped text: `FlowRow` for chips, subtitle `maxLines = 3`, titles `maxLines = 2` with ellipsis.
4. Single border per card; no nested outlined cards. Interactive surfaces use Material `Surface`, never raw `.clickable`, so the ripple follows the shape instead of drawing a rectangle.
5. Touch targets ≥48dp; destructive actions need confirmation/feedback.
6. Every card shows real hardware/network data; no fake metrics.
7. Loading, empty, error, and permission-denied states are explicit.
8. All UI strings live in `values/` and `values-tr/`.
9. Motion: 180–300ms with `FastOutSlowInEasing`. Tab switches scale + fade, detail pushes slide, pops reverse. Reduce motion disables all of it.
10. Pixel images use `FilterQuality.None`.

## Implementation status

Monochrome tokens, light/dark/system themes, six accents + Material You, edge-to-edge system
bars, pixel font toggle, UI scale, reduced motion, 28dp cards, white pills, fixed-slot capsule
dock, animated segments and dropdowns, Functions Containers/Tiles + Standard/Compact, and
Aseprite pixel assets are implemented across all 14 screens.
