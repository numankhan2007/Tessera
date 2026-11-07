package com.example.tessera;

import android.content.ComponentName;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

/** A launchable activity discovered through Android's LauncherApps service. */
public final class LauncherApp {
    public final ComponentName componentName;
    public final UserHandle user;
    public final String label;
    public final Drawable icon;
    public final String packageName;

    public LauncherApp(LauncherActivityInfo info) {
        componentName = info.getComponentName();
        user = info.getUser();
        label = String.valueOf(info.getLabel());
        icon = info.getBadgedIcon(0);
        packageName = componentName.getPackageName();
    }

    public String stableKey() {
        return componentName.flattenToString() + "@" + user.hashCode();
    }
}
