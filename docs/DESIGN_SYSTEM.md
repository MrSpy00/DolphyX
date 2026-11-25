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

Light theme: `#F4F2EE` background, `#FFFFFF` cards, `#1A1A1A` text.

Accents: `RAINBOW` (default monochrome white), `ORANGE #FF6600`, `RED #E5484D`, `GREEN #3DD68C`, `PURPLE #B48CFF`. Accents are used only for selected controls and emphasis; the base UI stays monochrome.

## Typography

- Screen title: `headlineMedium`, bold, `#F4F4F4`.
- Section label: `labelLarge`, uppercase, `letterSpacing 1.2sp`, `#9C9C9C`.
- Body: `bodyMedium` / `bodyLarge`.
- Telemetry: monospace.
- Optional Flipper pixel font (`born2b_sporty_v2.ttf`) applies to titles only when the user enables it.
- UI scale: 85–135% through `LocalDolphyScale` + density.

## Components

| Component | Rule |
| :--- | :--- |
| `DolphyXCard` | `#2A2A2A`, 28dp radius, no border, 18dp padding |
| `DolphyScreenHeader` | 40dp back circle + large title + subtitle |
| `DolphyPrimaryPillButton` | Full width, 64dp height, white fill, black text, 999dp radius |
| `DolphySegmentedPill` | Full-width capsule, selected segment `onSurface 16%` |
| `DolphySearchBar` | Filled text field, capsule, no outline |
| `DolphyFloatingDock` | Capsule + expanding active tab + 56dp square FAB |
| `DolphyToolRow` / `DolphyValueRow` / `DolphySwitchRow` | Borderless rows, title 1 line, subtitle max 3 lines |
| `DolphyPixelBanner` | `px_scene.png` / `px_dolphin.png`, nearest-neighbor scaling |

Spacing: 20dp screen edge, 10–12dp between cards, 18dp card padding, 24dp between sections. Dock respects `navigationBarsPadding()`.

## Rules

1. Edge-to-edge with `safeDrawingPadding()`; content never under the status bar or notch.
2. Zero emoji; use `Icons.Rounded.*` or Canvas/SVG.
3. No clipped text: `FlowRow` for chips, subtitle `maxLines = 3`, titles `maxLines = 2` with ellipsis.
4. Single border per card; no nested outlined cards.
5. Touch targets ≥48dp; destructive actions need confirmation/feedback.
6. Every card shows real hardware/network data; no fake metrics.
7. Loading, empty, error, and permission-denied states are explicit.
8. All UI strings live in `values/` and `values-tr/`.
9. Motion: 180–260ms, reduced-motion setting, animations stop in performance mode.
10. Pixel images use `FilterQuality.None`.

## Implementation status

Monochrome tokens, 5 accents, pixel font toggle, UI scale, reduced motion, 28dp cards, white pills, capsule dock, Functions Containers/Tiles + Standard/Compact, and pixel assets are implemented across all 14 screens.
