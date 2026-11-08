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

    @Test public void homeShowsLiveClockDateAndDaypartGreeting() throws Exception {
        try(ActivityScenario<LauncherActivity> scenario=ActivityScenario.launch(LauncherActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Field homeField=LauncherActivity.class.getDeclaredField("homeView");
                    homeField.setAccessible(true);
                    TesseraHomeView home=(TesseraHomeView)homeField.get(activity);
                    assertNotNull(home);
                    java.lang.reflect.Field timeField=TesseraHomeView.class.getDeclaredField("heroTime");
                    java.lang.reflect.Field greetingField=TesseraHomeView.class.getDeclaredField("heroGreeting");
                    java.lang.reflect.Field dateField=TesseraHomeView.class.getDeclaredField("clock");
                    timeField.setAccessible(true);
                    greetingField.setAccessible(true);
                    dateField.setAccessible(true);
                    android.widget.TextView time=(android.widget.TextView)timeField.get(home);
                    android.widget.TextView greeting=(android.widget.TextView)greetingField.get(home);
                    android.widget.TextView date=(android.widget.TextView)dateField.get(home);
                    assertNotNull(time);
                    assertNotNull(greeting);
                    assertNotNull(date);
                    assertTrue("Live time is empty",time.getText().toString().matches("\\d{1,2}:\\d{2}"));
                    assertTrue("Date is empty",date.getText().length()>0);
                    String daypart=greeting.getText().toString();
                    assertTrue("Unexpected daypart greeting: "+daypart,
                            daypart.equals("GOOD MORNING")||daypart.equals("GOOD AFTERNOON")
                                    ||daypart.equals("GOOD EVENING")||daypart.equals("GOOD NIGHT"));
                } catch(Exception error) {
                    throw new AssertionError("Live clock hero could not be inspected",error);
                }
            });
        }
    }
}
