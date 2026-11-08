# Tessera

**Kinetic Spatial UI · Native Android launcher**

Tessera is a native Android HOME application designed around a wallpaper-first workspace, expressive motion, deep emerald surfaces and antique-gold accents. It uses Android launcher APIs for app discovery, app launching, shortcuts and widgets; the home screen is not a WebView or a simulated desktop.

> **Current status:** feature-complete core launcher foundation (0.2.6). It is an installable debug build and is continuously compiled and audited by GitHub Actions. Real-device interaction, OEM-specific widget behavior and release certification still require a physical-device QA pass.

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
| App drawer | Native dialog with live label filtering and pin-to-home action |
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
- When an app is uninstalled, pinned app and shortcut entries for that package/profile are removed. Folder entries are pruned as well.
- App widgets are Android-owned bindings. They are not transferable between devices or installs, so a JSON restore skips widget bindings and tells the user to add those widgets again.
- App visibility depends on Android package visibility and the profiles available to the current user. \`QUERY_ALL_PACKAGES\` is declared because app discovery is a central launcher function; review the current Google Play policy before Play distribution.

## Build

Requirements: JDK 17, Android SDK Platform 36, Android SDK Build Tools 36.0.0, Gradle 8.14.6.

\`\`\`bash
gradle --no-daemon :app:assembleDebug
\`\`\`

The GitHub Actions workflow installs the Android SDK, builds the debug APK, runs Android lint, then audits the generated artifact three times. The audit checks ZIP integrity, package ID, HOME launcher entry, min/target SDK metadata, and APK signature.

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
- Advanced motion systems, custom icon packs, themed folder previews, per-page wallpaper effects and shortcut pinning requests that require vendor confirmation remain future enhancements.
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
