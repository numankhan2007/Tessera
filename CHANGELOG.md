# Changelog

## 0.4.3 — Kinetic home / adaptive workspace

- Removed the oversized empty workspace panel; the app grid now measures its content rows and widget spans while preserving the full six-row placement model.
- Replaced the flat ambient wash with three low-contrast, drifting emerald/teal/brass bloom fields and eight faint atmospheric motes.
- Increased motion smoothness by syncing ambient redraws to display frames and adding gentle per-icon drift.
- Added a breathing pulse to the slow reversing hero-star orbit; all custom movement stops when Reduce Motion is enabled or the launcher window is hidden.
- Refined the app-library sheet entrance to use a controlled deceleration instead of a pronounced overshoot.
- Preserved native app discovery, workspace persistence, folders, dock, shortcuts, widgets, and backup/restore behavior.

## 0.4.2 — Atmosphere refinement / CI visual gate

- Replaced radial ambient lighting with a restrained diagonal color wash to remove visible contour bands from the wallpaper-first canvas.
- Kept only five very low-opacity moving motes so the workspace, app icons and wallpaper remain the focus.
- Made the hero-star orbit continuously reverse and cancel cleanly when detached or when Reduce Motion is enabled.
- Reworked the Android 15 workflow checks to avoid shell variables across separate emulator-runner command invocations.
- CI now requires 12 passed, zero failed/skipped JUnit tests and two exported screenshot files.

## 0.4.1 — Refined aurora / motion polish

- Replaced clipped radial circles with low-contrast, full-canvas aurora gradients to remove visible rings and hard edges.
- Reduced ambient glow and mote intensity so app icons and labels remain the visual focus.
- Added a continuously reversing star-orbit animation that cancels when detached and respects Reduce Motion.
- Shortened workspace page count text to prevent clipping on narrow phone screens.
- Hardened Android 15 CI acceptance: 12/12 JUnit tests and both non-empty home/app-library screenshots are required for a successful run.

## 0.4.0 — Intelligent workspace and kinetic atmosphere

- Added a low-energy animated aurora atmosphere with floating motes; it stops when the view is detached and respects Reduce Motion.
- Refined the home hierarchy, typography, workspace density and icon proportions for phone-sized screens.
- Added spring-like workspace entrances, tile press response and subtle horizontal page parallax.
- Added app-library quick access based only on the user's own dock favourites.
- Added native app-category filters sourced from Android application metadata: Social, Work, Media, Games and Other.
- Added alphabetical letter dividers, a cleaner empty state and improved app-library presentation.
- Added Android 15 visual-regression captures for the home screen and app library; CI uploads the actual emulator screenshots.

## 0.3.0 — Obsidian Garden visual redesign

- Rebuilt the home hierarchy with a compact brand/date bar, clear headline, understated workspace sections and a calmer dock.
- Replaced the heavy green tiles with refined deep-pine icon surfaces, subtle borders, rounded geometry and a restrained ivory/brass/mint palette.
- Reworked the app library as an opaque, bottom-anchored sheet so wallpaper text no longer competes with app names.
- Added staggered workspace and dock entrances, a lifting/fading app-library transition, staggered app rows, ripple feedback and a restrained app-press response.
- Applied sans-serif as the application-wide fallback while keeping expressive typography in selected display elements.
- Preserved native app discovery, launching, folders, drag/reposition, widgets, shortcuts, backup and restore behavior.

## 0.2.6

- Prevent widget drag/drop from overlapping any occupied cells across its full grid span.
- Place new widgets on the currently visible page, creating and navigating to a new page when necessary.
- Detect duplicate app and shortcut pins across all pages, the dock, and folder contents.
- Make app-catalogue refreshes generation-safe so stale background scans cannot overwrite newer results.
- Isolate inaccessible work profiles so one profile cannot suppress apps from other profiles.
- Added regression coverage for duplicate detection across dock and folders.
- Promote the last remaining folder child back to a normal app/shortcut item when its sibling app is uninstalled.

## 0.2.5

- Persist the last selected workspace page across Activity recreation and app restarts.
- Pin new apps and shortcuts onto the page currently being viewed.
- Automatically navigate to a newly created page when the current page is full.
- Removed duplicate app-catalogue refresh during initial Activity startup.

## 0.2.4

- Added a stable, test-only debug signing key so later CI APKs can update the installed test build in place.
- Added a CI certificate-fingerprint assertion to the three-pass APK audit.
- Documented the one-time migration from older temporary CI debug signatures.

## 0.2.3

- Validate and commit workspace restores before removing any existing widget bindings.
- Preserve the current workspace when an imported JSON backup is invalid or unsupported.
- Release stale Android widget IDs when their provider can no longer be resolved.
- Added instrumentation regression tests for malformed and unsupported backups.

## 0.2.2

- Added per-child folder removal with safe promotion when only one child remains.
- Added a long-press folder item action to remove an app or shortcut from its folder.
- Removed manual orientation config handling so Android recreates the launcher with correct dimensions.
- Added regression tests for folder child removal and folder deletion.

## 0.2.1

- Fixed restoration of the fifth dock slot from workspace JSON backups.
- Added an instrumentation regression test for dock-slot persistence.

## 0.2.0

- Replaced the fixed home canvas with a persistent multi-page native workspace.
- Added app pins, dock positioning, drag/reposition, app folders and folder rename.
- Added profile-aware app discovery and package-removal cleanup.
- Added app-published shortcut discovery, pinning and launching.
- Added Android AppWidgetHost provider selection, bind/configuration flow and resizable grid spans.
- Added JSON workspace backup and restore; device-bound widget grants are intentionally skipped.
- Added Android instrumentation coverage for persistence, folder lifecycle, restore and activity startup.
- Raised target SDK to API 36 and added Android lint plus three-pass APK verification to CI.
