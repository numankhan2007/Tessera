package com.example.tessera;

import android.app.AlertDialog;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ClipData;
import android.content.res.ColorStateList;
import android.content.pm.ApplicationInfo;
import android.os.SystemClock;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
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
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
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
    // Obsidian Garden: ink canvas, evergreen glass, warm pearl and soft aurora.
    private static final int BG=0xff070d0a, PANEL=0xff101b16, SURFACE=0xff17251d,
            SURFACE_RAISED=0xff20362a, BORDER=0xff385746, IVORY=0xfff5f2e8,
            GOLD=0xffe7cc75, MINT=0xff9fe1bd, MUTED=0xff9fb2a5;
    private static final int GROUP_ALL=-1, GROUP_SOCIAL=1, GROUP_WORK=2,
            GROUP_MEDIA=3, GROUP_GAMES=4, GROUP_OTHER=5;
    private static final long AMBIENT_FRAME_DELAY_MS=24L;
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
    private final Paint ambientPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint motePaint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private RadialGradient emeraldBloom;
    private RadialGradient tealBloom;
    private RadialGradient goldBloom;
    private final List<View> floatingElements=new ArrayList<>();
    private ValueAnimator orbitAnimator;
    private long ambientEpoch=SystemClock.uptimeMillis();
    private float scrollProgress;

    public TesseraHomeView(Context c,Callbacks cb) {
        super(c);
        context=c;callbacks=cb;
        setOrientation(VERTICAL);
        setPadding(dp(18),dp(8),dp(18),dp(8));
        setBackgroundColor(Color.TRANSPARENT);
        setFocusableInTouchMode(true);
        setClipChildren(false);
        setWillNotDraw(false);

        LinearLayout masthead=new LinearLayout(c);
        masthead.setGravity(Gravity.CENTER_VERTICAL);
        TextView brand=label("T E S S E R A",11,MINT,Typeface.create("sans-serif-medium",Typeface.NORMAL));
        brand.setLetterSpacing(.16f);
        masthead.addView(brand,new LayoutParams(0,dp(40),1f));
        clock=label("",12,IVORY,Typeface.create("sans-serif",Typeface.NORMAL));
        masthead.addView(clock,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(40)));
        TextView options=button("＋",GOLD);
        options.setTextSize(23);
        options.setBackground(rippleSurface(SURFACE,0x5539b87c,dp(17),BORDER));
        options.setContentDescription("Workspace and launcher settings");
        options.setOnClickListener(v->showWorkspaceMenu(v));
        LayoutParams optionsLp=new LayoutParams(dp(44),dp(44));
        optionsLp.leftMargin=dp(11);
        masthead.addView(options,optionsLp);
        addView(masthead,new LayoutParams(-1,dp(46)));

        LinearLayout hero=new LinearLayout(c);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setOrientation(HORIZONTAL);
        hero.setPadding(dp(2),dp(1),dp(3),0);
        TextView title=label("Your space.\nYour own rhythm.",25,IVORY,
                Typeface.create("serif",Typeface.ITALIC));
        title.setLineSpacing(dp(1),1f);
        hero.addView(title,new LayoutParams(0,dp(61),1f));
        TextView orbit=label("✧",42,GOLD,Typeface.create("serif",Typeface.NORMAL));
        orbit.setGravity(Gravity.CENTER);
        orbit.setBackground(rippleSurface(0x221a3527,0x2239b87c,dp(30),0x333e6650));
        orbit.setElevation(dp(4));
        LayoutParams orbitLp=new LayoutParams(dp(58),dp(58));
        orbitLp.leftMargin=dp(10);
        hero.addView(orbit,orbitLp);
        orbitAnimator=ValueAnimator.ofFloat(-18f,18f);
        orbitAnimator.setDuration(9200);
        orbitAnimator.setRepeatCount(ValueAnimator.INFINITE);
        orbitAnimator.setRepeatMode(ValueAnimator.REVERSE);
        orbitAnimator.setInterpolator(new LinearInterpolator());
        orbitAnimator.addUpdateListener(animation->{
            orbit.setRotation((Float)animation.getAnimatedValue());
            float pulse=1f+.026f*(float)Math.sin(animation.getAnimatedFraction()*Math.PI);
            orbit.setScaleX(pulse);
            orbit.setScaleY(pulse);
        });
        LayoutParams heroLp=new LayoutParams(-1,dp(66));
        heroLp.topMargin=dp(2);
        addView(hero,heroLp);

        TextView subtitle=label("A CALMER PLACE TO LAND",9,GOLD,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        subtitle.setLetterSpacing(.22f);
        subtitle.setPadding(dp(3),0,0,0);
        addView(subtitle,new LayoutParams(-1,dp(21)));

        if(!defaultHome) {
            homeHint=button("SET TESSERA AS HOME   ↗",MINT);
            homeHint.setTextSize(10);
            homeHint.setLetterSpacing(.06f);
            homeHint.setBackground(rippleSurface(SURFACE,0x4439b87c,dp(13),BORDER));
            homeHint.setOnClickListener(v->callbacks.onSetHomeRequested());
            LayoutParams hintLp=new LayoutParams(-1,dp(34));
            hintLp.topMargin=dp(3);
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
        pagerLp.topMargin=dp(1);
        addView(pager,pagerLp);

        pageIndicator=label("01  /  01",9,MUTED,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        pageIndicator.setGravity(Gravity.CENTER);
        pageIndicator.setLetterSpacing(.18f);
        addView(pageIndicator,new LayoutParams(-1,dp(21)));

        LinearLayout bottom=new LinearLayout(c);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setOrientation(HORIZONTAL);
        dock=new LinearLayout(c);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(HORIZONTAL);
        dock.setPadding(dp(5),dp(4),dp(5),dp(4));
        dock.setBackground(rippleSurface(PANEL,0x2239b87c,dp(22),BORDER));
        dock.setElevation(dp(9));
        dock.setClipToOutline(true);
        bottom.addView(dock,new LayoutParams(0,dp(68),1f));
        TextView all=button("⌕",GOLD);
        all.setTextSize(27);
        all.setContentDescription("Open app library");
        all.setBackground(rippleSurface(SURFACE,0x5539b87c,dp(17),BORDER));
        all.setOnClickListener(v->openDrawer());
        LinearLayout.LayoutParams allLp=new LayoutParams(dp(56),dp(56));
        allLp.leftMargin=dp(9);
        bottom.addView(all,allLp);
        LayoutParams bottomLp=new LayoutParams(-1,dp(72));
        bottomLp.topMargin=dp(1);
        addView(bottom,bottomLp);

        pager.setOnScrollChangeListener((View v,int sx,int sy,int ox,int oy)->{
            int w=Math.max(1,pager.getWidth());
            scrollProgress=Math.max(0f,Math.min(pageCount-1,(float)sx/w));
            int nextPage=Math.max(0,Math.min(pageCount-1,Math.round(scrollProgress)));
            if(nextPage!=currentPage) {
                currentPage=nextPage;
                callbacks.onCurrentPageChanged(currentPage);
                pageIndicator.setAlpha(.5f);
                pageIndicator.animate().alpha(1f).setDuration(240).start();
            }
            updatePageParallax();
            updateIndicator();
        });
    }

    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        emeraldBloom=new RadialGradient(0f,0f,1f,
                new int[]{0x405ac38a,0x1d3d956a,0x003d956a},
                new float[]{0f,.46f,1f},Shader.TileMode.CLAMP);
        tealBloom=new RadialGradient(0f,0f,1f,
                new int[]{0x3048aa9a,0x17327f73,0x00327f73},
                new float[]{0f,.48f,1f},Shader.TileMode.CLAMP);
        goldBloom=new RadialGradient(0f,0f,1f,
                new int[]{0x20d8b56b,0x0d93734d,0x0093734d},
                new float[]{0f,.42f,1f},Shader.TileMode.CLAMP);
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        drawAtmosphere(canvas);
        super.dispatchDraw(canvas);
        updateFloatingElements();
        if(isMotionActive())postInvalidateDelayed(AMBIENT_FRAME_DELAY_MS);
    }

    private void drawAtmosphere(Canvas canvas) {
        if(emeraldBloom==null||tealBloom==null||goldBloom==null)return;
        float phase=(SystemClock.uptimeMillis()-ambientEpoch)/1000f;
        float driftA=(float)Math.sin(phase*.19f);
        float driftB=(float)Math.cos(phase*.15f);
        drawBloom(canvas,emeraldBloom,
                getWidth()*(.18f+.035f*driftA),getHeight()*(.23f+.045f*driftB),
                getWidth()*.67f,getHeight()*.54f);
        drawBloom(canvas,tealBloom,
                getWidth()*(.84f+.035f*driftB),getHeight()*(.73f+.04f*driftA),
                getWidth()*.55f,getHeight()*.43f);
        drawBloom(canvas,goldBloom,
                getWidth()*.54f,getHeight()*(.08f+.018f*driftA),
                getWidth()*.46f,getHeight()*.27f);

        motePaint.setShader(null);
        motePaint.setStyle(Paint.Style.FILL);
        for(int i=0;i<8;i++) {
            float baseX=.10f+((i*31)%79)/100f;
            float baseY=.14f+((i*23)%68)/100f;
            float x=(baseX+.007f*(float)Math.sin(phase*.24f+i))*getWidth();
            float y=(baseY+.007f*(float)Math.cos(phase*.20f+i*1.3f))*getHeight();
            float twinkle=.5f+.5f*(float)Math.sin(phase*.72f+i*1.7f);
            motePaint.setColor(Color.argb(Math.round(17+17*twinkle),201,233,211));
            canvas.drawCircle(x,y,dp(i%3==0?1.6f:1.1f),motePaint);
        }
    }

    private void drawBloom(Canvas canvas,RadialGradient shader,float x,float y,
                           float radiusX,float radiusY) {
        int save=canvas.save();
        canvas.translate(x,y);
        canvas.scale(Math.max(1f,radiusX),Math.max(1f,radiusY));
        ambientPaint.setShader(shader);
        ambientPaint.setAlpha(255);
        canvas.drawCircle(0f,0f,1f,ambientPaint);
        ambientPaint.setShader(null);
        canvas.restoreToCount(save);
    }

    private boolean isMotionActive() {
        return !reducedMotion&&ValueAnimator.areAnimatorsEnabled()
                &&isAttachedToWindow()&&getWindowVisibility()==VISIBLE;
    }

    private void updateFloatingElements() {
        if(floatingElements.isEmpty())return;
        if(!isMotionActive()) {
            for(View element:floatingElements)element.setTranslationY(0f);
            return;
        }
        float phase=(SystemClock.uptimeMillis()-ambientEpoch)/1000f;
        for(int i=0;i<floatingElements.size();i++) {
            View element=floatingElements.get(i);
            if(element.getParent()==null)continue;
            float amplitude=dp(i%3==0?1.8f:1.25f);
            float offset=(float)Math.sin(phase*.78f+i*1.17f)*amplitude;
            element.setTranslationY(offset);
        }
    }

    private void syncOrbitMotion() {
        if(orbitAnimator==null)return;
        if(!isMotionActive()) {
            if(orbitAnimator.isRunning())orbitAnimator.cancel();
        } else if(!orbitAnimator.isStarted()) {
            orbitAnimator.start();
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ambientEpoch=SystemClock.uptimeMillis();
        updateClock();
        syncOrbitMotion();
        if(isMotionActive())postInvalidateDelayed(AMBIENT_FRAME_DELAY_MS);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        syncOrbitMotion();
        if(visibility==VISIBLE&&isMotionActive())postInvalidateDelayed(AMBIENT_FRAME_DELAY_MS);
    }

    @Override protected void onDetachedFromWindow() {
        removeCallbacks(clockTicker);
        if(orbitAnimator!=null&&orbitAnimator.isRunning())orbitAnimator.cancel();
        for(View element:floatingElements)element.setTranslationY(0f);
        if(drawerDialog!=null&&drawerDialog.isShowing())drawerDialog.dismiss();
        super.onDetachedFromWindow();
    }

    public void render(List<WorkspaceItem> saved,List<LauncherApp> installed,int pages,
                       boolean isDefault,Map<Integer,View> widgets,boolean reduceMotion) {
        items=saved==null?new ArrayList<>():new ArrayList<>(saved);
        apps=installed==null?new ArrayList<>():new ArrayList<>(installed);
        pageCount=Math.max(1,pages);
        currentPage=Math.max(0,Math.min(pageCount-1,currentPage));
        scrollProgress=currentPage;
        defaultHome=isDefault;widgetViews=widgets;reducedMotion=reduceMotion;
        syncOrbitMotion();
        if(homeHint!=null)homeHint.setVisibility(defaultHome?GONE:VISIBLE);
        floatingElements.clear();
        pageStrip.removeAllViews();dock.removeAllViews();

        int width=Math.max(dp(300),getResources().getDisplayMetrics().widthPixels-getPaddingLeft()-getPaddingRight());
        for(int page=0;page<pageCount;page++) {
            final int pageIndex=page;
            LinearLayout column=new LinearLayout(context);
            column.setOrientation(VERTICAL);
            column.setPadding(dp(2),dp(3),dp(2),0);
            column.setClipChildren(false);
            pageStrip.addView(column,new LayoutParams(width,LayoutParams.MATCH_PARENT));

            LinearLayout section=new LinearLayout(context);
            section.setGravity(Gravity.CENTER_VERTICAL);
            TextView pageTitle=label(String.format(Locale.getDefault(),"SPACE  %02d",page+1),9,MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            pageTitle.setLetterSpacing(.19f);
            section.addView(pageTitle,new LayoutParams(0,dp(25),1f));
            int appCount=0;
            for(WorkspaceItem item:items)if(item.page==page)appCount++;
            TextView count=label(appCount==0?"READY":"ITEMS  "+String.format(Locale.getDefault(),"%02d",appCount),7,0xff789184,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            count.setLetterSpacing(.11f);
            section.addView(count,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(25)));
            column.addView(section,new LayoutParams(-1,dp(26)));

            List<WorkspaceItem> pageItems=new ArrayList<>();
            for(WorkspaceItem item:items)if(item.page==page)pageItems.add(item);
            if(pageItems.isEmpty()) {
                column.addView(createEmptySpace(pageIndex),new LayoutParams(-1,dp(260)));
            } else {
                GridLayout grid=new GridLayout(context);
                grid.setColumnCount(4);grid.setRowCount(6);
                grid.setUseDefaultMargins(false);
                grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
                grid.setColumnOrderPreserved(false);
                grid.setBackgroundColor(Color.TRANSPARENT);
                grid.setPadding(dp(1),dp(2),dp(1),dp(2));
                grid.setClipChildren(false);
                grid.setTag(page);
                grid.setOnDragListener((v,e)->handleDrop(v,e,pageIndex,false));
                int usedRows=1;
                for(WorkspaceItem item:pageItems) {
                    int bottom=WorkspaceItem.WIDGET.equals(item.type)
                            ?item.cellY+Math.max(1,item.spanY):item.cellY+1;
                    usedRows=Math.max(usedRows,Math.min(6,bottom));
                }
                int gridHeight=Math.max(84,Math.min(438,usedRows*72+12));
                column.addView(grid,new LayoutParams(-1,dp(gridHeight)));
                for(WorkspaceItem item:pageItems)addWorkspaceChild(grid,item);
            }
            animateIn(column,page*42L);
        }

        List<WorkspaceItem> dockItems=new ArrayList<>();
        for(WorkspaceItem i:items)if(i.page==-1)dockItems.add(i);
        for(WorkspaceItem item:dockItems)addDockChild(item);
        if(dockItems.isEmpty()) {
            TextView empty=label("＋   PIN YOUR FAVOURITES",8,MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            empty.setLetterSpacing(.13f);
            empty.setGravity(Gravity.CENTER);
            dock.addView(empty,new LayoutParams(-1,-1));
        }
        dock.setOnDragListener((v,e)->handleDrop(v,e,-1,true));
        updateIndicator();
        pager.post(()->{
            int target=currentPage*pager.getWidth();
            if(reducedMotion)pager.scrollTo(target,0);
            else pager.smoothScrollTo(target,0);
            updatePageParallax();
        });
        if(!reducedMotion) {
            dock.setAlpha(.7f);dock.setTranslationY(dp(12));
            dock.animate().alpha(1f).translationY(0).setStartDelay(110)
                    .setDuration(520).setInterpolator(new DecelerateInterpolator(1.7f)).start();
        }
        if(isMotionActive())postInvalidateDelayed(AMBIENT_FRAME_DELAY_MS);
    }

    private View createEmptySpace(int page) {
        LinearLayout empty=new LinearLayout(context);
        empty.setOrientation(VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundColor(Color.TRANSPARENT);
        TextView glyph=label("✧",35,GOLD,Typeface.create("serif",Typeface.NORMAL));
        glyph.setGravity(Gravity.CENTER);
        empty.addView(glyph,new LayoutParams(-1,dp(62)));
        TextView title=label("A little room to begin.",16,IVORY,
                Typeface.create("serif",Typeface.ITALIC));
        title.setGravity(Gravity.CENTER);
        empty.addView(title,new LayoutParams(-1,dp(30)));
        TextView detail=label("Bring your favourite apps into this space.",10,MUTED,
                Typeface.create("sans-serif",Typeface.NORMAL));
        detail.setGravity(Gravity.CENTER);
        empty.addView(detail,new LayoutParams(-1,dp(28)));
        TextView action=button("EXPLORE APPS   ↗",MINT);
        action.setTextSize(9);
        action.setLetterSpacing(.13f);
        LinearLayout.LayoutParams actionLp=new LayoutParams(dp(176),dp(38));
        actionLp.topMargin=dp(13);
        empty.addView(action,actionLp);
        action.setOnClickListener(v->openDrawer());
        animateIn(empty,page*60L);
        return empty;
    }

    private void updatePageParallax() {
        if(pageStrip==null||pager==null)return;
        for(int i=0;i<pageStrip.getChildCount();i++) {
            View page=pageStrip.getChildAt(i);
            float distance=i-scrollProgress;
            float absolute=Math.min(1f,Math.abs(distance));
            page.setTranslationX(-distance*dp(10));
            page.setScaleX(1f-absolute*.018f);
            page.setAlpha(1f-absolute*.12f);
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
        lp.width=0;lp.height=dp(72*sy);lp.setMargins(dp(1),dp(1),dp(1),dp(1));
        grid.addView(child,lp);
        animateIn(child,Math.min(360,(grid.getChildCount()-1)*28L));
    }

    private View createTile(WorkspaceItem item) {
        LinearLayout tile=new LinearLayout(context);
        tile.setOrientation(VERTICAL);tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(1),0,dp(1),0);
        tile.setBackground(rippleSurface(0x0014231c,0x2239b87c,dp(15),0x00000000));
        tile.setClipChildren(false);
        FrameLayout iconFrame=new FrameLayout(context);
        floatingElements.add(iconFrame);
        iconFrame.setBackground(round(SURFACE_RAISED,dp(17),0x34516f5b));
        iconFrame.setElevation(dp(3));
        ImageView icon=new ImageView(context);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable drawable=findIcon(item);
        if(drawable!=null)icon.setImageDrawable(drawable);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(47),dp(47),Gravity.CENTER);
        iconFrame.addView(icon,ip);
        tile.addView(iconFrame,new LayoutParams(dp(56),dp(56)));
        String title=WorkspaceItem.FOLDER.equals(item.type)?"Folder":item.title;
        TextView name=label(title==null?"App":title,9.5f,IVORY,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setPadding(0,dp(1),0,0);
        tile.addView(name,new LayoutParams(-1,dp(16)));
        tile.setContentDescription(title==null?"Workspace item":title);
        tile.setOnClickListener(v->{
            if(!reducedMotion) {
                v.animate().scaleX(.92f).scaleY(.92f).setDuration(110)
                        .setInterpolator(new DecelerateInterpolator())
                        .withEndAction(()->{
                            v.animate().scaleX(1f).scaleY(1f).setDuration(230)
                                    .setInterpolator(new OvershootInterpolator(.72f)).start();
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
        floatingElements.add(icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        Drawable d=findIcon(item);
        if(d!=null)icon.setImageDrawable(d);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        tile.addView(icon,new LayoutParams(dp(42),dp(42)));
        tile.setContentDescription(item.title==null?"Open application":item.title);
        tile.setBackground(rippleSurface(0x0014231c,0x3339b87c,dp(16),0x00000000));
        tile.setOnClickListener(v->callbacks.onLaunchItem(item));
        tile.setOnLongClickListener(v->{showItemMenu(item,v);return true;});
        LayoutParams lp=new LayoutParams(0,-1,1f);
        dock.addView(tile,lp);
        animateIn(tile,(dock.getChildCount()-1)*42L);
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
        root.setPadding(dp(20),dp(11),dp(20),dp(15));
        root.setBackground(sheetBackground(BG,dp(29),BORDER));
        root.setElevation(dp(20));
        root.setClipToOutline(true);

        View handle=new View(context);
        handle.setBackground(round(0xff50715d,dp(8),0));
        LinearLayout.LayoutParams handleLp=new LinearLayout.LayoutParams(dp(40),dp(4));
        handleLp.gravity=Gravity.CENTER_HORIZONTAL;
        handleLp.bottomMargin=dp(12);
        root.addView(handle,handleLp);

        LinearLayout top=new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView eyebrow=label("THE APP LIBRARY",9,MINT,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        eyebrow.setLetterSpacing(.2f);
        top.addView(eyebrow,new LayoutParams(0,dp(26),1f));
        TextView count=label(apps.size()+" APPS",8,0xff839b8d,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        count.setLetterSpacing(.15f);
        top.addView(count,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(26)));
        TextView close=button("×",IVORY);
        close.setTextSize(22);
        close.setBackground(rippleSurface(SURFACE,0x5539b87c,dp(15),BORDER));
        close.setContentDescription("Close app library");
        close.setOnClickListener(v->dismissDrawer(dialog,root,null));
        LinearLayout.LayoutParams closeLp=new LayoutParams(dp(40),dp(40));
        closeLp.leftMargin=dp(10);
        top.addView(close,closeLp);
        root.addView(top,new LayoutParams(-1,dp(42)));

        TextView title=label("Find your next move.",23,IVORY,
                Typeface.create("serif",Typeface.ITALIC));
        root.addView(title,new LayoutParams(-1,dp(39)));
        TextView caption=label("Your favourites first. Everything else, in its place.",9,MUTED,
                Typeface.create("sans-serif",Typeface.NORMAL));
        root.addView(caption,new LayoutParams(-1,dp(21)));

        EditText search=new EditText(context);
        search.setSingleLine(true);
        search.setTextColor(IVORY);
        search.setHintTextColor(0xff81968a);
        search.setHint("⌕   Search apps by name");
        search.setTextSize(13);
        search.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        search.setBackground(round(SURFACE,dp(16),BORDER));
        search.setPadding(dp(15),0,dp(15),0);
        search.setSelectAllOnFocus(false);
        LinearLayout.LayoutParams searchLp=new LayoutParams(-1,dp(48));
        searchLp.topMargin=dp(8);
        searchLp.bottomMargin=dp(8);
        root.addView(search,searchLp);

        ScrollView scroller=new ScrollView(context);
        scroller.setFillViewport(false);
        scroller.setVerticalScrollBarEnabled(false);
        LinearLayout rows=new LinearLayout(context);
        rows.setOrientation(VERTICAL);
        scroller.addView(rows,new ScrollView.LayoutParams(-1,-2));

        final int[] selectedGroup={GROUP_ALL};
        final List<TextView> chips=new ArrayList<>();
        LinearLayout filterRow=new LinearLayout(context);
        filterRow.setOrientation(HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        String[] filterNames={"All","Social","Work","Media","Games","Other"};
        int[] filterValues={GROUP_ALL,GROUP_SOCIAL,GROUP_WORK,GROUP_MEDIA,GROUP_GAMES,GROUP_OTHER};
        for(int i=0;i<filterNames.length;i++) {
            final int group=filterValues[i];
            TextView chip=label(filterNames[i],9,group==GROUP_ALL?IVORY:MUTED,
                    Typeface.create("sans-serif-medium",Typeface.NORMAL));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(12),0,dp(12),0);
            chip.setBackground(round(group==GROUP_ALL?SURFACE_RAISED:0x0014231c,dp(15),
                    group==GROUP_ALL?BORDER:0x22466b54));
            chip.setTag(group);
            chip.setOnClickListener(v->{
                selectedGroup[0]=group;
                for(TextView option:chips) {
                    int optionGroup=(Integer)option.getTag();
                    boolean active=optionGroup==group;
                    option.setTextColor(active?IVORY:MUTED);
                    option.setBackground(round(active?SURFACE_RAISED:0x0014231c,dp(15),
                            active?BORDER:0x22466b54));
                }
                refreshDrawerRows(search,rows,dialog,root,selectedGroup[0]);
            });
            chips.add(chip);
            LinearLayout.LayoutParams chipLp=new LayoutParams(LayoutParams.WRAP_CONTENT,dp(32));
            chipLp.rightMargin=dp(6);
            filterRow.addView(chip,chipLp);
        }
        HorizontalScrollView filters=new HorizontalScrollView(context);
        filters.setHorizontalScrollBarEnabled(false);
        filters.addView(filterRow,new HorizontalScrollView.LayoutParams(-2,-1));
        LinearLayout.LayoutParams filterLp=new LayoutParams(-1,dp(36));
        filterLp.bottomMargin=dp(6);
        root.addView(filters,filterLp);

        LinearLayout quickHeading=new LinearLayout(context);
        quickHeading.setGravity(Gravity.CENTER_VERTICAL);
        TextView favouritesTitle=label("QUICK ACCESS",8,GOLD,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        favouritesTitle.setLetterSpacing(.18f);
        quickHeading.addView(favouritesTitle,new LayoutParams(0,dp(22),1f));
        TextView favouriteHint=label("FROM YOUR DOCK",7,0xff6e8c7a,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        favouriteHint.setLetterSpacing(.1f);
        quickHeading.addView(favouriteHint,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(22)));
        root.addView(quickHeading,new LayoutParams(-1,dp(22)));

        HorizontalScrollView quickScroll=new HorizontalScrollView(context);
        quickScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout quickRow=new LinearLayout(context);
        quickRow.setOrientation(HORIZONTAL);
        quickRow.setPadding(0,dp(1),0,dp(3));
        int favouriteCount=0;
        for(WorkspaceItem item:items) {
            if(item.page!=-1||favouriteCount>=5)continue;
            LauncherApp app=findApp(item);
            if(app==null)continue;
            LinearLayout favourite=new LinearLayout(context);
            favourite.setOrientation(VERTICAL);
            favourite.setGravity(Gravity.CENTER);
            favourite.setBackground(rippleSurface(0x0014231c,0x2239b87c,dp(14),0x00000000));
            ImageView icon=new ImageView(context);
            icon.setImageDrawable(app.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            favourite.addView(icon,new LayoutParams(dp(36),dp(36)));
            TextView appName=label(app.label,8,IVORY,Typeface.create("sans-serif-medium",Typeface.NORMAL));
            appName.setGravity(Gravity.CENTER);
            appName.setMaxLines(1);
            appName.setEllipsize(android.text.TextUtils.TruncateAt.END);
            favourite.addView(appName,new LayoutParams(dp(62),dp(16)));
            favourite.setContentDescription("Open favourite "+app.label);
            favourite.setOnClickListener(v->dismissDrawer(dialog,root,()->callbacks.onAppSelected(app)));
            LinearLayout.LayoutParams favLp=new LayoutParams(dp(68),dp(58));
            favLp.rightMargin=dp(4);
            quickRow.addView(favourite,favLp);
            favouriteCount++;
        }
        quickScroll.addView(quickRow,new HorizontalScrollView.LayoutParams(-2,-1));
        quickScroll.setVisibility(favouriteCount==0?GONE:VISIBLE);
        root.addView(quickScroll,new LayoutParams(-1,dp(60)));

        TextView allHeading=label("ALL APPS",8,MUTED,
                Typeface.create("sans-serif-medium",Typeface.NORMAL));
        allHeading.setLetterSpacing(.18f);
        LinearLayout.LayoutParams allHeadingLp=new LayoutParams(-1,dp(24));
        allHeadingLp.topMargin=dp(2);
        root.addView(allHeading,allHeadingLp);
        root.addView(scroller,new LayoutParams(-1,0,1f));

        Runnable refresh=()->refreshDrawerRows(search,rows,dialog,root,selectedGroup[0]);
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
            params.height=Math.round(getResources().getDisplayMetrics().heightPixels*.94f);
            params.gravity=Gravity.BOTTOM;
            params.dimAmount=.68f;
            window.setAttributes(params);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        if(window!=null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    Math.round(getResources().getDisplayMetrics().heightPixels*.94f));
            window.setGravity(Gravity.BOTTOM);
            window.setDimAmount(.68f);
        }
        drawerDialog=dialog;
        dialog.setOnDismissListener(d->{drawerDialog=null;});
        if(!reducedMotion) {
            root.setAlpha(0f);
            root.setTranslationY(dp(72));
            root.setScaleX(.985f);root.setScaleY(.985f);
            root.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f)
                    .setDuration(460).setInterpolator(new DecelerateInterpolator(1.45f)).start();
            int stagger=0;
            for(int i=0;i<rows.getChildCount();i++) {
                View child=rows.getChildAt(i);
                if("drawer-app".equals(child.getTag()))animateIn(child,Math.min(360,stagger++*24L));
            }
        }
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
    }

    private void refreshDrawerRows(EditText search,LinearLayout rows,Dialog dialog,View root,int group) {
        String query=search.getText().toString().trim().toLowerCase(Locale.getDefault());
        rows.removeAllViews();
        int shown=0;
        char lastInitial=0;
        for(LauncherApp app:apps) {
            if(!query.isEmpty()&&!app.label.toLowerCase(Locale.getDefault()).contains(query))continue;
            if(group!=GROUP_ALL&&libraryGroup(app)!=group)continue;
            char initial=app.label.isEmpty()?'#':Character.toUpperCase(app.label.charAt(0));
            if(query.isEmpty()&&initial!=lastInitial) {
                TextView letter=label(String.valueOf(initial),8,0xff7f9b89,
                        Typeface.create("sans-serif-medium",Typeface.NORMAL));
                letter.setLetterSpacing(.16f);
                letter.setPadding(dp(5),0,0,0);
                rows.addView(letter,new LayoutParams(-1,dp(22)));
                lastInitial=initial;
            }
            LinearLayout row=new LinearLayout(context);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4),dp(5),dp(4),dp(5));
            row.setBackground(rippleSurface(0x0014231c,0x2239b87c,dp(14),0x00000000));
            FrameLayout iconWrap=new FrameLayout(context);
            iconWrap.setBackground(round(SURFACE_RAISED,dp(13),0x334c6c57));
            ImageView icon=new ImageView(context);
            icon.setImageDrawable(app.icon);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iconWrap.addView(icon,new FrameLayout.LayoutParams(dp(39),dp(39),Gravity.CENTER));
            row.addView(iconWrap,new LayoutParams(dp(50),dp(48)));
            TextView name=label(app.label,12,IVORY,Typeface.create("sans-serif-medium",Typeface.NORMAL));
            name.setPadding(dp(11),0,dp(3),0);
            row.addView(name,new LayoutParams(0,dp(48),1f));
            TextView pin=button("＋",GOLD);
            pin.setTextSize(18);
            pin.setBackground(rippleSurface(SURFACE,0x5539b87c,dp(13),BORDER));
            pin.setContentDescription("Pin "+app.label+" to Home");
            pin.setOnClickListener(v->dismissDrawer(dialog,root,()->callbacks.onPinApp(app)));
            row.addView(pin,new LayoutParams(dp(39),dp(39)));
            row.setOnClickListener(v->dismissDrawer(dialog,root,()->callbacks.onAppSelected(app)));
            row.setOnLongClickListener(v->{
                dismissDrawer(dialog,root,()->callbacks.onAppLongPressed(app,row));
                return true;
            });
            rows.addView(row,new LayoutParams(-1,dp(56)));
            row.setTag("drawer-app");
            shown++;
        }
        if(shown==0) {
            LinearLayout empty=new LinearLayout(context);
            empty.setGravity(Gravity.CENTER);
            empty.setOrientation(VERTICAL);
            TextView glyph=label("⌕",30,GOLD,Typeface.create("serif",Typeface.NORMAL));
            glyph.setGravity(Gravity.CENTER);
            empty.addView(glyph,new LayoutParams(-1,dp(40)));
            TextView message=label(query.isEmpty()?"No apps in this category":"No apps match “"+query+"”",12,MUTED,
                    Typeface.create("sans-serif",Typeface.NORMAL));
            message.setGravity(Gravity.CENTER);
            empty.addView(message,new LayoutParams(-1,dp(30)));
            rows.addView(empty,new LayoutParams(-1,dp(100)));
        }
    }

    private int libraryGroup(LauncherApp app) {
        switch(app.category) {
            case ApplicationInfo.CATEGORY_SOCIAL:return GROUP_SOCIAL;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:return GROUP_WORK;
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
            case ApplicationInfo.CATEGORY_IMAGE:return GROUP_MEDIA;
            case ApplicationInfo.CATEGORY_GAME:return GROUP_GAMES;
            default:return GROUP_OTHER;
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
        pageIndicator.setText(String.format(Locale.getDefault(),"%02d   /   %02d",currentPage+1,pageCount));
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
                .setStartDelay(delay).setDuration(500)
                .setInterpolator(new OvershootInterpolator(.48f)).start();
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
