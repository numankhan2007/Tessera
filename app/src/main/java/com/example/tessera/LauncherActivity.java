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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;

/** Android HOME activity. All app launches and widgets use platform launcher APIs. */
public final class LauncherActivity extends Activity implements TesseraHomeView.Callbacks {
    private static final int WIDGET_HOST_ID=0x544553;
    private static final int REQUEST_BIND_WIDGET=4102;
    private static final int REQUEST_CONFIGURE_WIDGET=4103;
    private static final int REQUEST_EXPORT_WORKSPACE=4201;
    private static final int REQUEST_IMPORT_WORKSPACE=4202;

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
    private volatile boolean activityDestroyed;

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
        activityDestroyed=true;
        unregisterPackageCallback();
        if(workspaceStore!=null)workspaceStore.close();
        super.onDestroy();
    }

    private void registerPackageCallback() {
        if(launcherApps==null||callbackRegistered)return;
        packageCallback=new LauncherApps.Callback() {
            @Override public void onPackageRemoved(String p,UserHandle u){
                long serial;
                try {serial=userManager.getSerialNumberForUser(u);} catch(Exception e) {serial=u.hashCode();}
                workspaceStore.removePackage(p,serial);
                refreshApps();
            }
            @Override public void onPackageAdded(String p,UserHandle u){refreshApps();}
            @Override public void onPackageChanged(String p,UserHandle u){refreshApps();}
            @Override public void onPackagesAvailable(String[] p,UserHandle u,boolean replacing){refreshApps();}
            @Override public void onPackagesUnavailable(String[] p,UserHandle u,boolean replacing){refreshApps();}
            @Override public void onPackagesSuspended(String[] p,UserHandle u){refreshApps();}
            @Override public void onPackagesUnsuspended(String[] p,UserHandle u){refreshApps();}
            @Override public void onShortcutsChanged(String p,List<ShortcutInfo> shortcuts,UserHandle u){
                runOnUiThread(()->renderWorkspace());
            }
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
                runOnUiThread(()->{
                if(!activityDestroyed&&!isFinishing())
                    Toast.makeText(this,"Tessera could not read the app catalogue.",Toast.LENGTH_LONG).show();
            });
            }
            runOnUiThread(()->{
                if(activityDestroyed||isFinishing()||workspaceStore==null)return;
                installedApps=result;
                workspaceStore.seedIfEmpty(installedApps);
                renderWorkspace();
            });
        },"Tessera-AppCatalog").start();
    }

    private void renderWorkspace() {
        if(activityDestroyed||homeView==null||workspaceStore==null)return;
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
                    launcherApps.startShortcut(parent.packageName,item.payload,(Rect)null,
                            ActivityOptions.makeBasic().toBundle(),parent.user);
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

    @Override public void onRenameFolder(WorkspaceItem item,String title) {
        if(item!=null&&WorkspaceItem.FOLDER.equals(item.type)) {
            workspaceStore.renameFolder(item.id,title);
            renderWorkspace();
        }
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
            x=Math.max(0,Math.min(4-item.spanX,x));
            y=Math.max(0,Math.min(6-item.spanY,y));
        }
        WorkspaceItem target=workspaceStore.findAt(page,x,y,item.id);
        if(target!=null&&target.id!=item.id) {
            if(WorkspaceItem.WIDGET.equals(target.type)||WorkspaceItem.WIDGET.equals(item.type)) {
                Toast.makeText(this,"That space is occupied by a widget.",Toast.LENGTH_SHORT).show();return;
            }
            if(WorkspaceItem.FOLDER.equals(item.type)) {
                int free=workspaceStore.firstFreeCell(page,4,6);
                if(free<0){Toast.makeText(this,"This page is full.",Toast.LENGTH_SHORT).show();return;}
                workspaceStore.move(item.id,page,free%4,free/4);
            } else {
                workspaceStore.mergeInto(item.id,target.id);
            }
        } else {
            workspaceStore.move(item.id,page,x,y);
        }
        renderWorkspace();
    }

    @Override public void onResizeWidget(WorkspaceItem item,int spanX,int spanY) {
        if(item==null)return;
        spanX=Math.max(1,Math.min(4,spanX));
        spanY=Math.max(1,Math.min(4,spanY));
        int x=Math.min(item.cellX,4-spanX),y=Math.min(item.cellY,6-spanY);
        if(workspaceStore.hasOverlap(item.page,x,y,spanX,spanY,item.id)) {
            Toast.makeText(this,"There is not enough free space to resize this widget.",Toast.LENGTH_SHORT).show();
            return;
        }
        workspaceStore.resizeWidget(item.id,spanX,spanY);
        workspaceStore.move(item.id,item.page,x,y);
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

    @Override public void onBackupWorkspace() {
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE,"Tessera-workspace-backup.json");
        startActivityForResult(intent,REQUEST_EXPORT_WORKSPACE);
    }

    @Override public void onRestoreWorkspace() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent,REQUEST_IMPORT_WORKSPACE);
    }

    private void writeWorkspaceBackup(Uri uri) {
        try(OutputStream out=getContentResolver().openOutputStream(uri);
            OutputStreamWriter writer=new OutputStreamWriter(out,StandardCharsets.UTF_8)) {
            writer.write(workspaceStore.exportJson());
            writer.flush();
            Toast.makeText(this,"Workspace backup exported.",Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"Could not export the workspace backup.",Toast.LENGTH_LONG).show();
        }
    }

    private void readWorkspaceBackup(Uri uri) {
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(
                getContentResolver().openInputStream(uri),StandardCharsets.UTF_8))) {
            StringBuilder json=new StringBuilder();
            String line;
            while((line=reader.readLine())!=null)json.append(line).append('\n');
            for(WorkspaceItem item:workspaceStore.all())if(WorkspaceItem.WIDGET.equals(item.type)) {
                try {widgetHost.deleteAppWidgetId(item.widgetId);} catch(RuntimeException ignored) {}
            }
            widgetViews.clear();
            int restored=workspaceStore.restoreJson(json.toString());
            int skipped=workspaceStore.getSkippedWidgetCount();
            renderWorkspace();
            String message="Restored "+restored+" workspace items.";
            if(skipped>0)message+=" Re-add "+skipped+" widgets (Android permissions are device-specific).";
            Toast.makeText(this,message,Toast.LENGTH_LONG).show();
        } catch(Exception e) {
            Toast.makeText(this,"Invalid or unreadable Tessera backup.",Toast.LENGTH_LONG).show();
        }
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
        if(launcherApps==null) {
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
        float density=getResources().getDisplayMetrics().density;
        float availableWidth=getResources().getDisplayMetrics().widthPixels/density-40f;
        int spanX=Math.max(1,Math.min(4,(int)Math.ceil(info.minWidth/Math.max(1f,availableWidth/4f))));
        int spanY=Math.max(1,Math.min(4,(int)Math.ceil(info.minHeight/82f)));
        int page=0,cell=workspaceStore.firstFreeSpanCell(page,spanX,spanY);
        if(cell<0){page=workspaceStore.pageCount();workspaceStore.addPage();cell=0;}
        String title=String.valueOf(info.loadLabel(getPackageManager()));
        workspaceStore.addWidget(page,cell%4,cell/4,id,info.provider.flattenToString(),title,spanX,spanY);
        pendingWidgetId=-1;
        renderWorkspace();
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQUEST_EXPORT_WORKSPACE) {
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)writeWorkspaceBackup(data.getData());
            return;
        } else if(requestCode==REQUEST_IMPORT_WORKSPACE) {
            if(resultCode==RESULT_OK&&data!=null&&data.getData()!=null)readWorkspaceBackup(data.getData());
            return;
        }
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
