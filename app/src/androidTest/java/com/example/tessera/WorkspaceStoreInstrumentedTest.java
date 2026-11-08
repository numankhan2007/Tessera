package com.example.tessera;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class WorkspaceStoreInstrumentedTest {
    private Context context;
    private WorkspaceStore store;

    @Before public void setUp() {
        context=ApplicationProvider.getApplicationContext();
        context.deleteDatabase("tessera-workspace.db");
        context.getSharedPreferences("tessera_workspace",Context.MODE_PRIVATE).edit().clear().commit();
        store=new WorkspaceStore(context);
    }

    @After public void tearDown() {
        if(store!=null)store.close();
        context.deleteDatabase("tessera-workspace.db");
    }

    @Test public void foldersMergeAndRestoreFromJson() throws Exception {
        long first=store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",0);
        long second=store.addApp(0,1,0,"com.example.beta/.Main","Beta",0);
        store.mergeInto(second,first);

        List<WorkspaceItem> items=store.all();
        assertEquals(1,items.size());
        WorkspaceItem folder=items.get(0);
        assertEquals(WorkspaceItem.FOLDER,folder.type);
        assertTrue(folder.payload.contains("com.example.alpha/.Main"));
        assertTrue(folder.payload.contains("com.example.beta/.Main"));

        long shortcut=store.addShortcut(0,2,0,"com.example.gamma/.Main","compose","Compose",0);
        store.mergeInto(shortcut,folder.id);
        folder=store.find(folder.id);
        assertTrue(folder.payload.contains("compose"));

        String backup=store.exportJson();
        store.close();store=null;
        context.deleteDatabase("tessera-workspace.db");
        store=new WorkspaceStore(context);
        int restored=store.restoreJson(backup);
        assertEquals(1,restored);
        WorkspaceItem restoredFolder=store.all().get(0);
        assertEquals(WorkspaceItem.FOLDER,restoredFolder.type);
        assertTrue(restoredFolder.payload.contains("compose"));
    }

    @Test public void dockIsPersistentAndWidgetBindingsAreSkippedOnRestore() throws Exception {
        store.addApp(-1,4,0,"com.example.dock/.Main","Dock app",0);
        store.addWidget(0,0,0,9876,"com.example.widget/.Provider","Clock",4,2);
        String backup=store.exportJson();

        store.close();store=null;
        context.deleteDatabase("tessera-workspace.db");
        store=new WorkspaceStore(context);
        int restored=store.restoreJson(backup);

        assertEquals(1,restored);
        assertEquals(1,store.getSkippedWidgetCount());
        List<WorkspaceItem> items=store.all();
        assertEquals(1,items.size());
        assertEquals(-1,items.get(0).page);
        assertEquals(4,items.get(0).cellX);
    }

    @Test public void uninstallPrunesFolderEntriesAndEmptyFolders() {
        long a=store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",0);
        long b=store.addApp(0,1,0,"com.example.beta/.Main","Beta",0);
        store.mergeInto(b,a);
        WorkspaceItem folder=store.find(a);
        store.removePackage("com.example.alpha",0);
        WorkspaceItem promoted=store.find(folder.id);
        assertNotNull(promoted);
        assertEquals(WorkspaceItem.APP,promoted.type);
        assertEquals("com.example.beta/.Main",promoted.component);
        assertEquals("Beta",promoted.title);
        store.removePackage("com.example.beta",0);
        assertTrue(store.all().isEmpty());
    }

    @Test public void spanPlacementAndCollisionDetectionAreStable() {
        store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",0);
        assertTrue(store.hasOverlap(0,0,0,1,1,-1));
        assertFalse(store.hasOverlap(0,1,0,1,1,-1));
        int cell=store.firstFreeSpanCell(0,2,2);
        assertTrue(cell>=0);
        store.addWidget(0,cell%4,cell/4,1234,"com.example.widget/.Provider","Weather",2,2);
        assertTrue(store.all().stream().anyMatch(i->WorkspaceItem.WIDGET.equals(i.type)));
    }
    @Test public void removingFolderChildPromotesTheLastRemainingApp() {
        long first=store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",17);
        long second=store.addApp(0,1,0,"com.example.beta/.Main","Beta",17);
        store.mergeInto(second,first);
        WorkspaceItem folder=store.find(first);
        assertTrue(store.removeFolderEntry(folder.id,"com.example.alpha/.Main",null,17));
        WorkspaceItem promoted=store.find(folder.id);
        assertNotNull(promoted);
        assertEquals(WorkspaceItem.APP,promoted.type);
        assertEquals("com.example.beta/.Main",promoted.component);
        assertEquals("Beta",promoted.title);
        assertEquals(17,promoted.userSerial);
        assertFalse(store.removeFolderEntry(folder.id,"com.example.alpha/.Main",null,17));
    }

    @Test public void removingFolderShortcutDoesNotRemoveParentApplication() {
        long parent=store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",17);
        long shortcut=store.addShortcut(0,1,0,"com.example.alpha/.Main","compose","Compose",17);
        store.mergeInto(shortcut,parent);
        WorkspaceItem folder=store.find(parent);
        assertTrue(store.removeFolderEntry(folder.id,"com.example.alpha/.Main","compose",17));
        WorkspaceItem promoted=store.find(folder.id);
        assertNotNull(promoted);
        assertEquals(WorkspaceItem.APP,promoted.type);
        assertEquals("Alpha",promoted.title);
    }

    @Test public void removingAllFolderChildrenDeletesTheFolder() {
        long first=store.addApp(0,0,0,"com.example.alpha/.Main","Alpha",0);
        long second=store.addApp(0,1,0,"com.example.beta/.Main","Beta",0);
        store.mergeInto(second,first);
        WorkspaceItem folder=store.find(first);
        assertTrue(store.removeFolderEntry(folder.id,"com.example.alpha/.Main",null,0));
        assertTrue(store.removeFolderEntry(folder.id,"com.example.beta/.Main",null,0));
        assertNull(store.find(folder.id));
    }

    @Test public void invalidBackupDoesNotDeleteExistingWorkspaceItems() {
        long app=store.addApp(0,0,0,"com.example.keep/.Main","Keep me",0);
        try {
            store.restoreJson("{not-json");
            fail("Expected malformed JSON to be rejected");
        } catch (Exception expected) {
            assertNotNull(store.find(app));
            assertEquals("Keep me",store.find(app).title);
        }
    }

    @Test public void unsupportedBackupDoesNotDeleteExistingWorkspaceItems() {
        long app=store.addApp(0,0,0,"com.example.keep/.Main","Keep me",0);
        try {
            store.restoreJson("{\"format\":\"other\",\"version\":1,\"items\":[]}");
            fail("Expected unsupported backup format to be rejected");
        } catch (Exception expected) {
            assertNotNull(store.find(app));
            assertEquals(1,store.all().size());
        }
    }
    @Test public void duplicateDetectionIncludesPagesDockAndFolderChildren() {
        store.addApp(-1,0,0,"com.example.dock/.Main","Dock app",7);
        assertTrue(store.containsWorkspaceEntry("com.example.dock/.Main",7,null));
        long first=store.addApp(1,0,0,"com.example.alpha/.Main","Alpha",7);
        long second=store.addShortcut(1,1,0,"com.example.beta/.Main","compose","Compose",7);
        store.mergeInto(second,first);
        assertTrue(store.containsWorkspaceEntry("com.example.alpha/.Main",7,null));
        assertTrue(store.containsWorkspaceEntry("com.example.beta/.Main",7,"compose"));
        assertFalse(store.containsWorkspaceEntry("com.example.beta/.Main",7,null));
        assertFalse(store.containsWorkspaceEntry("com.example.beta/.Main",8,"compose"));
    }

}
