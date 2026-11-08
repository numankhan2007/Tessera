package com.example.tessera;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class LauncherVisualRegressionTest {
    @Test public void capturesWorkspaceAndAppLibraryScreenshots() throws Exception {
        Context target=ApplicationProvider.getApplicationContext();
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            SystemClock.sleep(1400);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture(target,"home.png");

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
            capture(target,"app-library.png");
        }
    }

    private static void capture(Context target,String name) throws Exception {
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull("Screenshot capture returned null for "+name,bitmap);
        assertTrue("Screenshot width is unexpectedly small",bitmap.getWidth()>=320);
        assertTrue("Screenshot height is unexpectedly small",bitmap.getHeight()>=600);

        File directory=new File(target.getExternalFilesDir(null),"tessera-visual");
        assertTrue("Could not create screenshot directory",directory.exists()||directory.mkdirs());
        File file=new File(directory,name);
        try(FileOutputStream output=new FileOutputStream(file)) {
            assertTrue("Screenshot PNG could not be encoded",bitmap.compress(Bitmap.CompressFormat.PNG,100,output));
        } finally {
            bitmap.recycle();
        }

        String shared="/sdcard/Pictures/TesseraVisual";
        String command="mkdir -p "+shared+" && cp "+file.getAbsolutePath()+" "+shared+"/"+name;
        ParcelFileDescriptor descriptor=InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().executeShellCommand(command);
        try(FileInputStream input=new FileInputStream(descriptor.getFileDescriptor())) {
            byte[] buffer=new byte[512];
            while(input.read(buffer)!=-1) { /* Drain shell output until copy completes. */ }
        } finally {
            descriptor.close();
        }
    }
}
