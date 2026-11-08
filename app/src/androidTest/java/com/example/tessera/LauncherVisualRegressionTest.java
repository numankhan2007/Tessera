package com.example.tessera;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class LauncherVisualRegressionTest {
    private static final String SCREENSHOT_DIR="/sdcard/Pictures/TesseraVisual";

    @Test public void capturesWorkspaceAndAppLibraryScreenshots() throws Exception {
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            SystemClock.sleep(1400);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture("home.png");

            scenario.onActivity(activity -> {
                try {
                    Field field=LauncherActivity.class.getDeclaredField("homeView");
                    field.setAccessible(true);
                    TesseraHomeView view=(TesseraHomeView)field.get(activity);
                    assertNotNull(view);
                    view.openDrawer();
                } catch(Exception error) {
                    throw new AssertionError("Could not open native app library for visual capture",error);
                }
            });
            SystemClock.sleep(800);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture("app-library.png");
        }
    }

    private static void capture(String name) throws Exception {
        runShell("mkdir -p "+SCREENSHOT_DIR);
        String path=SCREENSHOT_DIR+"/"+name;
        runShell("screencap -p "+path);
        String byteCount=runShell("wc -c < "+path).trim();
        long bytes;
        try { bytes=Long.parseLong(byteCount); }
        catch(NumberFormatException error) {
            throw new AssertionError("Could not verify screenshot output for "+name+": "+byteCount,error);
        }
        assertTrue("Screenshot is missing or unexpectedly small: "+name,bytes>4096);
    }

    private static String runShell(String command) throws Exception {
        ParcelFileDescriptor descriptor=InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand(command);
        try(FileInputStream input=new FileInputStream(descriptor.getFileDescriptor());
            ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[1024];
            int count;
            while((count=input.read(buffer))!=-1)output.write(buffer,0,count);
            return output.toString("UTF-8");
        } finally {
            descriptor.close();
        }
    }
}
