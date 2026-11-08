package com.example.tessera;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class LauncherVisualRegressionTest {
    @Test public void capturesVisibleWorkspaceAndAppLibrary() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Visual screenshot export requires Android 10+",Build.VERSION.SDK_INT>=29);
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            SystemClock.sleep(1000);
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
            SystemClock.sleep(700);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture(target,"app-library.png");
        }
    }

    private static void capture(Context target,String name) throws Exception {
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull("Screenshot capture returned null for "+name,bitmap);
        assertTrue("Screenshot width is unexpectedly small",bitmap.getWidth()>=320);
        assertTrue("Screenshot height is unexpectedly small",bitmap.getHeight()>=600);

        int brightSamples=0;
        for(int y=0;y<bitmap.getHeight();y+=2) {
            for(int x=0;x<bitmap.getWidth();x+=2) {
                int pixel=bitmap.getPixel(x,y);
                int luminance=(Color.red(pixel)*3+Color.green(pixel)*6+Color.blue(pixel))/10;
                if(Color.alpha(pixel)>0&&luminance>36)brightSamples++;
            }
        }
        assertTrue("Captured screen is effectively blank: "+name+
                ", visible sample pixels="+brightSamples,brightSamples>1200);

        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,name);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/TesseraVisual");
        values.put(MediaStore.Images.Media.IS_PENDING,1);
        Uri uri=target.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull("Could not create public screenshot media entry for "+name,uri);
        boolean saved=false;
        try(OutputStream output=target.getContentResolver().openOutputStream(uri)) {
            assertNotNull("Could not open screenshot output for "+name,output);
            assertTrue("Screenshot PNG could not be encoded for "+name,
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,output));
            output.flush();
            saved=true;
        } finally {
            bitmap.recycle();
            if(saved) {
                ContentValues complete=new ContentValues();
                complete.put(MediaStore.Images.Media.IS_PENDING,0);
                target.getContentResolver().update(uri,complete,null,null);
            } else {
                target.getContentResolver().delete(uri,null,null);
            }
        }
    }
}
