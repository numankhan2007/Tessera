package com.example.tessera;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Native, responsive launcher workspace and searchable app drawer. */
public final class TesseraHomeView extends LinearLayout {
    public interface Callbacks {
        void onLaunchItem(WorkspaceItem item);
        void onAppSelected(LauncherApp app);
        void onAppLongPressed(LauncherApp app,View anchor);
        void onPinApp(LauncherApp app);
        void onAddWidgetRequested();
        void onAddPageRequested();
        void onRemoveItem(WorkspaceItem item);
        void onRenameFolder(WorkspaceItem item,String title);
        void onDropItem(WorkspaceItem item,int page,int cellX,int cellY);
        void onResizeWidget(WorkspaceItem item,int spanX,int spanY);
        void onSetHomeRequested();
        void onOpenSystemWallpaper();
        void onBackupWorkspace();
        void onRestoreWorkspace();
        void onReducedMotionChanged(boolean enabled);
    }

    private static final int BG=0xee10251e, PANEL=0xd9234035, IVORY=0xfff7f2e1,
            GOLD=0xffe6c75c, MINT=0xff8fd1ad, MUTED=0xffb7c7be;
    private final Context context;
    private final Callbacks callbacks;
    private final LinearLayout pageStrip;
    private final HorizontalScrollView pager;
    private final LinearLayout dock;
    private final TextView pageIndicator;
    private final TextView clock;
    private final TextView homeHint;
    private List<LauncherApp> apps=new ArrayList<>();
    private List<WorkspaceItem> items=new ArrayList<>();
    private Map<Integer,View> widgetViews;
    private int pageCount=1,currentPage=0;
    private boolean defaultHome,reducedMotion;
    private Dialog drawerDialog;

    public TesseraHomeView(Context c,Callbacks cb) {
        super(c);
        context=c;callbacks=cb;
        setOrientation(VERTICAL);
        setPadding(dp(18),dp(12),dp(18),dp(10));
        setBackgroundColor(Color.TRANSPARENT);
        setFocusableInTouchMode(true);

        LinearLayout brandRow=new LinearLayout(c);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("TESSERA",12,MINT,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        brand.setLetterSpacing(.22f);
        brandRow.addView(brand,new LayoutParams(0,dp(32),1));
        clock=label("",13,IVORY,Typeface.create("serif",Typeface.NORMAL));
        brandRow.addView(clock,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(32)));
        TextView add=button("＋",GOLD);
        add.setContentDescription("Workspace options");
        add.setOnClickListener(v->showWorkspaceMenu(v));
        brandRow.addView(add,new LayoutParams(dp(42),dp(42)));
        addView(brandRow,new LayoutParams(-1,dp(48)));

        TextView title=label("A quieter kind of everyday.",26,IVORY,Typeface.create("serif",Typeface.ITALIC));
        title.setPadding(dp(4),dp(6),0,0);
        addView(title,new LayoutParams(-1,dp(50)));
        TextView subtitle=label("YOUR UNIVERSE",10,GOLD,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        subtitle.setLetterSpacing(.16f);
        subtitle.setPadding(dp(4),0,0,0);
        addView(subtitle,new LayoutParams(-1,dp(28)));

        if(!defaultHome) {
            homeHint=button("Set Tessera as your Home app  ↗",MINT);
            homeHint.setOnClickListener(v->callbacks.onSetHomeRequested());
            addView(homeHint,new LayoutParams(-1,dp(42)));
        } else homeHint=null;

        pager=new HorizontalScrollView(c);
        pager.setHorizontalScrollBarEnabled(false);
        pager.setFillViewport(true);
        pageStrip=new LinearLayout(c);
        pageStrip.setOrientation(HORIZONTAL);
        pager.addView(pageStrip,new HorizontalScrollView.LayoutParams(-2,-1));
        addView(pager,new LayoutParams(-1,0,1f));

        pageIndicator=label("1 / 1",11,MUTED,Typeface.create("sans-serif",Typeface.NORMAL));
        pageIndicator.setGravity(Gravity.CENTER);
        addView(pageIndicator,new LayoutParams(-1,dp(24)));

        LinearLayout bottom=new LinearLayout(c);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOrientation(HORIZONTAL);
        dock=new LinearLayout(c);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(HORIZONTAL);
        dock.setBackground(round(PANEL,dp(20),0x557ac7a1));
        bottom.addView(dock,new LayoutParams(0,dp(82),1f));
        TextView all=button("⌕",GOLD);
        all.setTextSize(26);
        all.setContentDescription("Open app drawer");
        all.setOnClickListener(v->openDrawer());
        LinearLayout.LayoutParams allLp=new LayoutParams(dp(58),dp(58));
        allLp.leftMargin=dp(8);
        bottom.addView(all,allLp);
        addView(bottom,new LayoutParams(-1,dp(88)));

        pager.setOnScrollChangeListener((View v,int sx,int sy,int ox,int oy)->{
            int w=Math.max(1,pager.getWidth());
            currentPage=Math.max(0,Math.min(pageCount-1,Math.round((float)sx/w)));
            updateIndicator();
        });
        updateClock();
    }

    public void render(List<WorkspaceItem> saved,List<LauncherApp> installed,int pages,
                       boolean isDefault,Map<Integer,View> widgets,boolean reduceMotion) {
        items=saved==null?new ArrayList<>():new ArrayList<>(saved);
        apps=installed==null?new ArrayList<>():new ArrayList<>(installed);
        pageCount=Math.max(1,pages);defaultHome=isDefault;widgetViews=widgets;reducedMotion=reduceMotion;
        if(homeHint!=null)homeHint.setVisibility(defaultHome?GONE:VISIBLE);
        pageStrip.removeAllViews();dock.removeAllViews();
        int width=Math.max(dp(300),getResources().getDisplayMetrics().widthPixels-getPaddingLeft()-getPaddingRight());
        for(int page=0;page<pageCount;page++) {
            final int pageIndex=page;
            LinearLayout column=new LinearLayout(context);
            column.setOrientation(VERTICAL);
            column.setPadding(dp(4),dp(8),dp(4),dp(8));
            pageStrip.addView(column,new LayoutParams(width,LayoutParams.MATCH_PARENT));
            TextView pageTitle=label(String.format(Locale.getDefault(),"SPACE  %02d",page+1),9,MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            pageTitle.setLetterSpacing(.18f);
            pageTitle.setPadding(dp(8),0,0,dp(8));
            column.addView(pageTitle,new LayoutParams(-1,dp(24)));
            GridLayout grid=new GridLayout(context);
            grid.setColumnCount(4);grid.setRowCount(6);
            grid.setUseDefaultMargins(false);
            grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
            grid.setColumnOrderPreserved(false);
            grid.setBackground(round(0x142e5947,dp(24),0x227ac7a1));
            grid.setPadding(dp(3),dp(4),dp(3),dp(4));
            grid.setTag(page);
            grid.setOnDragListener((v,e)->handleDrop(v,e,pageIndex,false));
            column.addView(grid,new LayoutParams(-1,dp(510)));
            for(WorkspaceItem item:items) if(item.page==page) addWorkspaceChild(grid,item);
            TextView addSpace=button(page==pageCount-1?"+  Add another space":"·  ·  ·",MUTED);
            addSpace.setTextSize(12);
            addSpace.setOnClickListener(v->{if(pageIndex==pageCount-1)callbacks.onAddPageRequested();});
            column.addView(addSpace,new LayoutParams(-1,dp(34)));
        }

        List<WorkspaceItem> dockItems=new ArrayList<>();
        for(WorkspaceItem i:items)if(i.page==-1)dockItems.add(i);
        for(WorkspaceItem item:dockItems)addDockChild(item);
        if(dockItems.isEmpty()) {
            TextView empty=label("Pin apps here",11,MUTED,Typeface.create("serif",Typeface.ITALIC));
            empty.setGravity(Gravity.CENTER);
            dock.addView(empty,new LayoutParams(-1,-1));
        }
        dock.setOnDragListener((v,e)->handleDrop(v,e, -1,true));
        updateIndicator();
        pager.post(()->pager.scrollTo(currentPage*pager.getWidth(),0));
    }

    private void addWorkspaceChild(GridLayout grid,WorkspaceItem item) {
        int x=Math.max(0,Math.min(3,item.cellX)),y=Math.max(0,Math.min(5,item.cellY));
        int sx=Math.max(1,Math.min(4-x,item.spanX)),sy=Math.max(1,Math.min(6-y,item.spanY));
        View child;
        if(WorkspaceItem.WIDGET.equals(item.type)) {
            FrameLayout wrapper=new FrameLayout(context);
            wrapper.setBackground(round(0x44233d34,dp(18),0x6687bba0));
            View host=widgetViews==null?null:widgetViews.get(item.widgetId);
            if(host!=null) {
                if(host.getParent() instanceof ViewGroup)((ViewGroup)host.getParent()).removeView(host);
                wrapper.addView(host,new FrameLayout.LayoutParams(-1,-1));
            } else {
                TextView missing=label("Widget unavailable",11,MUTED,Typeface.create("sans-serif",Typeface.NORMAL));
                missing.setGravity(Gravity.CENTER);wrapper.addView(missing);
            }
            wrapper.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
            child=wrapper;
        } else {
            child=createTile(item);
        }
        GridLayout.LayoutParams lp=new GridLayout.LayoutParams(
                GridLayout.spec(y,sy),GridLayout.spec(x,sx,1f));
        lp.width=0;lp.height=dp(82*sy);lp.setMargins(dp(2),dp(2),dp(2),dp(2));
        grid.addView(child,lp);
    }

    private View createTile(WorkspaceItem item) {
        LinearLayout tile=new LinearLayout(context);
        tile.setOrientation(VERTICAL);tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2),dp(4),dp(2),dp(2));
        tile.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout iconFrame=new FrameLayout(context);
        iconFrame.setBackground(round(colorFor(item),dp(16),0x447ac7a1));
        ImageView icon=new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable drawable=findIcon(item);
        if(drawable!=null)icon.setImageDrawable(drawable);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(42),dp(42),Gravity.CENTER);
        iconFrame.addView(icon,ip);
        tile.addView(iconFrame,new LayoutParams(dp(54),dp(54)));
        String title=WorkspaceItem.FOLDER.equals(item.type)?"Folder":item.title;
        TextView name=label(title==null?"App":title,10.5f,IVORY,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(name,new LayoutParams(-1,dp(30)));
        tile.setContentDescription(title==null?"Workspace item":title);
        tile.setOnClickListener(v->callbacks.onLaunchItem(item));
        tile.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
        tile.setOnDragListener((v,e)->{
            if(e.getAction()==android.view.DragEvent.ACTION_DROP) {
                Object state=e.getLocalState();
                if(state instanceof WorkspaceItem) {
                    WorkspaceItem moving=(WorkspaceItem)state;
                    if(moving.id!=item.id)callbacks.onDropItem(moving,item.page,item.cellX,item.cellY);
                }
                return true;
            }
            return true;
        });
        tile.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
        return tile;
    }

    private LauncherApp findApp(WorkspaceItem item) {
        if(item.component==null)return null;
        for(LauncherApp app:apps)if(item.component.equals(app.componentName.flattenToString())
                &&item.userSerial==app.userSerial)return app;
        return null;
    }

    private Drawable findIcon(WorkspaceItem item) {
        if(item.component==null||apps==null)return null;
        for(LauncherApp app:apps)if(app.componentName.flattenToString().equals(item.component)
                &&app.userSerial==item.userSerial)return app.icon;
        return null;
    }

    private void addDockChild(WorkspaceItem item) {
        LinearLayout tile=new LinearLayout(context);
        tile.setOrientation(VERTICAL);tile.setGravity(Gravity.CENTER);
        ImageView icon=new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable d=findIcon(item);
        if(d!=null)icon.setImageDrawable(d);else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        tile.addView(icon,new LayoutParams(dp(38),dp(38)));
        TextView name=label(item.title==null?"App":item.title,9,IVORY,Typeface.create("sans-serif",Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);name.setMaxLines(1);
        tile.addView(name,new LayoutParams(-1,dp(20)));
        tile.setContentDescription(item.title);
        tile.setOnClickListener(v->callbacks.onLaunchItem(item));
        tile.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
        LayoutParams lp=new LayoutParams(0,-1,1f);
        dock.addView(tile,lp);
    }

    private boolean handleDrop(View view,android.view.DragEvent e,int page,boolean inDock) {
        switch(e.getAction()) {
            case android.view.DragEvent.ACTION_DRAG_STARTED:return e.getLocalState() instanceof WorkspaceItem;
            case android.view.DragEvent.ACTION_DRAG_ENTERED:view.setAlpha(.78f);return true;
            case android.view.DragEvent.ACTION_DRAG_EXITED:view.setAlpha(1f);return true;
            case android.view.DragEvent.ACTION_DROP:
                view.setAlpha(1f);
                Object state=e.getLocalState();
                if(!(state instanceof WorkspaceItem))return false;
                WorkspaceItem item=(WorkspaceItem)state;
                int x,y;
                if(inDock) {
                    x=Math.max(0,Math.min(4,(int)(e.getX()/Math.max(1f,view.getWidth()/5f))));y=0;
                } else {
                    x=Math.max(0,Math.min(3,(int)(e.getX()/Math.max(1f,view.getWidth()/4f))));
                    y=Math.max(0,Math.min(5,(int)(e.getY()/Math.max(1f,view.getHeight()/6f))));
                }
                callbacks.onDropItem(item,page,x,y);return true;
            case android.view.DragEvent.ACTION_DRAG_ENDED:view.setAlpha(1f);return true;
        }
        return true;
    }

    private void showItemMenu(WorkspaceItem item,View anchor) {
        PopupMenu menu=new PopupMenu(context,anchor);
        menu.getMenu().add("Move / Reposition").setOnMenuItemClickListener(mi->{
            ClipData data=ClipData.newPlainText("tessera-item",String.valueOf(item.id));
            anchor.startDragAndDrop(data,new View.DragShadowBuilder(anchor),item,0);
            return true;
        });
        if(WorkspaceItem.FOLDER.equals(item.type)) {
            menu.getMenu().add("Open folder").setOnMenuItemClickListener(mi->{callbacks.onLaunchItem(item);return true;});
            menu.getMenu().add("Rename folder").setOnMenuItemClickListener(mi->{
                EditText name=new EditText(context);name.setSingleLine(true);name.setText(item.title==null?"Folder":item.title);
                new AlertDialog.Builder(context).setTitle("Rename folder").setView(name)
                        .setPositiveButton("Save",(d,w)->{
                            String value=name.getText().toString().trim();
                            if(!value.isEmpty())callbacks.onRenameFolder(item,value);
                        }).setNegativeButton("Cancel",null).show();
                return true;
            });
        }
        if(WorkspaceItem.APP.equals(item.type)||WorkspaceItem.SHORTCUT.equals(item.type)) {
            menu.getMenu().add("App info & shortcuts").setOnMenuItemClickListener(mi->{
                LauncherApp app=findApp(item);
                if(app!=null)callbacks.onAppLongPressed(app,anchor);
                return true;
            });
        }
        if(WorkspaceItem.WIDGET.equals(item.type)) {
            menu.getMenu().add("Wider").setOnMenuItemClickListener(mi->{callbacks.onResizeWidget(item,Math.min(4,item.spanX+1),item.spanY);return true;});
            menu.getMenu().add("Narrower").setOnMenuItemClickListener(mi->{callbacks.onResizeWidget(item,Math.max(1,item.spanX-1),item.spanY);return true;});
            menu.getMenu().add("Make taller").setOnMenuItemClickListener(mi->{callbacks.onResizeWidget(item,item.spanX,Math.min(4,item.spanY+1));return true;});
            menu.getMenu().add("Make shorter").setOnMenuItemClickListener(mi->{callbacks.onResizeWidget(item,item.spanX,Math.max(1,item.spanY-1));return true;});
        }
        menu.getMenu().add("Remove from Home").setOnMenuItemClickListener(mi->{callbacks.onRemoveItem(item);return true;});
        menu.show();
    }

    private void showWorkspaceMenu(View anchor) {
        PopupMenu menu=new PopupMenu(context,anchor);
        menu.getMenu().add("Add application").setOnMenuItemClickListener(mi->{openDrawer();return true;});
        menu.getMenu().add("Add widget").setOnMenuItemClickListener(mi->{callbacks.onAddWidgetRequested();return true;});
        menu.getMenu().add("New workspace page").setOnMenuItemClickListener(mi->{callbacks.onAddPageRequested();return true;});
        menu.getMenu().add("Wallpaper").setOnMenuItemClickListener(mi->{callbacks.onOpenSystemWallpaper();return true;});
        menu.getMenu().add("Back up workspace").setOnMenuItemClickListener(mi->{callbacks.onBackupWorkspace();return true;});
        menu.getMenu().add("Restore workspace").setOnMenuItemClickListener(mi->{callbacks.onRestoreWorkspace();return true;});
        menu.getMenu().add("Motion & accessibility").setOnMenuItemClickListener(mi->{showAccessibilitySettings();return true;});
        menu.show();
    }

    private void showAccessibilitySettings() {
        android.widget.Switch motion=new android.widget.Switch(context);
        motion.setText("Reduce launcher motion");
        motion.setTextColor(IVORY);motion.setChecked(reducedMotion);
        LinearLayout box=new LinearLayout(context);box.setPadding(dp(22),dp(16),dp(22),dp(12));
        box.addView(motion);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle("Accessibility")
                .setView(box).setPositiveButton("Done",(d,w)->callbacks.onReducedMotionChanged(motion.isChecked()))
                .setNegativeButton("Cancel",null).create();
        dialog.show();
    }

    public void openDrawer() {
        if(drawerDialog!=null&&drawerDialog.isShowing())return;
        Dialog dialog=new Dialog(context);
        LinearLayout root=new LinearLayout(context);root.setOrientation(VERTICAL);
        root.setPadding(dp(20),dp(18),dp(20),dp(18));root.setBackground(round(BG,dp(28),0x667ac7a1));
        LinearLayout head=new LinearLayout(context);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=label("YOUR UNIVERSE",11,MINT,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        title.setLetterSpacing(.18f);head.addView(title,new LayoutParams(0,dp(34),1f));
        TextView close=button("×",IVORY);close.setOnClickListener(v->dialog.dismiss());
        head.addView(close,new LayoutParams(dp(42),dp(42)));root.addView(head);
        TextView sub=label("Everything, in its place.",27,IVORY,Typeface.create("serif",Typeface.ITALIC));
        root.addView(sub,new LayoutParams(-1,dp(48)));
        EditText search=new EditText(context);
        search.setSingleLine(true);search.setTextColor(IVORY);search.setHintTextColor(MUTED);
        search.setHint("Find an app");search.setTextSize(15);
        search.setBackground(round(0xaa2c493d,dp(18),0x447ac7a1));
        search.setPadding(dp(16),0,dp(16),0);
        root.addView(search,new LayoutParams(-1,dp(52)));
        ScrollView scroller=new ScrollView(context);
        LinearLayout rows=new LinearLayout(context);rows.setOrientation(VERTICAL);
        scroller.addView(rows);
        root.addView(scroller,new LayoutParams(-1,0,1f));
        Runnable refresh=()->{
            String q=search.getText().toString().trim().toLowerCase(Locale.getDefault());
            rows.removeAllViews();
            for(LauncherApp app:apps) {
                if(!q.isEmpty()&&!app.label.toLowerCase(Locale.getDefault()).contains(q))continue;
                LinearLayout row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4),dp(7),dp(4),dp(7));
                ImageView icon=new ImageView(context);icon.setImageDrawable(app.icon);
                row.addView(icon,new LayoutParams(dp(42),dp(42)));
                TextView name=label(app.label,14,IVORY,Typeface.create("sans-serif-medium",Typeface.NORMAL));
                name.setPadding(dp(12),0,0,0);row.addView(name,new LayoutParams(0,dp(48),1f));
                TextView pin=button("+",GOLD);pin.setContentDescription("Pin "+app.label+" to Home");
                pin.setOnClickListener(v->{callbacks.onPinApp(app);dialog.dismiss();});
                row.addView(pin,new LayoutParams(dp(42),dp(42)));
                row.setOnClickListener(v->{dialog.dismiss();callbacks.onAppSelected(app);});
                row.setOnLongClickListener(v->{dialog.dismiss();callbacks.onAppLongPressed(app,v);return true;});
                rows.addView(row,new LayoutParams(-1,dp(58)));
                View line=new View(context);line.setBackgroundColor(0x227ac7a1);
                rows.addView(line,new LayoutParams(-1,dp(1)));
            }
            if(rows.getChildCount()==0) {
                TextView empty=label("No matching apps",14,MUTED,Typeface.create("serif",Typeface.ITALIC));
                empty.setGravity(Gravity.CENTER);rows.addView(empty,new LayoutParams(-1,dp(90)));
            }
        };
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}
            public void onTextChanged(CharSequence s,int st,int before,int count){refresh.run();}
            public void afterTextChanged(Editable e){}
        });
        refresh.run();
        dialog.setContentView(root);
        Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(getResources().getDisplayMetrics().heightPixels*.86f));
            window.setGravity(Gravity.BOTTOM);}
        dialog.show();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(getResources().getDisplayMetrics().heightPixels*.86f));
            window.setGravity(Gravity.BOTTOM);}
        drawerDialog=dialog;
        dialog.setOnDismissListener(d->{drawerDialog=null;});
        search.requestFocus();
    }

    public boolean isDrawerOpen() {
        return drawerDialog!=null && drawerDialog.isShowing();
    }

    private void updateClock() {
        clock.setText(new SimpleDateFormat("h:mm  ·  EEE, d MMM",Locale.getDefault()).format(new Date()));
        postDelayed(this::updateClock,60000);
    }
    private void updateIndicator(){pageIndicator.setText((currentPage+1)+" / "+pageCount);}
    private TextView label(String text,float size,int color,Typeface face) {
        TextView v=new TextView(context);v.setText(text);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(face);v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private TextView button(String text,int color) {
        TextView v=label(text,18,color,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);v.setBackground(round(0x552c493d,dp(16),0x447ac7a1));
        v.setClickable(true);v.setFocusable(true);return v;
    }
    private GradientDrawable round(int color,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);
        if(stroke!=0)d.setStroke(dp(1),stroke);return d;
    }
    private int colorFor(WorkspaceItem i) {
        if(WorkspaceItem.FOLDER.equals(i.type))return 0xff816f43;
        if(WorkspaceItem.SHORTCUT.equals(i.type))return 0xff526c7e;
        return 0xff476f5c;
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
