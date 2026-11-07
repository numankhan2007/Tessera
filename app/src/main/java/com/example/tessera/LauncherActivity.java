package com.example.tessera;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.pm.LauncherApps;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.content.pm.LauncherActivityInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Native Android HOME activity; no simulated app windows. */
public final class LauncherActivity extends Activity implements TesseraHomeView.Callbacks {
    private LauncherApps launcherApps;
    private TesseraHomeView homeView;
    private EditText searchInput;
    private boolean searchVisible;
    private LauncherApps.Callback packageCallback;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Window w=getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29){w.setNavigationBarContrastEnforced(false);w.setStatusBarContrastEnforced(false);}
        launcherApps=(LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);
        FrameLayout root=new FrameLayout(this);
        homeView=new TesseraHomeView(this,this);
        root.addView(homeView,new FrameLayout.LayoutParams(-1,-1));
        searchInput=new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextColor(0xfff6f2e1);
        searchInput.setHintTextColor(0xbff6f2e1);
        searchInput.setHint("Find an app or action");
        searchInput.setTextSize(14);
        searchInput.setTypeface(Typeface.create("serif",Typeface.ITALIC));
        searchInput.setBackgroundColor(Color.TRANSPARENT);
        searchInput.setPadding(dp(2),0,dp(2),0);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,dp(52),Gravity.TOP);
        sp.leftMargin=dp(70);sp.rightMargin=dp(54);sp.topMargin=dp(184);
        searchInput.setVisibility(View.GONE);
        root.addView(searchInput,sp);
        searchInput.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int c,int a){}
            public void onTextChanged(CharSequence s,int st,int before,int count){homeView.setQuery(s.toString());}
            public void afterTextChanged(Editable e){}
        });
        searchInput.setOnEditorActionListener((v,id,event)->{
            if(id==EditorInfo.IME_ACTION_SEARCH){List<LauncherApp> a=homeView.getVisibleApps();if(!a.isEmpty())launch(a.get(0));return true;}return false;
        });
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());v.setPadding(i.left,i.top,i.right,i.bottom);return android.view.WindowInsets.CONSUMED;}
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();
        });
        setContentView(root);
        registerPackageCallback();
        refreshApps();
        homeView.setDefaultHome(isDefaultHome());
    }

    private void registerPackageCallback(){
        if(launcherApps==null)return;
        packageCallback=new LauncherApps.Callback(){
            public void onPackageRemoved(String p,android.os.UserHandle u){refreshApps();}
            public void onPackageAdded(String p,android.os.UserHandle u){refreshApps();}
            public void onPackageChanged(String p,android.os.UserHandle u){refreshApps();}
            public void onPackagesAvailable(String[] p,android.os.UserHandle u,boolean r){refreshApps();}
            public void onPackagesUnavailable(String[] p,android.os.UserHandle u,boolean r){refreshApps();}
            public void onPackagesSuspended(String[] p,android.os.UserHandle u){refreshApps();}
            public void onPackagesUnsuspended(String[] p,android.os.UserHandle u){refreshApps();}
            public void onShortcutsChanged(String p,List<android.content.pm.ShortcutInfo>s,android.os.UserHandle u){}
        };
        launcherApps.registerCallback(packageCallback);
    }

    private void refreshApps(){
        if(launcherApps==null)return;
        try{
            List<LauncherApp> list=new ArrayList<>();
            for(LauncherActivityInfo i:launcherApps.getActivityList(null,Process.myUserHandle())){
                try{list.add(new LauncherApp(i));}catch(RuntimeException ignored){}
            }
            Collections.sort(list,Comparator.comparing(a->a.label.toLowerCase(Locale.getDefault())));
            runOnUiThread(()->{if(homeView!=null)homeView.setApps(list);});
        }catch(SecurityException|IllegalStateException e){
            runOnUiThread(()->Toast.makeText(this,"Unable to read installed apps.",Toast.LENGTH_LONG).show());
        }
    }

    private boolean isDefaultHome(){
        if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);return r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME)&&r.isRoleHeld(RoleManager.ROLE_HOME);}
        Intent i=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        return i.resolveActivity(getPackageManager())!=null&&getPackageName().equals(i.resolveActivity(getPackageManager()).getPackageName());
    }
    @Override protected void onStart(){super.onStart();refreshApps();}
    @Override protected void onResume(){super.onResume();if(homeView!=null)homeView.setDefaultHome(isDefaultHome());}
    @Override protected void onDestroy(){if(launcherApps!=null&&packageCallback!=null)launcherApps.unregisterCallback(packageCallback);super.onDestroy();}

    @Override public boolean onSearchRequested(){
        showSearchInput();
        return true;
    }
    @Override public void onLauncherSearchRequested(){ showSearchInput(); }
    private void showSearchInput(){
        searchVisible=true;searchInput.setVisibility(View.VISIBLE);searchInput.requestFocus();
        searchInput.post(()->{InputMethodManager m=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);if(m!=null)m.showSoftInput(searchInput,InputMethodManager.SHOW_IMPLICIT);});
    }
    @Override public void onDefaultHomeRequested(){
        try{if(Build.VERSION.SDK_INT>=29){RoleManager r=getSystemService(RoleManager.class);if(r!=null&&r.isRoleAvailable(RoleManager.ROLE_HOME))startActivityForResult(r.createRequestRoleIntent(RoleManager.ROLE_HOME),4101);}else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));}
        catch(Exception e){Toast.makeText(this,"Choose Tessera in Settings → Default apps → Home app.",Toast.LENGTH_LONG).show();}
    }
    @Override public void onAppSelected(LauncherApp app){hideSearch();launch(app);}
    @Override public void onAppLongPressed(LauncherApp app){
        hideSearch();try{startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+app.packageName)));}catch(Exception e){Toast.makeText(this,"App details unavailable.",Toast.LENGTH_SHORT).show();}
    }
    private void launch(LauncherApp app){
        try{launcherApps.startMainActivity(app.componentName,app.user,null,ActivityOptions.makeBasic().toBundle());}
        catch(Exception e){Toast.makeText(this,"Unable to open "+app.label,Toast.LENGTH_SHORT).show();refreshApps();}
    }
    private void hideSearch(){
        searchVisible=false;searchInput.setText("");searchInput.clearFocus();searchInput.setVisibility(View.GONE);
        InputMethodManager m=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);if(m!=null)m.hideSoftInputFromWindow(searchInput.getWindowToken(),0);
    }
    @Override public void onBackPressed(){if(searchVisible){hideSearch();homeView.closeDrawer();}else if(homeView.isDrawerOpen())homeView.closeDrawer();else super.onBackPressed();}
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
