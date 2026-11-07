package com.example.tessera;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class LauncherActivitySmokeTest {
    @Test public void homeActivityCreatesItsNativeWorkspace() {
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(android.R.id.content));
                assertNotNull(activity.getWindow());
            });
        }
    }
}
