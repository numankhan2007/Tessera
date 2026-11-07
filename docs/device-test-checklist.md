# Tessera device verification checklist

Run this checklist on the target phone after installing the debug APK. Record the Android version, device model, and any OEM launcher/security customizations.

## Install and HOME role

- [ ] Install app-debug.apk from Android's package installer.
- [ ] Open Tessera and grant it the Home role from Android's chooser.
- [ ] Press Home from an app; Tessera should appear consistently.
- [ ] Reboot the device; Tessera should remain selectable and launch as Home.
- [ ] Open Android Settings → Default apps → Home app and verify Tessera is selected.

## Workspace and persistence

- [ ] Verify the default dock and initial home pins render.
- [ ] Pin an app from the app drawer.
- [ ] Swipe horizontally between pages and create a new page.
- [ ] Move an app to another page.
- [ ] Move an app into and out of the dock.
- [ ] Drop one app onto another to create a folder.
- [ ] Add another app to a folder, open it, rename it, and remove a child.
- [ ] Force-stop Tessera and reopen it; verify pins, pages, folders, and dock remain.
- [ ] Uninstall a pinned app; verify its pin and any folder entry are removed.

## Search and shortcuts

- [ ] Open the app drawer and search by partial app name.
- [ ] Launch an app from search results.
- [ ] Long-press an app and open Android App Info.
- [ ] Open the app shortcuts list for an app that publishes shortcuts.
- [ ] Pin and launch one app shortcut; verify stale shortcuts fail gracefully.

## Widgets

- [ ] Add a simple clock/weather/calendar widget.
- [ ] Complete Android's widget binding/consent flow.
- [ ] Complete the provider configuration flow, where one exists.
- [ ] Verify widget content updates after leaving and returning to Home.
- [ ] Resize a widget using its context menu; test wider/narrower/taller/shorter.
- [ ] Remove a widget and verify the provider host view disappears.
- [ ] Reboot and verify the widget is restored by its persisted widget ID.

## Wallpaper, display, and accessibility

- [ ] Verify system wallpaper remains visible through the translucent Tessera surface.
- [ ] Open the system wallpaper picker from Workspace options.
- [ ] Verify status-bar, navigation-bar, display-cutout, and gesture insets.
- [ ] Test portrait and landscape orientations.
- [ ] Test display/font scale at default and enlarged values.
- [ ] Enable Reduce launcher motion and verify page transitions become immediate.
- [ ] Test TalkBack focus order and content descriptions for page, dock, drawer, and app controls.

## Backup / restore

- [ ] Export a workspace JSON file through the Android document picker.
- [ ] Clear Tessera app data or use a clean test device.
- [ ] Restore the JSON file.
- [ ] Verify dock slots, pages, pins, folders, and shortcut metadata are restored.
- [ ] Confirm the app explains that bound widgets must be added again after restore.

## Stability / OEM checks

- [ ] Repeatedly launch and close the drawer.
- [ ] Open and close Settings / App Info from Tessera.
- [ ] Install, update, disable, enable, and uninstall apps while Tessera is running.
- [ ] Lock/unlock and resume from background.
- [ ] Test Android predictive-back and system Home/Recents gestures.
- [ ] Confirm no crash, ANR, stuck overlay, or ghost widget view during repeated interaction.

## Release gates

- [ ] Review crash/ANR logs from at least one full day of ordinary use.
- [ ] Confirm work-profile behavior under the target organization's profile policy.
- [ ] Test with several real third-party widgets from different providers.
- [ ] Configure a user-owned release signing key before distributing a release build.
- [ ] Complete Google Play package-visibility policy review before Play distribution.
