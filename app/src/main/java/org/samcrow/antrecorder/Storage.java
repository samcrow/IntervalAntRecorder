package org.samcrow.antrecorder;

import android.os.Environment;

import java.io.File;

/**
 * Utilities for storage and file management
 */
public class Storage {
    private Storage() {
    }

    /**
     * Tries to find the location of the SD card on the file system. If no known directories
     * are available, returns some other external storage directory.
     *
     * @return a directory for storage, which may be a memory card
     */
    public static File getMemoryCard() {
        File dir = new File("/mnt/extSdCard");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        dir = new File("/Removable/MicroSD");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        dir = new File("/storage/extSdCard");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        dir = new File("/storage/6133-3731");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        dir = new File("/storage/1C45-180D");
        if (dir.exists() && dir.isDirectory()) {
            return dir;
        }
        return Environment.getExternalStorageDirectory();
    }
}
