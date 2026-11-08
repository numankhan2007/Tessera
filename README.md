# Tessera

**Kinetic Spatial UI · Native Android launcher**

Tessera is a native Android HOME application designed around a wallpaper-first workspace, expressive motion, deep emerald surfaces and antique-gold accents. It uses Android launcher APIs for app discovery, app launching, shortcuts and widgets; the home screen is not a WebView or a simulated desktop.

> **Current status:** core launcher with the 0.4.3 Kinetic Home and Adaptive Workspace refresh. It is an installable debug build and is continuously compiled and audited by GitHub Actions. Real-device interaction, OEM-specific widget behavior and release certification still require a physical-device QA pass.

## Core functionality

| Area | Current implementation |
|---|---|
| Android HOME role | HOME/DEFAULT intent filters and Android 10+ \`RoleManager\` request flow |
| App catalogue | \`LauncherApps\` enumeration across accessible user profiles |
| App lifecycle | App install, removal, package change, profile-aware refresh and pinned-item cleanup |
| App launch | Native \`LauncherApps.startMainActivity()\` with the correct user profile |
| Workspace pages | Multiple horizontal pages, persisted in SQLite; remembers the last page shown |
| Home pins | Pin installed applications from the searchable drawer onto the current workspace page |
| Dock | Persisted dock slots; move items to/from the dock |
| Repositioning | Long-press an item, choose **Move / Reposition**, then drop it onto a page or dock location |
| Folders | Create, open, rename and edit folders; remove individual children, with the final child promoted back to a normal home item |
| App drawer | Opaque app-library sheet with quick access from dock favourites, live search, Android category filters and pin-to-home action |
| App shortcuts | Discover app-published dynamic/manifest shortcuts, pin them and launch them through \`LauncherApps\` |
| Work profiles | Enumerate profiles Android exposes to the launcher and retain the user's profile serial with pins |
| Widgets | Native \`AppWidgetHost\` / \`AppWidgetHostView\`; provider selection, Android bind permission, provider configuration, persistence, removal and grid-span resizing |
| Wallpaper | Translucent launcher window with system wallpaper behind it; opens Android wallpaper picker |
| Backup / restore | Export/import workspace JSON through Android's Storage Access Framework |
| Accessibility basics | Native text controls, app labels and content descriptions; system app/widget surfaces retain their platform accessibility behavior |

## Data and behavior

- Workspace entries, folder contents, widget IDs and layout coordinates are stored locally in \`tessera-workspace.db\`.
- The dock is represented as page \`-1\`; normal workspace pages are numbered from zero.
- A folder contains the original component, profile serial, title and optional shortcut ID of each child entry.
- When an app is uninstalled, pinned app and shortcut entries for that package/profile are removed. Folder children are pruned, and the last remaining child is promoted back to a normal workspace item.
- App widgets are Android-owned bindings. They are not transferable between devices or installs, so a JSON restore skips widget bindings and tells the user to add those widgets again.
- App visibility depends on Android package visibility and the profiles available to the current user. \`QUERY_ALL_PACKAGES\` is declared because app discovery is a central launcher function; review the current Google Play policy before Play distribution.

## Build

Requirements: JDK 17, Android SDK Platform 36, Android SDK Build Tools 36.0.0, Gradle 8.14.6.

\`\`\`bash
gradle --no-daemon :app:assembleDebug
\`\`\`

The GitHub Actions workflow installs the Android SDK, builds the debug APK, runs Android lint, audits the generated artifact three times, then runs 12 Android instrumentation tests on an Android 15 emulator and requires a 12/12 JUnit pass plus both home/app-library PNG captures. The audit checks ZIP integrity, package ID, HOME launcher entry, min/target SDK metadata, and APK signature.

APK output:

\`\`\`text
app/build/outputs/apk/debug/app-debug.apk
\`\`\`

The CI APK is signed with a stable, repository-included test-only debug key so subsequent Tessera CI builds can update it in place. It is intended only for installation and device testing, not Play Store release. This shared key is public and must never be reused for production signing. The debug application ID is \`com.example.tessera.debug\`; the release application ID is \`com.example.tessera\`.

## Install and select as Home

1. Download \`app-debug.apk\` from the successful GitHub Actions artifact.
2. For the first install of this stable-signature line, uninstall older temporary-CI-signed Tessera debug builds first. Export a workspace backup from the older build before uninstalling if you need to preserve its layout. Later builds on this same stable key can update in place.
3. Open the APK on the Android device and allow installation from the selected source when prompted.
4. Open Tessera and tap **Set Tessera as your Home app**.
5. Choose Tessera in Android's Home app role chooser.

To pin apps, open the drawer and tap the \`+\` beside an app. Long-press an app in the drawer to reveal **Pin to Home**, **App shortcuts** and **App info**. To move a home item, long-press it, select **Move / Reposition**, then drop it at the target location. Dropping one app on another creates a folder.

## Known limitations / release gates

This is a complete installable core launcher, not a claim that every possible launcher feature is implemented or that every OEM device has been tested.

- Widget layout uses a four-column grid with provider-derived initial spans and menu-based span resizing. It does not yet have drag handles, arbitrary pixel-level resizing or guaranteed behavior for every vendor widget.
- Restored backups intentionally omit widget bindings because Android widget IDs and bind grants are device-specific.
- Folder contents are app/shortcut entries; individual children can be removed. Nested folders and custom artwork are not supported.
- Drawer search currently matches installed app labels; it does not search contacts, settings, files, web content or contextual actions.
- Workspace icons use the app-provided launcher icons. Licensed Reem Kufi / Amiri / Aref Ruqaa assets are not bundled yet; the UI uses installed Android typefaces as safe fallbacks.
- The 0.4.3 motion system adds three low-contrast drifting aurora bloom fields, eight atmospheric motes, a slow reversing and breathing hero-star orbit, subtle per-icon drift, spring-like workspace entrances, a content-sized app grid, a refined app-library sheet entrance, tile ripple/press feedback and horizontal page parallax. Ambient drawing is paced at roughly 40 fps, pauses while the launcher window is hidden, and respects both Tessera's Reduce Motion switch and Android's global animator scale. The app library uses Android application categories and user-pinned dock favourites; it does not request usage-history permission or fabricate app-usage predictions. More advanced physics-based page transitions, per-page wallpaper effects and custom icon packs remain future enhancements.
- CI verifies compilation, Android lint, APK archive integrity, package metadata and debug signature. It does not replace installation and gesture/widget testing on a physical device. OEM launchers, work-profile policies, battery optimizers, edge-to-edge insets and individual widgets need device-level QA.
- A release APK needs a user-owned signing key and signing configuration. The shared debug key is intentionally public and must never be used as a release identity.

## Architecture

\`\`\`text
LauncherActivity
├── LauncherApps / UserManager
│   ├── profile-aware app catalogue
│   ├── package and shortcut callbacks
│   ├── startMainActivity / startShortcut
│   └── pinShortcuts
├── AppWidgetManager / AppWidgetHost
│   ├── bind and configure
│   ├── host AppWidgetHostView
│   └── restore persisted widget IDs
├── WorkspaceStore (SQLiteOpenHelper)
│   ├── pages, pins and dock slots
│   ├── folders and shortcuts
│   └── backup / restore JSON
└── TesseraHomeView
    ├── wallpaper-first home surface
    ├── horizontal workspace pager
    ├── drag/reposition and folder actions
    ├── searchable app drawer
    └── widget hosting containers
\`\`\`

## Source provenance

The original \`Tessera.apk\` contained compiled DEX and resources, not the original source tree. This repository is a clean, maintainable reimplementation informed by the uploaded APK's static inventory and the approved visual direction. It is not presented as an exact decompilation.

See [docs/original-apk-inventory.md](docs/original-apk-inventory.md) and [docs/design-reference.md](docs/design-reference.md).
