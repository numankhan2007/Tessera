# Tessera design reference

Figma source of truth: https://www.figma.com/design/IGoHRIPtbgqFHT3RhpQ24a

- `16:22` — Home / living wallpaper
- `16:68` — App drawer / morph state
- `16:131` — Search / portal state
- `18:2` — Typography direction specimen

## 0.4.4 — Daily Canvas / Live Clock Hero

- Home masthead: TESSERA wordmark at left, compact localized date at right, workspace options in a 44 dp circular action.
- Hero: a 8.5 sp uppercase daypart greeting above a 48 sp sans-serif-light clock, with the existing small gold star emblem as the visual counterweight.
- Clock formatting follows Android's global 12/24-hour preference and updates every minute; the greeting changes across morning, afternoon, evening and night.
- First-run Home-role action is a centered 224 × 34 dp pill, not a full-width banner.
- Workspace item count uses concise, non-tracked typography with no letter-spacing that would clip on narrow displays.
- Keep the system wallpaper fully unobstructed; do not reintroduce full-screen radial gradients.

## 0.4.3 — Kinetic Home / Adaptive Workspace

- Workspace grids are content-sized from occupied rows and widget spans instead of drawing a large empty six-row panel.
- Keep the system wallpaper unobstructed; avoid full-screen radial gradients because low-bit-depth surfaces can show visible contour banding. Eight extremely faint motes provide just enough ambient movement.
- Eight faint motes and restrained per-icon drift add motion at the home-screen level.
- The hero star uses a continuous reversing orbit with a small breathing pulse.
- Ambient updates are paced at roughly 40 fps using delayed frame requests, stop when the launcher window is hidden, and respect both Tessera's Reduce Motion setting and Android's global animator scale.
- The app-library sheet now enters with a controlled deceleration and less overshoot.

## 0.4.2 — Intelligent Workspace / Minimal Atmosphere

The visual language is a quiet, premium launcher surface rather than a collection of outlined cards.

- Canvas: wallpaper-first black/ink base with deep-pine panels.
- Surface hierarchy: near-black shell, raised pine icon tiles, subdued borders and restrained elevation.
- Accents: warm ivory for primary content, antique brass for actions, soft mint for active/secondary cues.
- Typography: sans-serif UI hierarchy; selected display phrases may use a more expressive face, never at the expense of app labels.
- Workspace: compact masthead, concise headline, clear space label, low-contrast grid container, icon-forward tiles and a minimal dock.
- App library: opaque bottom sheet with a handle, compact heading, installed count, search field and legible app rows. Wallpaper content must not bleed through app labels.

## Motion language

Motion is expressive but brief and purposeful. It should make the interface feel spatial without making app launching slower.

- The ambient background uses a single low-opacity diagonal color wash across the full canvas. Avoid clipped radial circles, banding or high-contrast rings.\n- Five very faint motes drift almost imperceptibly; a small hero star loops through a slow, reversing orbit and stops when Reduce Motion is enabled.\n- Workspace columns and dock icons enter with a subtle upward movement, scale settle and stagger.
- The app library rises from the bottom with a fade and a decelerating curve.
- App rows arrive in a short stagger; filtering itself remains immediate to preserve search responsiveness.
- App tiles use a short press response and platform ripple feedback.
- Page changes update the page counter with a restrained crossfade.
- Reduce Motion disables custom entrance and press animations while preserving functional state changes.

## Intelligent app library

- Quick access is derived from apps the user explicitly pinned to the dock. No usage-history or accessibility permission is requested.
- Categories use Android's declared application category metadata, keeping classification grounded in package metadata rather than guessed from app names.
- Search filters app labels immediately and combines with the active category.
- Empty search/category results have an explicit, restrained empty state.

## Visual regression

- CI launches the native HOME activity on an Android 15 emulator and renders both the workspace and app-library view hierarchies into PNGs. JUnit and both PNG files are required for the visual regression step to pass.
- PNGs are uploaded alongside JUnit XML reports for inspection at the actual emulator viewport.
- Screenshot review should check status/navigation insets, clipping, app-label legibility, dock spacing, drawer hierarchy, ripple hit areas and wallpaper contrast.

## Responsive and accessibility requirements

- Translate design frames using Android system insets, display density, font scale, display cutouts and device aspect ratio.
- Keep all app names readable at common display sizes and support Android's font scaling.
- Preserve content descriptions for app, drawer, page and action controls.
- Retain Android's native app launch, shortcut, widget and drag/drop behavior.
