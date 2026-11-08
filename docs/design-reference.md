# Tessera design reference

Figma source of truth: https://www.figma.com/design/IGoHRIPtbgqFHT3RhpQ24a

- `16:22` — Home / living wallpaper
- `16:68` — App drawer / morph state
- `16:131` — Search / portal state
- `18:2` — Typography direction specimen

## 0.4.1 — Intelligent Workspace / Refined Aurora

The visual language is a quiet, premium launcher surface rather than a collection of outlined cards.

- Canvas: wallpaper-first black/ink base with deep-pine panels.
- Surface hierarchy: near-black shell, raised pine icon tiles, subdued borders and restrained elevation.
- Accents: warm ivory for primary content, antique brass for actions, soft mint for active/secondary cues.
- Typography: sans-serif UI hierarchy; selected display phrases may use a more expressive face, never at the expense of app labels.
- Workspace: compact masthead, concise headline, clear space label, low-contrast grid container, icon-forward tiles and a minimal dock.
- App library: opaque bottom sheet with a handle, compact heading, installed count, search field and legible app rows. Wallpaper content must not bleed through app labels.

## Motion language

Motion is expressive but brief and purposeful. It should make the interface feel spatial without making app launching slower.

- The ambient background uses broad, translucent gradients drawn across the full canvas. Avoid clipped radial circles, banding or high-contrast rings.\n- A small hero star loops through a slow, reversing orbit and stops when Reduce Motion is enabled.\n- Workspace columns and dock icons enter with a subtle upward movement, scale settle and stagger.
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
