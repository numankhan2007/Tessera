# Tessera

**Kinetic Spatial UI · Native Android launcher**

Tessera is being rebuilt as a real Android HOME application. Its foundation is a wallpaper-first launcher surface, a live app catalogue backed by Android's `LauncherApps` service, an animated app drawer, and an in-launcher app search.

> **Current status:** early native foundation (0.1.0). This is a clean source reconstruction informed by the uploaded APK's package/class inventory and the approved Figma direction. The original Java/Kotlin source was not present in the upload, so this repository does not claim to be the exact decompilation of the APK.

## Product direction

- Wallpaper-first surface with translucent, restrained emerald/antique-gold treatments.
- Expressive serif italic display typography and RTL-aware Arabic secondary typography; app labels and controls stay legible.
- Morphing app drawer, portal-style search, gesture-following transitions, and native app launching.
- No WebView-based home screen, desktop simulator, or fake app windows.

## Current implementation

| Capability | Status |
|---|---|
| Android HOME intent and default-home flow | Implemented |
| Live installed-app discovery via `LauncherApps` | Implemented |
| Open selected launcher activities | Implemented |
| Home surface, live clock/date, dock and shortcuts | Initial implementation |
| Swipe-up drawer and swipe-down dismissal | Initial implementation |
| App drawer search and filtering | Initial implementation |
| Long-press app → Android app info | Implemented |
| Package install/update/uninstall refresh callbacks | Implemented |
| Wallpaper display behind translucent launcher window | Implemented; device validation pending |
| Drag/drop, folders, multi-page workspace persistence | Planned |
| App widgets through `AppWidgetHost` | Planned |
| Deep shortcuts, work profiles, backup/restore | Planned |
| Production motion tuning and accessibility QA | Planned |

## Architecture

```text
LauncherActivity
├── LauncherApps service
│   ├── getActivityList()
│   ├── startMainActivity()
│   └── LauncherApps.Callback
├── TesseraHomeView
│   ├── wallpaper-first home canvas
│   ├── animated drawer transition
│   ├── searchable app grid
│   └── touch/gesture routing
└── EditText search input
```

The first milestone deliberately uses Android framework APIs and a custom Canvas view to keep the launcher lightweight. App discovery and app launching are system-backed rather than simulated.

## Build

Requirements: JDK 17, Android SDK Platform 37, Android SDK Build Tools 36.0.0, Gradle 9.6.0.

```bash
./gradlew :app:assembleDebug
```

The repository contains a lightweight Gradle bootstrap script because a generated Gradle Wrapper JAR could not be obtained in the source-generation environment. The script downloads the pinned Gradle distribution on the first run. In CI, GitHub Actions installs Gradle directly and builds `:app:assembleDebug`.

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Install and select as Home

Install the debug APK, open Tessera, then choose **Set Tessera as your Home app**. On Android 10+, Tessera asks Android's `RoleManager` for the Home role. On earlier releases, it opens Home settings.

The release `applicationId` is kept as `com.example.tessera`, matching the uploaded APK's DEX package identity. Debug builds add `.debug` so the new launcher can be installed beside the prototype. An in-place release update still requires the original APK's signing certificate; if the key differs, Android requires uninstalling the old app before installing the new release build.

## Typography

The current source uses Android's installed `serif`, `serif-italic`, and `cursive` typefaces as safe fallbacks. The Figma specimen explores Alegreya Italic, Amiri, Aladin, Reem Kufi, and Aref Ruqaa. Packaging the final licensed font files, checking font attribution, and validating Arabic shaping on the target device remain a separate integration step. Do not assume the Figma font assignments automatically install fonts on Android.

## Privacy and package visibility

The launcher declares `QUERY_ALL_PACKAGES` because discovering launchable applications is its core function. Before distribution, review the current Google Play package-visibility policy and provide the required Play Console declaration if distributing through Play.

## Known limitations

- No `AppWidgetHost` yet, so widgets cannot be added or resized.
- No drag-and-drop workspace editor, folders, or multiple workspace pages yet.
- Home shortcuts are currently the first six alphabetically sorted launchable apps, not user-customisable pins.
- Search currently filters installed app labels; contextual actions and contacts are not implemented.
- The view uses a 393 × 852 design coordinate space and scales to the available viewport; device-specific layout, font scale, landscape and cutout testing are still required.
- Wallpaper rendering, navigation gesture interactions, app launch transitions, and target-API behaviour require on-device validation. The initial source-generation environment did not have Android SDK Build Tools installed; GitHub Actions is configured to perform an SDK-backed build after the source is pushed.

## Roadmap

1. Stabilise HOME role, app catalogue, drawer/search, wallpaper, insets and app lifecycle.
2. Add persistent workspace pages, editable hotseat, drag/drop, folders and uninstall-safe layout restoration.
3. Add native widgets with `AppWidgetHost` / `AppWidgetHostView`, resizing and widget permissions.
4. Add shortcuts, work-profile handling, predictive back and accessibility/reduced-motion settings.
5. Integrate licensed typography assets and refine motion to a spring/velocity-driven system.
6. Add backup/restore, release signing, QA matrix and Play policy review.

## Source provenance

The project was bootstrapped from the uploaded `Tessera.apk` and the agreed design direction. The APK contains compiled DEX and assets, not the original source tree. This source is a maintainable reimplementation, not a byte-for-byte restoration of the app that produced that APK.

See [docs/original-apk-inventory.md](docs/original-apk-inventory.md) for the static prototype inventory and [docs/design-reference.md](docs/design-reference.md) for the Figma design direction.
