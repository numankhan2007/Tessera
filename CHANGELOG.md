# Changelog

## 0.2.0

- Replaced the fixed home canvas with a persistent multi-page native workspace.
- Added app pins, dock positioning, drag/reposition, app folders and folder rename.
- Added profile-aware app discovery and package-removal cleanup.
- Added app-published shortcut discovery, pinning and launching.
- Added Android AppWidgetHost provider selection, bind/configuration flow and resizable grid spans.
- Added JSON workspace backup and restore; device-bound widget grants are intentionally skipped.
- Added Android instrumentation coverage for persistence, folder lifecycle, restore and activity startup.
- Raised target SDK to API 36 and added Android lint plus three-pass APK verification to CI.
