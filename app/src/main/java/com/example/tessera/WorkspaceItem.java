package com.example.tessera;

/** Persisted workspace entry. page=-1 identifies a dock slot. */
public final class WorkspaceItem {
    public static final String APP = "APP";
    public static final String SHORTCUT = "SHORTCUT";
    public static final String FOLDER = "FOLDER";
    public static final String WIDGET = "WIDGET";

    public long id;
    public int page, cellX, cellY, spanX, spanY, widgetId;
    public long userSerial;
    public String type, component, title, payload;

    public WorkspaceItem(long id, int page, int cellX, int cellY,
                         int spanX, int spanY, String type, String component,
                         String title, String payload, int widgetId, long userSerial) {
        this.id=id; this.page=page; this.cellX=cellX; this.cellY=cellY;
        this.spanX=spanX; this.spanY=spanY; this.type=type;
        this.component=component; this.title=title; this.payload=payload;
        this.widgetId=widgetId; this.userSerial=userSerial;
    }
}
