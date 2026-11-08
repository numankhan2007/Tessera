# Changelog

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
