package com.example.tessera;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Wallpaper-first, gesture-driven native launcher canvas. */
public final class TesseraHomeView extends View {
    public interface Callbacks { void onLauncherSearchRequested(); void onDefaultHomeRequested(); void onAppSelected(LauncherApp a); void onAppLongPressed(LauncherApp a); }
    private static final float W=393f,H=852f;
    private static final int IVORY=0xfff7f2e1,GOLD=0xffe6c75c,MINT=0xff7ac7a1,MUTED=0xffb1c3b7;
    private static final int[] COLORS={0xff74c29d,0xffe4c362,0xffda754a,0xff9175b5,0xff699ac2,0xffafbcad};
    private final Callbacks callbacks;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF r=new RectF();
    private final Typeface serif=Typeface.create("serif",0),italic=Typeface.create("serif",2),sans=Typeface.create("sans-serif-medium",0);
    private List<LauncherApp> apps=new ArrayList<>(),shown=new ArrayList<>();
    private String query="";
    private boolean defaultHome,drawer;
    private float progress,downX,downY,lastY;
    private long downTime;
    private boolean moved;
    private int scrollRows;
    private ValueAnimator animator;

    public TesseraHomeView(Context c,Callbacks cb){super(c);callbacks=cb;setLayerType(View.LAYER_TYPE_HARDWARE,null);setFocusable(true);}
    public void setApps(List<LauncherApp> a){apps=new ArrayList<>(a);filter();invalidate();}
    public List<LauncherApp> getVisibleApps(){return new ArrayList<>(shown);}
    public void setDefaultHome(boolean b){defaultHome=b;invalidate();}
    public void setQuery(String q){query=q==null?"":q.trim();scrollRows=0;filter();invalidate();}
    private void filter(){shown.clear();String n=query.toLowerCase(Locale.getDefault());for(LauncherApp a:apps)if(n.isEmpty()||a.label.toLowerCase(Locale.getDefault()).contains(n))shown.add(a);}
    public boolean isDrawerOpen(){return drawer||progress>.02f;}
    public void openDrawer(){animate(true);}
    public void closeDrawer(){animate(false);}
    private void animate(boolean open){
        drawer=open;if(animator!=null)animator.cancel();
        animator=ValueAnimator.ofFloat(progress,open?1f:0f);animator.setDuration(open?480:360);animator.setInterpolator(new DecelerateInterpolator(1.4f));
        animator.addUpdateListener(a->{progress=(float)a.getAnimatedValue();invalidate();});animator.start();
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);float sx=getWidth()/W,sy=getHeight()/H;c.save();c.scale(sx,sy);
        c.drawColor(0x48020b09);
        p.setShader(new RadialGradient(196,275,370,new int[]{0x69215b43,0x18124536,Color.TRANSPARENT},null,Shader.TileMode.CLAMP));c.drawRect(0,0,W,H,p);p.setShader(null);
        drawHome(c);if(progress>.001f)drawDrawer(c);c.restore();
    }
    private void drawHome(Canvas c){
        text(c,"TESSERA",26,72,10,MINT,sans,Paint.Align.LEFT,1.8f);
        String time=new SimpleDateFormat("h:mm",Locale.getDefault()).format(new Date());
        text(c,time,24,220,76,IVORY,italic,Paint.Align.LEFT,0);
        text(c,new SimpleDateFormat("EEEE, d MMMM",Locale.getDefault()).format(new Date()),28,252,13,MUTED,serif,Paint.Align.LEFT,0);
        text(c,"A quieter kind of everyday.",26,330,25,IVORY,italic,Paint.Align.LEFT,0);
        text(c,"YOUR UNIVERSE",26,420,10,GOLD,sans,Paint.Align.LEFT,1.6f);
        if(!defaultHome){round(c,24,435,345,42,14,0x883d584b,0x557ac7a1);text(c,"Set Tessera as your Home app",196,462,12,IVORY,sans,Paint.Align.CENTER,0);}
        int[][] pos={{36,505},{128,505},{220,505},{312,505},{82,614},{266,614}};
        for(int i=0;i<Math.min(6,apps.size());i++)drawApp(c,apps.get(i),pos[i][0],pos[i][1],48,true,i);
        round(c,22,690,349,58,20,0xaa2b463b,0x557dc19d);
        text(c,"⌕",46,728,25,GOLD,serif,Paint.Align.LEFT,0);
        text(c,"Search your universe",76,725,14,IVORY,italic,Paint.Align.LEFT,0);
        text(c,"Swipe up to explore",196,810,10,MUTED,serif,Paint.Align.CENTER,0);
    }
    private void drawDrawer(Canvas c){
        float q=progress,top=H-(H-28)*q;
        p.setShader(new RadialGradient(196,260,430,new int[]{0x55245e49,0x33203931,0x11071311},null,Shader.TileMode.CLAMP));c.drawRect(0,0,W,H,p);p.setShader(null);
        round(c,8,top,W-16,H-top+24,32,0xf20d221d,0x557dc19d);
        if(q<.35f)return;
        float a=Math.min(1,(q-.25f)*2.0f);
        text(c,"YOUR UNIVERSE",26,88,10,MINT,sans,Paint.Align.LEFT,1.5f,a);
        text(c,"Everything,",24,138,34,IVORY,italic,Paint.Align.LEFT,0,a);
        text(c,"in its place.",24,175,34,IVORY,italic,Paint.Align.LEFT,0,a);
        round(c,24,192,349,54,19,0xbe2b463b,0x447dc19d);
        text(c,"⌕",43,227,22,GOLD,serif,Paint.Align.LEFT,0,a);
        text(c,query.isEmpty()?"Find an app or action":query,76,225,13,IVORY,italic,Paint.Align.LEFT,0,a);
        text(c,"ALL APPS",25,284,10,GOLD,sans,Paint.Align.LEFT,1.3f,a);
        text(c,"FREQUENT",127,284,10,MUTED,sans,Paint.Align.LEFT,1.1f,a);
        text(c,"TOOLS",259,284,10,MUTED,sans,Paint.Align.LEFT,1.1f,a);
        round(c,24,296,55,2,1,GOLD,null);
        int start=scrollRows*4,rows=5,count=Math.min(Math.max(0,shown.size()-start),rows*4);
        for(int i=0;i<count;i++){int index=start+i;drawApp(c,shown.get(index),35+(i%4)*88,333+(i/4)*105,48,true,index);}
        if(shown.isEmpty())text(c,"No matching apps",196,405,15,MUTED,italic,Paint.Align.CENTER,0,a);
        text(c,"×",350,87,22,IVORY,serif,Paint.Align.CENTER,0,a);
    }
    private void drawApp(Canvas c,LauncherApp app,float x,float y,float size,boolean label,int index){
        p.setColor(0x50000000);c.drawRoundRect(x+1,y+3,x+size+1,y+size+3,15,15,p);
        p.setColor(COLORS[Math.floorMod(index,COLORS.length)]);c.drawRoundRect(x,y,x+size,y+size,15,15,p);
        Drawable d=app.icon;if(d!=null){int inset=Math.round(size*.1f);d.setBounds((int)x+inset,(int)y+inset,(int)(x+size)-inset,(int)(y+size)-inset);d.draw(c);}
        if(label){String s=app.label.length()>12?app.label.substring(0,11)+"…":app.label;text(c,s,x+size/2,y+size+20,10.5f,IVORY,sans,Paint.Align.CENTER,0);}
    }
    private void round(Canvas c,float x,float y,float w,float h,float radius,int fill,Integer stroke){
        r.set(x,y,x+w,y+h);p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRoundRect(r,radius,radius,p);
        if(stroke!=null){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(stroke);c.drawRoundRect(r,radius,radius,p);p.setStyle(Paint.Style.FILL);}
    }
    private void text(Canvas c,String s,float x,float y,float size,int color,Typeface face,Paint.Align align,float spacing){text(c,s,x,y,size,color,face,align,spacing,1);}
    private void text(Canvas c,String s,float x,float y,float size,int color,Typeface face,Paint.Align align,float spacing,float alpha){
        p.setShader(null);p.setStyle(Paint.Style.FILL);p.setColor((Math.round(Color.alpha(color)*Math.max(0,Math.min(1,alpha)))<<24)|(color&0xffffff));p.setTextSize(size);p.setTypeface(face);p.setTextAlign(align);p.setLetterSpacing(spacing);c.drawText(s,x,y,p);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        float x=e.getX()/(getWidth()/W),y=e.getY()/(getHeight()/H);
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:downX=x;downY=y;lastY=y;downTime=System.currentTimeMillis();moved=false;return true;
            case MotionEvent.ACTION_MOVE:
                float dy=y-lastY;if(Math.abs(y-downY)>12||Math.abs(x-downX)>16)moved=true;
                if(isDrawerOpen()&&downY>315&&Math.abs(dy)>3){int max=Math.max(0,(shown.size()-20+3)/4);if(max>0){scrollRows=Math.max(0,Math.min(max,scrollRows+(int)Math.signum(-dy)));invalidate();}}
                lastY=y;return true;
            case MotionEvent.ACTION_UP:
                float dx=x-downX,delta=y-downY;long held=System.currentTimeMillis()-downTime;
                if(Math.abs(delta)>76&&Math.abs(delta)>Math.abs(dx)*1.25f){if(delta<0)openDrawer();else if(isDrawerOpen())closeDrawer();return true;}
                if(!moved&&held<500)tap(x,y);else if(!moved)longPress(x,y);return true;
            case MotionEvent.ACTION_CANCEL:moved=false;return true;
        }return true;
    }
    private void tap(float x,float y){
        if(isDrawerOpen()){
            if(x>320&&y<112){closeDrawer();return;}
            if(y>=186&&y<=254){callbacks.onLauncherSearchRequested();return;}
            if(y>=320&&y<=800){int col=Math.max(0,Math.min(3,(int)((x-35)/88)));int row=Math.max(0,(int)((y-333)/105));int i=scrollRows*4+row*4+col;if(i>=0&&i<shown.size())callbacks.onAppSelected(shown.get(i));}
            return;
        }
        if(y>=690&&y<=758){openDrawer();postDelayed(callbacks::onSearchRequested,280);return;}
        if(!defaultHome&&y>=435&&y<=477){callbacks.onDefaultHomeRequested();return;}
        int i=homeIndex(x,y);if(i>=0&&i<apps.size())callbacks.onAppSelected(apps.get(i));
    }
    private void longPress(float x,float y){
        int i=isDrawerOpen()&&y>=320?scrollRows*4+Math.max(0,(int)((y-333)/105))*4+Math.max(0,Math.min(3,(int)((x-35)/88))):homeIndex(x,y);
        if(i>=0&&i<(isDrawerOpen()?shown.size():apps.size()))callbacks.onAppLongPressed(isDrawerOpen()?shown.get(i):apps.get(i));
    }
    private int homeIndex(float x,float y){
        int[][] a={{36,505},{128,505},{220,505},{312,505},{82,614},{266,614}};
        for(int i=0;i<a.length;i++)if(x>=a[i][0]-8&&x<=a[i][0]+57&&y>=a[i][1]-8&&y<=a[i][1]+67)return i;return -1;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
