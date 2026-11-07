package com.example.tessera;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.content.pm.UserInfo;
import android.app.role.RoleManager;
import android.content.ComponentName;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
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
import android.widget.PopupMenu;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Android HOME activity. All app launches and widgets use platform launcher APIs. */
public final class LauncherActivity extends Activity implements TesseraHomeView.Callbacks {
    private static final int WIDGET_HOST_ID=0x544553;
    private static final int REQUEST_BIND_WIDGET=4102;
    private static final int REQUEST_CONFIGURE_WIDGET=4103;

    private LauncherApps launcherApps;
    private UserManager userManager;
    private AppWidgetManager widgetManager;
    private AppWidgetHost widgetHost;
    private WorkspaceStore workspaceStore;
    private TesseraHomeView homeView;
    private List<LauncherApp> installedApps=new ArrayList<>();
    private final Map<Integer,View> widgetViews=new HashMap<>();
    private LauncherApps.Callback packageCallback;
    private int pendingWidgetId=-1;
    private boolean callbackRegistered;
    private boolean reducedMotion;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if(Build.VERSION.SDK_INT>=29) {
            getWindow().setNavigationBarContrastEnforced(false);
            getWindow().setStatusBarContrastEnforced(false);
        }

        launcherApps=(LauncherApps)getSystemService(LAUNCHER_APPS_SERVICE);
        userManager=(UserManager)getSystemService(USER_SERVICE);
        widgetManager=AppWidgetManager.getInstance(this);
        widgetHost=new AppWidgetHost(this,WIDGET_HOST_ID);
        workspaceStore=new WorkspaceStore(this);
        reducedMotion=getPreferences(MODE_PRIVATE).getBoolean("reduce_motion",false);

        homeView=new TesseraHomeView(this,this);
        homeView.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets i=insets.getInsets(android.view.WindowInsets.Type.systemBars()
                        |android.view.WindowInsets.Type.displayCutout());
                v.setPadding(dp(18)+i.left,dp(12)+i.top,dp(18)+i.right,dp(10)+i.bottom);
                return android.view.WindowInsets.CONSUMED;
            }
            v.setPadding(dp(18)+insets.getSystemWindowInsetLeft(),
                    dp(12)+insets.getSystemWindowInsetTop(),
                    dp(18)+insets.getSystemWindowInsetRight(),
                    dp(10)+insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(homeView);
        refreshApps();
        renderWorkspace();
    }

    @Override protected void onStart() {
        super.onStart();
        try { widgetHost.startListening(); } catch(RuntimeException ignored) {}
        registerPackageCallback();
        refreshApps();
        renderWorkspace();
    }

    @Override protected void onStop() {
        unregisterPackageCallback();
        try { widgetHost.stopListening(); } catch(RuntimeException ignored) {}
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if(homeView!=null)renderWorkspace();
    }

    @Override protected void onDestroy() {
        unregisterPackageCallback();
        if(workspaceStore!=null)workspaceStore.close();
        super.onDestroy();
    }

    private void registerPackageCallback() {
        if(launcherApps==null||callbackRegistered)return;
        packageCallback=new LauncherApps.Callback() {
            @Override public void onPackageRemoved(String p,UserHandle u){refreshApps();}
            @Override public void onPackageAdded(String p,UserHandle u){refreshApps();}
            @Override public void onPackageChanged(String p,UserHandle u){refreshApps();}
            @Override public void onPackagesAvailable(String[] p,UserHandle u,boolean replacing){refreshApps();}
            @Override public void onPackagesUnavailable(String[] p,UserHandle u,boolean replacing){refreshApps();}
            @Override public void onPackagesSuspended(String[] p,UserHandle u){refreshApps();}
            @Override public void onPackagesUnsuspended(String[] p,UserHandle u){refreshApps();}
            @Override public void onShortcutsChanged(String p,List<ShortcutInfo> shortcuts,UserHandle u){}
        };
        launcherApps.registerCallback(packageCallback);
        callbackRegistered=true;
    }

    private void unregisterPackageCallback() {
        if(launcherApps!=null&&callbackRegistered) {
            try { launcherApps.unregisterCallback(packageCallback); } catch(RuntimeException ignored) {}
            callbackRegistered=false;
        }
    }

    private void refreshApps() {
        if(launcherApps==null)return;
        new Thread(()->{
            ArrayList<LauncherApp> result=new ArrayList<>();
            try {
                List<UserHandle> profiles=launcherApps.getProfiles();
                for(UserHandle profile:profiles) {
                    long serial;
                    try { serial=userManager.getSerialNumberForUser(profile); }
                    catch(Exception e) { serial=profile.hashCode(); }
                    List<LauncherActivityInfo> activities=launcherApps.getActivityList(null,profile);
                    for(LauncherActivityInfo info:activities) {
                        try { result.add(new LauncherApp(info,serial)); }
                        catch(RuntimeException ignored) {}
                    }
                }
                Collections.sort(result,Comparator.comparing(a->a.label.toLowerCase(Locale.getDefault())));
            } catch(SecurityException|IllegalStateException e) {
                runOnUiThread(()->Toast.makeText(this,"Tessera could not read the app catalogue.",Toast.LENGTH_LONG).show());
            }
            runOnUiThread(()->{
                installedApps=result;
                if(workspaceStore!=null)workspaceStore.seedIfEmpty(installedApps);
                renderWorkspace();
            });
        },"Tessera-AppCatalog").start();
    }

    private void renderWorkspace() {
        if(homeView==null||workspaceStore==null)return;
        List<WorkspaceItem> items=workspaceStore.all();
        HashMap<Integer,View> activeWidgets=new HashMap<>();
        for(WorkspaceItem item:items) if(WorkspaceItem.WIDGET.equals(item.type)) {
            AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(item.widgetId);
            if(info==null) {
                workspaceStore.delete(item.id);
                continue;
            }
            View host=widgetViews.get(item.widgetId);
            if(host==null) {
                try {
                    android.appwidget.AppWidgetHostView widget=widgetHost.createView(this,item.widgetId,info);
                    widget.setAppWidget(item.widgetId,info);
                    host=widget;
                    widgetViews.put(item.widgetId,host);
                } catch(RuntimeException ignored) {}
            }
            if(host!=null)activeWidgets.put(item.widgetId,host);
        }
        homeView.render(workspaceStore.all(),installedApps,workspaceStore.pageCount(),
                isDefaultHome(),activeWidgets,reducedMotion);
    }

    private boolean isDefaultHome() {
        if(Build.VERSION.SDK_INT>=29) {
            RoleManager role=getSystemService(RoleManager.class);
            return role!=null&&role.isRoleAvailable(RoleManager.ROLE_HOME)&&role.isRoleHeld(RoleManager.ROLE_HOME);
        }
        Intent intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ComponentName current=intent.resolveActivity(getPackageManager());
        return current!=null&&getPackageName().equals(current.getPackageName());
    }

    @Override public boolean onSearchRequested() {
        if(homeView!=null)homeView.openDrawer();
        return true;
    }

    @Override public void onLaunchItem(WorkspaceItem item) {
        if(item==null)return;
        if(WorkspaceItem.FOLDER.equals(item.type)) { openFolder(item);return; }
        if(WorkspaceItem.WIDGET.equals(item.type))return;
        if(WorkspaceItem.SHORTCUT.equals(item.type)) {
            LauncherApp parent=findApp(item.component,item.userSerial);
            if(parent!=null&&launcherApps!=null) {
                try {
                    launcherApps.startShortcut(parent.packageName,item.payload,parent.user,
                            (Rect)null,ActivityOptions.makeBasic().toBundle());
                } catch(Exception e) {
                    Toast.makeText(this,"This shortcut is no longer available.",Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }
        LauncherApp app=findApp(item.component,item.userSerial);
        if(app!=null)launch(app);
        else Toast.makeText(this,"This app is no longer installed.",Toast.LENGTH_SHORT).show();
    }

    @Override public void onAppSelected(LauncherApp app) { launch(app); }

    private void launch(LauncherApp app) {
        try {
            launcherApps.startMainActivity(app.componentName,app.user,null,
                    ActivityOptions.makeBasic().toBundle());
        } catch(Exception e) {
            Toast.makeText(this,"Unable to open "+app.label,Toast.LENGTH_SHORT).show();
            refreshApps();
        }
    }

    private LauncherApp findApp(String component,long serial) {
        if(component==null)return null;
        for(LauncherApp app:installedApps)
            if(component.equals(app.componentName.flattenToString())&&app.userSerial==serial)return app;
        return null;
    }

    @Override public void onAppLongPressed(LauncherApp app,View anchor) {
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add("Pin to Home").setOnMenuItemClickListener(mi->{onPinApp(app);return true;});
        menu.getMenu().add("App shortcuts").setOnMenuItemClickListener(mi->{showAppShortcuts(app);return true;});
        menu.getMenu().add("App info").setOnMenuItemClickListener(mi->{openAppInfo(app);return true;});
        menu.show();
    }

    @Override public void onPinApp(LauncherApp app) {
        for(WorkspaceItem item:workspaceStore.all()) {
            if(WorkspaceItem.APP.equals(item.type)&&app.componentName.flattenToString().equals(item.component)
                    &&item.userSerial==app.userSerial) {
                Toast.makeText(this,app.label+" is already on Home.",Toast.LENGTH_SHORT).show();return;
            }
        }
        int page=0,cell=workspaceStore.firstFreeCell(page,4,6);
        if(cell<0) {
            page=workspaceStore.pageCount();
            workspaceStore.addPage();
            cell=0;
        }
        workspaceStore.addApp(page,cell%4,cell/4,app.componentName.flattenToString(),app.label,app.userSerial);
        renderWorkspace();
    }

    @Override public void onAddWidgetRequested() { showWidgetPicker(); }

    @Override public void onAddPageRequested() {
        int page=workspaceStore.addPage();
        renderWorkspace();
        Toast.makeText(this,"Workspace page "+(page+1)+" created.",Toast.LENGTH_SHORT).show();
    }

    @Override public void onRemoveItem(WorkspaceItem item) {
        if(item==null)return;
        if(WorkspaceItem.WIDGET.equals(item.type)) {
            try { widgetHost.deleteAppWidgetId(item.widgetId); } catch(RuntimeException ignored) {}
            widgetViews.remove(item.widgetId);
        }
        workspaceStore.delete(item.id);
        renderWorkspace();
    }

    @Override public void onDropItem(WorkspaceItem item,int page,int x,int y) {
        if(item==null)return;
        if(page==-1) { x=Math.max(0,Math.min(4,x));y=0; }
        else { x=Math.max(0,Math.min(3,x));y=Math.max(0,Math.min(5,y)); }
        if(WorkspaceItem.WIDGET.equals(item.type)) {
            if(page==-1){Toast.makeText(this,"Widgets stay on workspace pages.",Toast.LENGTH_SHORT).show();return;}
            x=0;y=Math.min(4,y);
        }
        WorkspaceItem target=workspaceStore.findAt(page,x,y,item.id);
        if(target!=null&&target.id!=item.id) {
            if(WorkspaceItem.WIDGET.equals(target.type)||WorkspaceItem.WIDGET.equals(item.type)) {
                Toast.makeText(this,"That space is occupied by a widget.",Toast.LENGTH_SHORT).show();return;
            }
            workspaceStore.mergeInto(item.id,target.id);
        } else {
            workspaceStore.move(item.id,page,x,y);
        }
        renderWorkspace();
    }

    @Override public void onResizeWidget(WorkspaceItem item,int spanY) {
        if(item==null)return;
        workspaceStore.resizeWidget(item.id,spanY);
        renderWorkspace();
    }

    @Override public void onSetHomeRequested() {
        try {
            if(Build.VERSION.SDK_INT>=29) {
                RoleManager role=getSystemService(RoleManager.class);
                if(role!=null&&role.isRoleAvailable(RoleManager.ROLE_HOME))
                    startActivityForResult(role.createRequestRoleIntent(RoleManager.ROLE_HOME),4101);
            } else startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
        } catch(Exception e) {
            Toast.makeText(this,"Choose Tessera in Settings → Default apps → Home app.",Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onOpenSystemWallpaper() {
        try { startActivity(new Intent(Intent.ACTION_SET_WALLPAPER)); }
        catch(Exception e){Toast.makeText(this,"Wallpaper settings are not available.",Toast.LENGTH_SHORT).show();}
    }

    @Override public void onReducedMotionChanged(boolean enabled) {
        reducedMotion=enabled;
        getPreferences(MODE_PRIVATE).edit().putBoolean("reduce_motion",enabled).apply();
        renderWorkspace();
    }

    private void openAppInfo(LauncherApp app) {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:"+app.packageName)));
        } catch(Exception e) {
            Toast.makeText(this,"App details unavailable.",Toast.LENGTH_SHORT).show();
        }
    }

    private void showAppShortcuts(LauncherApp app) {
        if(Build.VERSION.SDK_INT<25||launcherApps==null) {
            Toast.makeText(this,"App shortcuts require Android 7.1 or newer.",Toast.LENGTH_SHORT).show();return;
        }
        try {
            LauncherApps.ShortcutQuery query=new LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC
                            |LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                            |LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> shortcuts=launcherApps.getShortcuts(query,app.user);
            if(shortcuts==null||shortcuts.isEmpty()) {
                Toast.makeText(this,"This app does not publish launcher shortcuts.",Toast.LENGTH_SHORT).show();return;
            }
            ArrayList<ShortcutInfo> enabled=new ArrayList<>();
            ArrayList<String> names=new ArrayList<>();
            for(ShortcutInfo s:shortcuts)if(s.isEnabled()) {
                enabled.add(s);names.add(String.valueOf(s.getShortLabel()));
            }
            if(enabled.isEmpty()) {
                Toast.makeText(this,"No active shortcuts are available.",Toast.LENGTH_SHORT).show();return;
            }
            new AlertDialog.Builder(this).setTitle(app.label+" shortcuts")
                    .setItems(names.toArray(new String[0]),(d,which)->pinShortcut(app,enabled.get(which)))
                    .setNegativeButton("Cancel",null).show();
        } catch(SecurityException e) {
            Toast.makeText(this,"Set Tessera as your Home app to manage app shortcuts.",Toast.LENGTH_LONG).show();
        } catch(RuntimeException e) {
            Toast.makeText(this,"Unable to load shortcuts for this app.",Toast.LENGTH_SHORT).show();
        }
    }

    private void pinShortcut(LauncherApp app,ShortcutInfo shortcut) {
        try {
            LauncherApps.ShortcutQuery query=new LauncherApps.ShortcutQuery()
                    .setPackage(app.packageName)
                    .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
            List<ShortcutInfo> current=launcherApps.getShortcuts(query,app.user);
            ArrayList<String> ids=new ArrayList<>();
            if(current!=null)for(ShortcutInfo s:current)ids.add(s.getId());
            if(!ids.contains(shortcut.getId()))ids.add(shortcut.getId());
            launcherApps.pinShortcuts(app.packageName,ids,app.user);
            int page=0,cell=workspaceStore.firstFreeCell(page,4,6);
            if(cell<0){page=workspaceStore.pageCount();workspaceStore.addPage();cell=0;}
            workspaceStore.addShortcut(page,cell%4,cell/4,app.componentName.flattenToString(),
                    shortcut.getId(),String.valueOf(shortcut.getShortLabel()),app.userSerial);
            renderWorkspace();
        } catch(Exception e) {
            Toast.makeText(this,"Could not pin this shortcut.",Toast.LENGTH_SHORT).show();
        }
    }

    private void openFolder(WorkspaceItem folder) {
        String payload=folder.payload==null?"":folder.payload;
        String[] entries=payload.split("\n");
        ArrayList<String> titles=new ArrayList<>();
        ArrayList<String[]> parsed=new ArrayList<>();
        for(String entry:entries) {
            String[] parts=entry.split("\\|",4);
            if(parts.length>=3) {parsed.add(parts);titles.add(parts[2]);}
        }
        if(titles.isEmpty()) {
            Toast.makeText(this,"This folder is empty.",Toast.LENGTH_SHORT).show();return;
        }
        new AlertDialog.Builder(this).setTitle(folder.title==null?"Folder":folder.title)
                .setItems(titles.toArray(new String[0]),(d,which)->{
                    String[] parts=parsed.get(which);
                    long serial;
                    try {serial=Long.parseLong(parts[1]);}catch(Exception e){serial=0;}
                    WorkspaceItem child=new WorkspaceItem(-1,folder.page,0,0,1,1,
                            WorkspaceItem.APP,parts[0],parts[2],parts.length>3?parts[3]:null,-1,serial);
                    if(parts.length>3&&!parts[3].isEmpty())child.type=WorkspaceItem.SHORTCUT;
                    onLaunchItem(child);
                }).setNegativeButton("Close",null).show();
    }

    private void showWidgetPicker() {
        List<AppWidgetProviderInfo> providers=widgetManager.getInstalledProviders();
        Collections.sort(providers,Comparator.comparing(p->String.valueOf(p.loadLabel(getPackageManager()))
                .toLowerCase(Locale.getDefault())));
        if(providers.isEmpty()) {
            Toast.makeText(this,"No widgets are installed on this device.",Toast.LENGTH_SHORT).show();return;
        }
        String[] names=new String[providers.size()];
        for(int i=0;i<providers.size();i++)names[i]=String.valueOf(providers.get(i).loadLabel(getPackageManager()));
        new AlertDialog.Builder(this).setTitle("Add a widget")
                .setItems(names,(dialog,which)->requestWidgetBind(providers.get(which)))
                .setNegativeButton("Cancel",null).show();
    }

    private void requestWidgetBind(AppWidgetProviderInfo provider) {
        int id=widgetHost.allocateAppWidgetId();
        Bundle options=new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,provider.minWidth);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,provider.minHeight);
        try {
            if(widgetManager.bindAppWidgetIdIfAllowed(id,provider.provider,options)) {
                finishWidgetSetup(id);
            } else {
                Intent bind=new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
                bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
                bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,provider.provider);
                bind.putExtra(AppWidgetManager.EXTRA_APPWIDGET_OPTIONS,options);
                pendingWidgetId=id;
                startActivityForResult(bind,REQUEST_BIND_WIDGET);
            }
        } catch(Exception e) {
            widgetHost.deleteAppWidgetId(id);
            Toast.makeText(this,"Unable to bind this widget.",Toast.LENGTH_SHORT).show();
        }
    }

    private void finishWidgetSetup(int id) {
        AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);
        if(info==null){widgetHost.deleteAppWidgetId(id);return;}
        if(info.configure!=null) {
            try {
                Intent configure=new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                configure.setComponent(info.configure);
                configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);
                pendingWidgetId=id;
                startActivityForResult(configure,REQUEST_CONFIGURE_WIDGET);
                return;
            } catch(Exception e) {
                widgetHost.deleteAppWidgetId(id);
                Toast.makeText(this,"Widget configuration could not be opened.",Toast.LENGTH_SHORT).show();return;
            }
        }
        persistWidget(id,info);
    }

    private void persistWidget(int id,AppWidgetProviderInfo info) {
        int page=0,row=workspaceStore.firstFreeWidgetRow(page,2);
        if(row<0){page=workspaceStore.pageCount();workspaceStore.addPage();row=0;}
        String title=String.valueOf(info.loadLabel(getPackageManager()));
        workspaceStore.addWidget(page,0,row,id,info.provider.flattenToString(),title);
        pendingWidgetId=-1;
        renderWorkspace();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_BIND_WIDGET) {
            int id=pendingWidgetId;
            if(resultCode==RESULT_OK&&id>=0)finishWidgetSetup(id);
            else {if(id>=0)widgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;}
        } else if(requestCode==REQUEST_CONFIGURE_WIDGET) {
            int id=pendingWidgetId;
            if(resultCode==RESULT_OK&&id>=0) {
                AppWidgetProviderInfo info=widgetManager.getAppWidgetInfo(id);
                if(info!=null)persistWidget(id,info);
                else {widgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;}
            } else {if(id>=0)widgetHost.deleteAppWidgetId(id);pendingWidgetId=-1;}
        } else if(requestCode==4101) {
            renderWorkspace();
        }
    }

    @Override public void onBackPressed() {
        if(homeView!=null&&homeView.isDrawerOpen()) {
            // Dialog owns Back while open; fall through when already dismissed.
        }
        super.onBackPressed();
    }

    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
