package com.example.tessera;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Small transactional persistence layer for pages, dock, folders and widgets. */
public final class WorkspaceStore extends SQLiteOpenHelper {
    private static final String DB_NAME = "tessera-workspace.db";
    private static final int DB_VERSION = 1;
    private static final int DEFAULT_PAGES = 2;
    private final SharedPreferences prefs;

    public WorkspaceStore(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        prefs=context.getSharedPreferences("tessera_workspace",Context.MODE_PRIVATE);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE workspace_items (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "page INTEGER NOT NULL, cell_x INTEGER NOT NULL, cell_y INTEGER NOT NULL," +
                "span_x INTEGER NOT NULL, span_y INTEGER NOT NULL," +
                "type TEXT NOT NULL, component TEXT, title TEXT, payload TEXT," +
                "widget_id INTEGER NOT NULL DEFAULT -1, user_serial INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX workspace_page_idx ON workspace_items(page,cell_y,cell_x)");
    }

    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) {
        // Future schema migrations belong here. Never discard user layout on upgrade.
    }

    public synchronized List<WorkspaceItem> all() {
        ArrayList<WorkspaceItem> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("workspace_items",null,null,null,null,null,
                "page,cell_y,cell_x,_id")) {
            while(c.moveToNext()) out.add(from(c));
        }
        return out;
    }

    public synchronized List<WorkspaceItem> atPage(int page) {
        ArrayList<WorkspaceItem> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("workspace_items",null,"page=?",
                new String[]{String.valueOf(page)},null,null,"cell_y,cell_x,_id")) {
            while(c.moveToNext()) out.add(from(c));
        }
        return out;
    }

    public synchronized long addApp(int page,int x,int y,String component,String title,long serial) {
        return insert(page,x,y,1,1,WorkspaceItem.APP,component,title,null,-1,serial);
    }

    public synchronized long addShortcut(int page,int x,int y,String component,String shortcutId,
                                         String title,long serial) {
        return insert(page,x,y,1,1,WorkspaceItem.SHORTCUT,component,title,shortcutId,-1,serial);
    }

    public synchronized long addWidget(int page,int x,int y,int widgetId,String provider,String title) {
        return insert(page,x,y,4,2,WorkspaceItem.WIDGET,provider,title,null,widgetId,0);
    }

    private long insert(int page,int x,int y,int sx,int sy,String type,String component,
                        String title,String payload,int widgetId,long serial) {
        ContentValues v=new ContentValues();
        v.put("page",page);v.put("cell_x",x);v.put("cell_y",y);
        v.put("span_x",sx);v.put("span_y",sy);v.put("type",type);
        v.put("component",component);v.put("title",title);v.put("payload",payload);
        v.put("widget_id",widgetId);v.put("user_serial",serial);
        return getWritableDatabase().insertOrThrow("workspace_items",null,v);
    }

    public synchronized void move(long id,int page,int x,int y) {
        ContentValues v=new ContentValues();v.put("page",page);v.put("cell_x",x);v.put("cell_y",y);
        getWritableDatabase().update("workspace_items",v,"_id=?",new String[]{String.valueOf(id)});
    }

    public synchronized void resizeWidget(long id,int spanY) {
        ContentValues v=new ContentValues();v.put("span_y",Math.max(1,Math.min(4,spanY)));
        getWritableDatabase().update("workspace_items",v,"_id=? AND type=?",
                new String[]{String.valueOf(id),WorkspaceItem.WIDGET});
    }

    public synchronized void delete(long id) {
        getWritableDatabase().delete("workspace_items","_id=?",new String[]{String.valueOf(id)});
    }

    public synchronized WorkspaceItem find(long id) {
        try(Cursor c=getReadableDatabase().query("workspace_items",null,"_id=?",
                new String[]{String.valueOf(id)},null,null,null)) {
            return c.moveToFirst()?from(c):null;
        }
    }

    public synchronized WorkspaceItem findAt(int page,int x,int y,long exceptId) {
        for(WorkspaceItem item:all()) {
            if(item.id==exceptId || item.page!=page) continue;
            if(x>=item.cellX && x<item.cellX+item.spanX &&
               y>=item.cellY && y<item.cellY+item.spanY) return item;
        }
        return null;
    }

    /** Merge two app entries into a folder, or append the dragged item to an existing folder. */
    public synchronized void mergeInto(long sourceId,long targetId) {
        SQLiteDatabase db=getWritableDatabase();
        db.beginTransaction();
        try {
            WorkspaceItem source=find(sourceId),target=find(targetId);
            if(source==null||target==null||source.type.equals(WorkspaceItem.WIDGET)) return;
            if(target.type.equals(WorkspaceItem.FOLDER)) {
                String entry=folderEntry(source);
                ContentValues v=new ContentValues();
                String old=target.payload==null?"":target.payload;
                v.put("payload",old.isEmpty()?entry:old+"\n"+entry);
                db.update("workspace_items",v,"_id=?",new String[]{String.valueOf(targetId)});
                db.delete("workspace_items","_id=?",new String[]{String.valueOf(sourceId)});
            } else if(!target.type.equals(WorkspaceItem.WIDGET)) {
                String payload=folderEntry(target)+"\n"+folderEntry(source);
                ContentValues v=new ContentValues();
                v.put("type",WorkspaceItem.FOLDER);v.put("component",(String)null);
                v.put("title","Folder");v.put("payload",payload);
                v.put("span_x",1);v.put("span_y",1);
                db.update("workspace_items",v,"_id=?",new String[]{String.valueOf(targetId)});
                db.delete("workspace_items","_id=?",new String[]{String.valueOf(sourceId)});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private String folderEntry(WorkspaceItem item) {
        String component=item.component==null?"":item.component;
        String title=item.title==null?"App":item.title.replace("|"," ");
        return component+"|"+item.userSerial+"|"+title+"|"+
                (WorkspaceItem.SHORTCUT.equals(item.type)&&item.payload!=null?item.payload:"");
    }

    public synchronized int pageCount() {
        int count=Math.max(DEFAULT_PAGES,prefs.getInt("page_count",DEFAULT_PAGES));
        for(WorkspaceItem item:all()) if(item.page>=count) count=item.page+1;
        return Math.max(1,count);
    }

    public synchronized int addPage() {
        int count=pageCount()+1;prefs.edit().putInt("page_count",count).apply();return count-1;
    }

    public synchronized int firstFreeWidgetRow(int page,int spanY) {
        List<WorkspaceItem> items=atPage(page);
        for(int y=0;y<=6-spanY;y++) {
            boolean occupied=false;
            for(WorkspaceItem i:items) {
                boolean vertical=y<i.cellY+i.spanY && y+spanY>i.cellY;
                boolean horizontal=i.cellX<4 && i.cellX+i.spanX>0;
                if(vertical&&horizontal){occupied=true;break;}
            }
            if(!occupied)return y;
        }
        return -1;
    }

    public synchronized int firstFreeCell(int page,int columns,int rows) {
        List<WorkspaceItem> items=atPage(page);
        for(int y=0;y<rows;y++) for(int x=0;x<columns;x++) {
            boolean occupied=false;
            for(WorkspaceItem i:items) if(x>=i.cellX&&x<i.cellX+i.spanX&&
                    y>=i.cellY&&y<i.cellY+i.spanY){occupied=true;break;}
            if(!occupied)return y*columns+x;
        }
        return -1;
    }

    public synchronized void seedIfEmpty(List<LauncherApp> apps) {
        if(!all().isEmpty()||apps==null||apps.isEmpty()) return;
        int limit=Math.min(apps.size(),16);
        for(int i=0;i<limit;i++) {
            LauncherApp a=apps.get(i);
            if(i<4) addApp(-1,i,0,a.componentName.flattenToString(),a.label,a.userSerial);
            else {
                int n=i-4, page=n/20, cell=n%20;
                if(page>=pageCount()) addPage();
                addApp(page,cell%4,cell/4,a.componentName.flattenToString(),a.label,a.userSerial);
            }
        }
    }

    private WorkspaceItem from(Cursor c) {
        return new WorkspaceItem(c.getLong(c.getColumnIndexOrThrow("_id")),
                c.getInt(c.getColumnIndexOrThrow("page")),
                c.getInt(c.getColumnIndexOrThrow("cell_x")),
                c.getInt(c.getColumnIndexOrThrow("cell_y")),
                c.getInt(c.getColumnIndexOrThrow("span_x")),
                c.getInt(c.getColumnIndexOrThrow("span_y")),
                c.getString(c.getColumnIndexOrThrow("type")),
                c.getString(c.getColumnIndexOrThrow("component")),
                c.getString(c.getColumnIndexOrThrow("title")),
                c.getString(c.getColumnIndexOrThrow("payload")),
                c.getInt(c.getColumnIndexOrThrow("widget_id")),
                c.getLong(c.getColumnIndexOrThrow("user_serial")));
    }
}
