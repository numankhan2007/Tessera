# Changelog

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
