package com.example.tessera;

import android.app.AlertDialog;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
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
@SuppressLint("ViewConstructor")
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
        void onCurrentPageChanged(int page);
    }

    // Obsidian Garden palette: near-black canvas, deep pine surfaces, warm ivory and restrained brass.
    private static final int BG=0xff0b120f, PANEL=0xff14231c, SURFACE=0xff192b22,
            SURFACE_RAISED=0xff20372b, BORDER=0xff345443, IVORY=0xfff4f1e6,
            GOLD=0xffe6c75c, MINT=0xff9bd8b7, MUTED=0xffaabbb1;
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
    private final Runnable clockTicker=()->updateClock();

    public TesseraHomeView(Context c,Callbacks cb) {
        super(c);
        context=c;callbacks=cb;
        setOrientation(VERTICAL);
        setPadding(dp(20),dp(10),dp(20),dp(10));
        setBackgroundColor(Color.TRANSPARENT);
        setFocusableInTouchMode(true);
        setClipChildren(false);

        LinearLayout brandRow=new LinearLayout(c);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("T E S S E R A",11,MINT,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        brand.setLetterSpacing(.12f);
        brandRow.addView(brand,new LayoutParams(0,dp(40),1f));
        clock=label("",13,IVORY,Typeface.create("sans-serif",Typeface.NORMAL));
        brandRow.addView(clock,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(40)));
        TextView add=button("＋",GOLD);
        add.setTextSize(23);
        add.setBackground(rippleSurface(SURFACE,0x559bd8b7,dp(18),BORDER));
        add.setContentDescription("Workspace options");
        add.setOnClickListener(v->showWorkspaceMenu(v));
        LinearLayout.LayoutParams addLp=new LayoutParams(dp(46),dp(46));
        addLp.leftMargin=dp(12);
        brandRow.addView(add,addLp);
        addView(brandRow,new LayoutParams(-1,dp(50)));

        TextView title=label("Make room for wonder.",25,IVORY,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        title.setPadding(dp(2),dp(5),0,0);
        addView(title,new LayoutParams(-1,dp(42)));
        TextView subtitle=label("YOUR SPACE, COMPOSED",10,GOLD,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        subtitle.setLetterSpacing(.2f);
        subtitle.setPadding(dp(3),0,0,0);
        addView(subtitle,new LayoutParams(-1,dp(25)));

        if(!defaultHome) {
            homeHint=button("Set Tessera as your Home app   ↗",MINT);
            homeHint.setTextSize(12);
            homeHint.setBackground(rippleSurface(SURFACE,0x449bd8b7,dp(14),BORDER));
            homeHint.setOnClickListener(v->callbacks.onSetHomeRequested());
            LayoutParams hintLp=new LayoutParams(-1,dp(40));
            hintLp.topMargin=dp(4);
            addView(homeHint,hintLp);
        } else homeHint=null;

        pager=new HorizontalScrollView(c);
        pager.setHorizontalScrollBarEnabled(false);
        pager.setFillViewport(true);
        pager.setOverScrollMode(OVER_SCROLL_NEVER);
        pageStrip=new LinearLayout(c);
        pageStrip.setOrientation(HORIZONTAL);
        pageStrip.setClipChildren(false);
        pager.addView(pageStrip,new HorizontalScrollView.LayoutParams(-2,-1));
        LayoutParams pagerLp=new LayoutParams(-1,0,1f);
        pagerLp.topMargin=dp(4);
        addView(pager,pagerLp);

        pageIndicator=label("01  /  01",10,MUTED,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        pageIndicator.setGravity(Gravity.CENTER);
        pageIndicator.setLetterSpacing(.12f);
        addView(pageIndicator,new LayoutParams(-1,dp(24)));

        LinearLayout bottom=new LinearLayout(c);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOrientation(HORIZONTAL);
        dock=new LinearLayout(c);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(HORIZONTAL);
        dock.setPadding(dp(6),dp(5),dp(6),dp(5));
        dock.setBackground(rippleSurface(PANEL,0x223bdc9b,dp(24),BORDER));
        dock.setElevation(dp(8));
        dock.setClipToOutline(true);
        bottom.addView(dock,new LayoutParams(0,dp(76),1f));
        TextView all=button("⌕",GOLD);
        all.setTextSize(28);
        all.setContentDescription("Open app drawer");
        all.setBackground(rippleSurface(SURFACE,0x559bd8b7,dp(18),BORDER));
        all.setOnClickListener(v->openDrawer());
        LinearLayout.LayoutParams allLp=new LayoutParams(dp(58),dp(58));
        allLp.leftMargin=dp(10);
        bottom.addView(all,allLp);
        LayoutParams bottomLp=new LayoutParams(-1,dp(80));
        bottomLp.topMargin=dp(2);
        addView(bottom,bottomLp);

        pager.setOnScrollChangeListener((View v,int sx,int sy,int ox,int oy)->{
            int w=Math.max(1,pager.getWidth());
            int nextPage=Math.max(0,Math.min(pageCount-1,Math.round((float)sx/w)));
            if(nextPage!=currentPage) {
                currentPage=nextPage;
                callbacks.onCurrentPageChanged(currentPage);
                pageIndicator.setAlpha(.55f);
                pageIndicator.animate().alpha(1f).setDuration(220).start();
            }
            updateIndicator();
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateClock();
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(clockTicker);
        if(drawerDialog!=null&&drawerDialog.isShowing())drawerDialog.dismiss();
        super.onDetachedFromWindow();
    }

    public void render(List<WorkspaceItem> saved,List<LauncherApp> installed,int pages,
                       boolean isDefault,Map<Integer,View> widgets,boolean reduceMotion) {
        items=saved==null?new ArrayList<>():new ArrayList<>(saved);
        apps=installed==null?new ArrayList<>():new ArrayList<>(installed);
        pageCount=Math.max(1,pages);
        currentPage=Math.max(0,Math.min(pageCount-1,currentPage));
        defaultHome=isDefault;widgetViews=widgets;reducedMotion=reduceMotion;
        if(homeHint!=null)homeHint.setVisibility(defaultHome?GONE:VISIBLE);
        pageStrip.removeAllViews();dock.removeAllViews();
        int width=Math.max(dp(300),getResources().getDisplayMetrics().widthPixels-getPaddingLeft()-getPaddingRight());
        for(int page=0;page<pageCount;page++) {
            final int pageIndex=page;
            LinearLayout column=new LinearLayout(context);
            column.setOrientation(VERTICAL);
            column.setPadding(dp(2),dp(7),dp(2),dp(6));
            column.setClipChildren(false);
            pageStrip.addView(column,new LayoutParams(width,LayoutParams.MATCH_PARENT));

            LinearLayout section=new LinearLayout(context);
            section.setGravity(Gravity.CENTER_VERTICAL);
            TextView pageTitle=label(String.format(Locale.getDefault(),"SPACE  %02d",page+1),10,MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            pageTitle.setLetterSpacing(.16f);
            section.addView(pageTitle,new LayoutParams(0,dp(28),1f));
            int appCount=0;
            for(WorkspaceItem item:items)if(item.page==page)appCount++;
            TextView count=label(appCount+" ITEMS",9,0xff82998c,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            count.setLetterSpacing(.1f);
            section.addView(count,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(28)));
            column.addView(section,new LayoutParams(-1,dp(30)));

            GridLayout grid=new GridLayout(context);
            grid.setColumnCount(4);grid.setRowCount(6);
            grid.setUseDefaultMargins(false);
            grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
            grid.setColumnOrderPreserved(false);
            grid.setBackground(round(0x1b1c3328,dp(26),0x253d5b48));
            grid.setPadding(dp(3),dp(7),dp(3),dp(5));
            grid.setClipChildren(false);
            grid.setTag(page);
            grid.setOnDragListener((v,e)->handleDrop(v,e,pageIndex,false));
            column.addView(grid,new LayoutParams(-1,dp(510)));
            for(WorkspaceItem item:items)if(item.page==page)addWorkspaceChild(grid,item);

            TextView addSpace=label(page==pageCount-1?"＋  CREATE A NEW SPACE":"SWIPE TO EXPLORE",9,
                    page==pageCount-1?MINT:0xff6f887a,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            addSpace.setGravity(Gravity.CENTER);
            addSpace.setLetterSpacing(.12f);
            addSpace.setBackground(rippleSurface(0x0014231c,0x223bdc9b,dp(14),0x223d5b48));
            addSpace.setOnClickListener(v->{if(pageIndex==pageCount-1)callbacks.onAddPageRequested();});
            column.addView(addSpace,new LayoutParams(-1,dp(34)));
            animateIn(column,page*55L);
        }

        List<WorkspaceItem> dockItems=new ArrayList<>();
        for(WorkspaceItem i:items)if(i.page==-1)dockItems.add(i);
        for(WorkspaceItem item:dockItems)addDockChild(item);
        if(dockItems.isEmpty()) {
            TextView empty=label("＋   PIN YOUR FAVOURITES",9,MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            empty.setLetterSpacing(.08f);
            empty.setGravity(Gravity.CENTER);
            dock.addView(empty,new LayoutParams(-1,-1));
        }
        dock.setOnDragListener((v,e)->handleDrop(v,e,-1,true));
        updateIndicator();
        pager.post(()->{
            int target=currentPage*pager.getWidth();
            if(reducedMotion)pager.scrollTo(target,0);
            else pager.smoothScrollTo(target,0);
        });
        if(!reducedMotion) {
            dock.setAlpha(0f);dock.setTranslationY(dp(12));
            dock.animate().alpha(1f).translationY(0).setStartDelay(90)
                    .setDuration(420).setInterpolator(new DecelerateInterpolator(1.6f)).start();
        }
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
        animateIn(child,Math.min(360,(grid.getChildCount()-1)*28L));
    }

    private View createTile(WorkspaceItem item) {
        LinearLayout tile=new LinearLayout(context);
        tile.setOrientation(VERTICAL);tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(2),dp(4),dp(2),dp(2));
        tile.setBackground(rippleSurface(0x0014231c,0x334bd68e,dp(17),0x00000000));
        tile.setClipChildren(false);
        FrameLayout iconFrame=new FrameLayout(context);
        iconFrame.setBackground(round(SURFACE,dp(18),BORDER));
        iconFrame.setElevation(dp(3));
        ImageView icon=new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable drawable=findIcon(item);
        if(drawable!=null)icon.setImageDrawable(drawable);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.CENTER);
        iconFrame.addView(icon,ip);
        tile.addView(iconFrame,new LayoutParams(dp(60),dp(60)));
        String title=WorkspaceItem.FOLDER.equals(item.type)?"Folder":item.title;
        TextView name=label(title==null?"App":title,11,IVORY,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(dp(1),dp(2),dp(1),0);
        tile.addView(name,new LayoutParams(-1,dp(28)));
        tile.setContentDescription(title==null?"Workspace item":title);
        tile.setOnClickListener(v->{
            if(!reducedMotion) {
                v.animate().scaleX(.94f).scaleY(.94f).setDuration(90).withEndAction(()->{
                    v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                            .setInterpolator(new DecelerateInterpolator()).start();
                    callbacks.onLaunchItem(item);
                }).start();
            } else callbacks.onLaunchItem(item);
        });
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
        if(d!=null)icon.setImageDrawable(d);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        tile.addView(icon,new LayoutParams(dp(42),dp(42)));
        tile.setContentDescription(item.title==null?"Open application":item.title);
        tile.setBackground(rippleSurface(0x0014231c,0x334bd68e,dp(16),0x00000000));
        tile.setOnClickListener(v->callbacks.onLaunchItem(item));
        tile.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
        LayoutParams lp=new LayoutParams(0,-1,1f);
        dock.addView(tile,lp);
        animateIn(tile,(dock.getChildCount()-1)*35L);
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
        motion.setText(R.string.reduce_launcher_motion);
        motion.setTextColor(IVORY);motion.setChecked(reducedMotion);
        LinearLayout box=new LinearLayout(context);box.setPadding(dp(22),dp(16),dp(22),dp(12));
        box.addView(motion);
        AlertDialog dialog=new AlertDialog.Builder(context).setTitle("Accessibility")
                .setView(box).setPositiveButton("Done",(d,w)->callbacks.onReducedMotionChanged(motion.isChecked()))
                .setNegativeButton("Cancel",null).create();
        dialog.show();
    }

    public int getCurrentPage() { return currentPage; }

    public void setCurrentPage(int page) {
        // The caller may set this before the first render, when pageCount is still its default.
        int next=Math.max(0,page);
        if(next!=currentPage) {
            currentPage=next;
            updateIndicator();
            callbacks.onCurrentPageChanged(currentPage);
        }
        pager.post(()->{
            int target=currentPage*Math.max(1,pager.getWidth());
            if(reducedMotion)pager.scrollTo(target,0);
            else pager.smoothScrollTo(target,0);
        });
    }

    public void openDrawer() {
        if(drawerDialog!=null&&drawerDialog.isShowing())return;
        Dialog dialog=new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);

        LinearLayout root=new LinearLayout(context);
        root.setOrientation(VERTICAL);
        root.setPadding(dp(22),dp(14),dp(22),dp(18));
        root.setBackground(sheetBackground(BG,dp(30),BORDER));
        root.setElevation(dp(18));
        root.setClipToOutline(true);

        View handle=new View(context);
        handle.setBackground(round(0xff476556,dp(6),0));
        LinearLayout.LayoutParams handleLp=new LinearLayout.LayoutParams(dp(42),dp(4));
        handleLp.gravity=Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin=dp(18);
        root.addView(handle,handleLp);

        LinearLayout head=new LinearLayout(context);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow=label("APP LIBRARY",10,MINT,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        eyebrow.setLetterSpacing(.2f);
        head.addView(eyebrow,new LayoutParams(0,dp(30),1f));
        TextView count=label(apps.size()+" INSTALLED",9,0xff839a8d,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        count.setLetterSpacing(.1f);
        head.addView(count,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(30)));
        TextView close=button("×",IVORY);
        close.setTextSize(22);
        close.setBackground(rippleSurface(SURFACE,0x553bdc9b,dp(16),BORDER));
        close.setContentDescription("Close app library");
        close.setOnClickListener(v->dismissDrawer(dialog,root,null));
        LinearLayout.LayoutParams closeLp=new LayoutParams(dp(44),dp(44));
        closeLp.leftMargin=dp(12);
        head.addView(close,closeLp);
        root.addView(head,new LayoutParams(-1,dp(48)));

        TextView title=label("Everything, in its place.",23,IVORY,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        title.setPadding(0,dp(3),0,0);
        root.addView(title,new LayoutParams(-1,dp(42)));
        TextView caption=label("Search, open or pin an app to your space.",11,MUTED,
                Typeface.create("sans-serif",Typeface.NORMAL));
        root.addView(caption,new LayoutParams(-1,dp(25)));

        EditText search=new EditText(context);
        search.setSingleLine(true);
        search.setTextColor(IVORY);
        search.setHintTextColor(0xff81958a);
        search.setHint("⌕   Search your apps");
        search.setTextSize(14);
        search.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        search.setBackground(round(SURFACE,dp(17),BORDER));
        search.setPadding(dp(17),0,dp(17),0);
        search.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams searchLp=new LayoutParams(-1,dp(54));
        searchLp.topMargin=dp(12);
        searchLp.bottomMargin=dp(10);
        root.addView(search,searchLp);

        ScrollView scroller=new ScrollView(context);
        scroller.setFillViewport(false);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout rows=new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        scroller.addView(rows,new ScrollView.LayoutParams(-1,-2));
        root.addView(scroller,new LayoutParams(-1,0,1f));

        Runnable refresh=()->{
            String q=search.getText().toString().trim().toLowerCase(Locale.getDefault());
            rows.removeAllViews();
            int shown=0;
            for(LauncherApp app:apps) {
                if(!q.isEmpty()&&!app.label.toLowerCase(Locale.getDefault()).contains(q))continue;
                LinearLayout row=new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(5),dp(7),dp(5),dp(7));
                row.setBackground(rippleSurface(0x0014231c,0x223bdc9b,dp(16),0x00000000));

                FrameLayout iconWrap=new FrameLayout(context);
                iconWrap.setBackground(round(SURFACE_RAISED,dp(14),0x553b5b49));
                ImageView icon=new ImageView(context);
                icon.setImageDrawable(app.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iconWrap.addView(icon,new FrameLayout.LayoutParams(dp(38),dp(38),Gravity.CENTER));
                row.addView(iconWrap,new LayoutParams(dp(50),dp(50)));

                TextView name=label(app.label,14,IVORY,
                        Typeface.create("sans-serif-medium",Typeface.NORMAL));
                name.setPadding(dp(13),0,dp(4),0);
                row.addView(name,new LayoutParams(0,dp(50),1f));

                TextView pin=button("＋",GOLD);
                pin.setTextSize(20);
                pin.setBackground(rippleSurface(SURFACE,0x553bdc9b,dp(14),BORDER));
                pin.setContentDescription("Pin "+app.label+" to Home");
                pin.setOnClickListener(v->dismissDrawer(dialog,root,()->callbacks.onPinApp(app)));
                row.addView(pin,new LayoutParams(dp(40),dp(40)));
                row.setOnClickListener(v->dismissDrawer(dialog,root,()->callbacks.onAppSelected(app)));
                row.setOnLongClickListener(v->{
                    dismissDrawer(dialog,root,()->callbacks.onAppLongPressed(app,row));
                    return true;
                });
                rows.addView(row,new LayoutParams(-1,dp(64)));
                row.setTag("drawer-app");
                shown++;
                View line=new View(context);
                line.setBackgroundColor(0x263b5b49);
                rows.addView(line,new LayoutParams(-1,dp(1)));
            }
            if(shown==0) {
                TextView empty=label(q.isEmpty()?"No apps available":"No apps match “"+q+"”",13,MUTED,
                        Typeface.create("sans-serif",Typeface.NORMAL));
                empty.setGravity(Gravity.CENTER);
                rows.addView(empty,new LayoutParams(-1,dp(100)));
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
        if(window!=null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width=WindowManager.LayoutParams.MATCH_PARENT;
            params.height=Math.round(getResources().getDisplayMetrics().heightPixels*.92f);
            params.gravity=Gravity.BOTTOM;
            params.dimAmount=.62f;
            window.setAttributes(params);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        if(window!=null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    Math.round(getResources().getDisplayMetrics().heightPixels*.92f));
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(.62f);
        }
        drawerDialog=dialog;
        dialog.setOnDismissListener(d->{drawerDialog=null;});
        if(!reducedMotion) {
            root.setAlpha(0f);
            root.setTranslationY(dp(54));
            root.animate().alpha(1f).translationY(0).setDuration(430)
                    .setInterpolator(new DecelerateInterpolator(1.7f)).start();
            int stagger=0;
            for(int i=0;i<rows.getChildCount();i++) {
                View child=rows.getChildAt(i);
                if("drawer-app".equals(child.getTag()))animateIn(child,Math.min(420,stagger++*28L));
            }
        }
    }

    private void dismissDrawer(Dialog dialog,View root,Runnable after) {
        if(reducedMotion) {
            dialog.dismiss();
            if(after!=null)after.run();
            return;
        }
        root.animate().cancel();
        root.animate().alpha(0f).translationY(dp(28)).setDuration(190)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(()->{
                    dialog.dismiss();
                    if(after!=null)after.run();
                }).start();
    }

    public boolean isDrawerOpen() {
        return drawerDialog!=null && drawerDialog.isShowing();
    }

    private void updateClock() {
        clock.setText(new SimpleDateFormat("h:mm  ·  EEE, d MMM",Locale.getDefault()).format(new Date()));
        removeCallbacks(clockTicker);
        if(isAttachedToWindow())postDelayed(clockTicker,60000);
    }
    private void updateIndicator(){
        pageIndicator.setText(context.getString(R.string.workspace_page_indicator,currentPage+1,pageCount));
    }
    private TextView label(String text,float size,int color,Typeface face) {
        TextView v=new TextView(context);v.setText(text);v.setTextSize(size);v.setTextColor(color);
        v.setTypeface(face);v.setGravity(Gravity.CENTER_VERTICAL);return v;
    }
    private TextView button(String text,int color) {
        TextView v=label(text,18,color,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        v.setGravity(Gravity.CENTER);
        v.setBackground(rippleSurface(SURFACE,0x553bdc9b,dp(16),BORDER));
        v.setClickable(true);v.setFocusable(true);return v;
    }

    private void animateIn(View view,long delay) {
        if(reducedMotion||view==null)return;
        view.setAlpha(0f);
        view.setTranslationY(dp(14));
        view.setScaleX(.96f);view.setScaleY(.96f);
        view.animate().cancel();
        view.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                .setStartDelay(delay).setDuration(390)
                .setInterpolator(new DecelerateInterpolator(1.65f)).start();
    }

    private RippleDrawable rippleSurface(int fill,int pressed,int radius,int stroke) {
        GradientDrawable content=round(fill,radius,stroke);
        GradientDrawable mask=round(Color.WHITE,radius,0);
        return new RippleDrawable(ColorStateList.valueOf(pressed),content,mask);
    }

    private GradientDrawable sheetBackground(int fill,int radius,int stroke) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadii(new float[]{radius,radius,radius,radius,0,0,0,0});
        if(stroke!=0)d.setStroke(dp(1),stroke);
        return d;
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
