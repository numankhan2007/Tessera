package com.example.tessera;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.Window;
import android.provider.MediaStore;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class LauncherVisualRegressionTest {
    @Test public void capturesRenderedWorkspaceAndAppLibrary() throws Exception {
        assertTrue("Visual screenshot export requires Android 10+",Build.VERSION.SDK_INT>=29);
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            SystemClock.sleep(900);
            Bitmap[] homeCapture=new Bitmap[1];
            Context[] targetContext=new Context[1];
            scenario.onActivity(activity -> {
                targetContext[0]=activity;
                View decor=activity.getWindow().getDecorView();
                decor.getRootView().invalidate();
                homeCapture[0]=renderView(decor,"home.png");
            });
            saveBitmap(targetContext[0],homeCapture[0],"home.png");

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
            SystemClock.sleep(550);
            InstrumentationRegistryWait.idle();

            Bitmap[] drawerCapture=new Bitmap[1];
            scenario.onActivity(activity -> {
                try {
                    Field viewField=LauncherActivity.class.getDeclaredField("homeView");
                    viewField.setAccessible(true);
                    TesseraHomeView view=(TesseraHomeView)viewField.get(activity);
                    Field drawerField=TesseraHomeView.class.getDeclaredField("drawerDialog");
                    drawerField.setAccessible(true);
                    Dialog dialog=(Dialog)drawerField.get(view);
                    assertNotNull("App library dialog was not created",dialog);
                    assertTrue("App library dialog is not showing",dialog.isShowing());
                    Window window=dialog.getWindow();
                    assertNotNull("App library window is missing",window);
                    drawerCapture[0]=renderView(window.getDecorView(),"app-library.png");
                } catch(Exception error) {
                    throw new AssertionError("Could not render native app library",error);
                }
            });
            saveBitmap(targetContext[0],drawerCapture[0],"app-library.png");
        }
    }

    private static Bitmap renderView(View view,String name) {
        assertNotNull("View missing for "+name,view);
        int width=view.getWidth();
        int height=view.getHeight();
        assertTrue("View has invalid width for "+name+": "+width,width>=320);
        assertTrue("View has invalid height for "+name+": "+height,height>=600);
        view.invalidate();
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        view.draw(canvas);
        int visibleSamples=0;
        for(int y=0;y<height;y+=2) {
            for(int x=0;x<width;x+=2) {
                int pixel=bitmap.getPixel(x,y);
                int luminance=(Color.red(pixel)*3+Color.green(pixel)*6+Color.blue(pixel))/10;
                if(Color.alpha(pixel)>0&&luminance>36)visibleSamples++;
            }
        }
        assertTrue("Rendered view is effectively blank: "+name+
                ", visible sample pixels="+visibleSamples,visibleSamples>1200);
        return bitmap;
    }

    private static void saveBitmap(Context context,Bitmap bitmap,String name) throws Exception {
        assertNotNull("Bitmap was not rendered for "+name,bitmap);
        ContentValues values=new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME,name);
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/TesseraVisual");
        values.put(MediaStore.Images.Media.IS_PENDING,1);
        Uri uri=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        assertNotNull("Could not create public screenshot media entry for "+name,uri);
        boolean saved=false;
        try(OutputStream output=context.getContentResolver().openOutputStream(uri)) {
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
                context.getContentResolver().update(uri,complete,null,null);
            } else {
                context.getContentResolver().delete(uri,null,null);
            }
        }
    }

    private static final class InstrumentationRegistryWait {
        static void idle() {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        }
    }
}
