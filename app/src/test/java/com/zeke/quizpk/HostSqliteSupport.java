package com.zeke.quizpk;

import com.almworks.sqlite4java.SQLite;
import org.robolectric.shadows.util.SQLiteLibraryLoader;
import org.robolectric.util.ReflectionHelpers;

/** Optional host adapter: real sqlite4java JNI compiled for Termux ARM64, never packaged. */
public final class HostSqliteSupport {
    private HostSqliteSupport() { }
    public static void install() {
        String path=System.getProperty("quizpk.test.sqliteLibraryDir");
        if(path==null) return; // Supported desktop hosts use Robolectric's normal loader.
        ReflectionHelpers.setStaticField(SQLiteLibraryLoader.class,"instance",new SQLiteLibraryLoader() {
            @Override public void doLoad() {
                SQLite.setLibraryPath(path);
                try { SQLite.getSQLiteVersion(); }
                catch(com.almworks.sqlite4java.SQLiteException e) { throw new IllegalStateException(e); }
            }
        });
    }
}
