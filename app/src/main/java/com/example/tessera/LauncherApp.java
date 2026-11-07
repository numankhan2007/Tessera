package com.example.tessera;

import android.content.ComponentName;
import android.content.pm.LauncherActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

/** Launchable activity metadata, including the stable Android user-profile serial. */
public final class LauncherApp {
    public final ComponentName componentName;
    public final UserHandle user;
    public final String label;
    public final Drawable icon;
    public final String packageName;
    public final long userSerial;

    public LauncherApp(LauncherActivityInfo info) { this(info,info.getUser().hashCode()); }

    public LauncherApp(LauncherActivityInfo info,long serial) {
        componentName=info.getComponentName();
        user=info.getUser();
        label=String.valueOf(info.getLabel());
        icon=info.getBadgedIcon(0);
        packageName=componentName.getPackageName();
        userSerial=serial;
    }

    public String stableKey() {
        return componentName.flattenToString()+"@"+userSerial;
    }
}
